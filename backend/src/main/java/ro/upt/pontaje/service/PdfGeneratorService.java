package ro.upt.pontaje.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.util.Matrix;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.upt.pontaje.exception.BadRequestException;
import ro.upt.pontaje.exception.ResourceNotFoundException;
import ro.upt.pontaje.model.*;
import ro.upt.pontaje.model.Document;
import ro.upt.pontaje.repository.DocumentRepository;

import java.awt.Color;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.List;

/**
 * Serviciu pentru generarea documentelor PDF (Anexa 1 / Anexa 3) în formatul oficial UPT:
 * fișă săptămânală de evidență a orelor (ore x zile), cu legendă, totaluri și semnături.
 */
@Service
public class PdfGeneratorService {

    private final DocumentRepository documentRepository;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PdfGeneratorService.class);

    public PdfGeneratorService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Value("${app.documents.storage-path}")
    private String storagePath;

    // ---- Stil / culori ----
    private static final Color UPT_BLUE = new Color(0x00, 0x33, 0x66);
    private static final Color UPT_BLUE_LIGHT = new Color(0x00, 0x66, 0xCC);
    private static final Color HEADER_FILL = new Color(0xE9, 0xEE, 0xF6);
    private static final Color TOTAL_FILL = new Color(0xDF, 0xE7, 0xF2);
    private static final Color GRID = new Color(0x9A, 0xA8, 0xBC);
    private static final Color NORMA_FILL = new Color(0xE3, 0xF6, 0xE8);
    private static final Color PLATA_FILL = new Color(0xFD, 0xEC, 0xDD);
    private static final Color NORMA_TXT = new Color(0x1E, 0x7E, 0x34);
    private static final Color PLATA_TXT = new Color(0xB2, 0x5A, 0x12);
    private static final Color MUTED = new Color(0x9A, 0xA8, 0xBC);

    private static final float MARGIN = 28f;
    private static final DateTimeFormatter D_DMY = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter D_DM = DateTimeFormatter.ofPattern("dd.MM");

    private static final String[] MONTHS_RO = {"", "ianuarie", "februarie", "martie", "aprilie", "mai", "iunie",
            "iulie", "august", "septembrie", "octombrie", "noiembrie", "decembrie"};
    private static final String[] DAY_NAMES_RO = {"Luni", "Marți", "Miercuri", "Joi", "Vineri", "Sâmbătă", "Duminică"};
    private static final String[] DAY_ABBR_RO = {"Lu", "Ma", "Mi", "Jo", "Vi", "Sâ", "Du"};

    // Intervalele orare (rândurile fișei): 08-09 ... 21-22
    private static final int FIRST_HOUR = 8;
    private static final int LAST_HOUR = 22;

    /**
     * Generează PDF pentru Anexa 1 / Anexa 3 în format de fișă săptămânală (o pagină per săptămână).
     */
    @Transactional
    public Document generateAnnex(Timesheet timesheet, AnnexType annexType) throws IOException {
        User user = timesheet.getUser();

        Path storageDir = Paths.get(storagePath);
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }

        String fileName = String.format("%s_%s_%s_%d_%d.pdf",
                annexType.name().toLowerCase(),
                safe(user.getLastName()),
                safe(user.getFirstName()),
                timesheet.getMonth(),
                timesheet.getYear());

        String filePath = Paths.get(storagePath, fileName).toString();

        try (PDDocument document = new PDDocument()) {
            PDType0Font fontRegular = PDType0Font.load(document,
                    new ClassPathResource("fonts/DejaVuSans.ttf").getInputStream(), true);
            PDType0Font fontBold = PDType0Font.load(document,
                    new ClassPathResource("fonts/DejaVuSans-Bold.ttf").getInputStream(), true);

            String departmentName = "—";
            if (user.getDepartment() != null && Hibernate.isInitialized(user.getDepartment())) {
                departmentName = user.getDepartment().getName();
            }

            // Indexăm intrările după dată
            Map<LocalDate, List<TimesheetEntry>> byDate = new HashMap<>();
            for (TimesheetEntry e : timesheet.getEntries()) {
                byDate.computeIfAbsent(e.getEntryDate(), k -> new ArrayList<>()).add(e);
            }

            // Calculăm săptămânile (Luni→Duminică) care acoperă luna
            LocalDate first = LocalDate.of(timesheet.getYear(), timesheet.getMonth(), 1);
            LocalDate last = first.withDayOfMonth(first.lengthOfMonth());
            LocalDate weekStart = first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

            int weekIndex = 1;
            while (!weekStart.isAfter(last)) {
                PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth())); // landscape
                document.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                    drawWeekSheet(cs, fontRegular, fontBold, page, annexType, user, departmentName,
                            timesheet, byDate, weekStart, weekIndex);
                }
                weekStart = weekStart.plusWeeks(1);
                weekIndex++;
            }

            document.save(filePath);
        }

        // Idempotent: reutilizăm un singur rând pentru (pontaj, tip anexă), suprascriem fișierul
        // cu formatul curent și ștergem eventualele duplicate generate anterior (care produceau
        // pagini dublate în centralizator).
        List<Document> existing = documentRepository
                .findAllByTimesheetIdAndAnnexType(timesheet.getId(), annexType);
        Document doc;
        if (existing.isEmpty()) {
            doc = Document.builder()
                    .user(user)
                    .timesheet(timesheet)
                    .annexType(annexType)
                    .build();
        } else {
            doc = existing.get(0);
            if (existing.size() > 1) {
                documentRepository.deleteAll(existing.subList(1, existing.size()));
            }
        }
        doc.setFilePath(filePath);
        doc.setFileName(fileName);

        return documentRepository.save(doc);
    }

    // ====================== DESENARE FIȘĂ SĂPTĂMÂNALĂ ======================

    private void drawWeekSheet(PDPageContentStream cs, PDFont fr, PDFont fb, PDPage page,
                               AnnexType annexType, User user, String departmentName, Timesheet timesheet,
                               Map<LocalDate, List<TimesheetEntry>> byDate,
                               LocalDate weekStart, int weekIndex) throws IOException {
        float pageW = page.getMediaBox().getWidth();
        float pageH = page.getMediaBox().getHeight();
        float left = MARGIN;
        float right = pageW - MARGIN;
        float y = pageH - MARGIN;

        // --- Antet instituțional ---
        text(cs, fb, 12, UPT_BLUE, left, y - 10, "Universitatea Politehnica Timișoara");
        textRight(cs, fb, 12, UPT_BLUE, right, y - 10, annexType.getDisplayName());
        text(cs, fr, 9.5f, Color.DARK_GRAY, left, y - 26, "Departamentul " + departmentName);

        cs.setStrokingColor(UPT_BLUE_LIGHT);
        cs.setLineWidth(1.4f);
        cs.moveTo(left, y - 33);
        cs.lineTo(right, y - 33);
        cs.stroke();

        // --- Titlu centrat ---
        float titleY = y - 52;
        String title = annexType == AnnexType.ANEXA_1
                ? "EVIDENȚA NUMĂRULUI DE ORE LUCRATE DE CĂTRE CADRELE DIDACTICE"
                : "EVIDENȚA NUMĂRULUI DE ORE DE CONDUCERE DOCTORAT";
        textCenter(cs, fb, 11, UPT_BLUE, left, right, titleY, title);
        if (annexType == AnnexType.ANEXA_1) {
            textCenter(cs, fb, 11, UPT_BLUE, left, right, titleY - 14,
                    "TITULARE SAU CU CONTRACT DE MUNCĂ PE DURATĂ DETERMINATĂ");
        }

        // --- Sub-antet: perioadă / cadru ---
        LocalDate weekEnd = weekStart.plusDays(6);
        String period = String.format("Luna %s %d  ·  Săptămâna %d  ·  %s – %s",
                MONTHS_RO[timesheet.getMonth()], timesheet.getYear(), weekIndex,
                weekStart.format(D_DMY), weekEnd.format(D_DMY));
        textCenter(cs, fr, 9.5f, Color.DARK_GRAY, left, right, titleY - 30, period);

        // ====== TABEL (fidel șablonului Anexa 1) ======
        float tableTop = titleY - 44;

        // lățimi coloane
        float wA = 80f;     // Nume și prenume
        float wB = 46f;     // Program de lucru (intervale orare)
        float wJ = 66f;     // Semnătură
        float gap = 8f;     // spațiu înainte de legendă
        float wL = 16f, wM = 138f, wN = 42f; // legendă (etichetă verticală, denumire, cod)
        float legendW = wL + wM + wN;
        float wDaysTotal = (right - left) - wA - wB - wJ - gap - legendW;
        float wDay = wDaysTotal / 7f;

        float xA = left;
        float xB = xA + wA;
        float xDays = xB + wB;
        float xJ = xDays + wDaysTotal;
        float xLeg = xJ + wJ + gap;

        float HL = 26f; // rând antet (row 7)
        float R = 20f;  // înălțime rând (row 8..23)
        int NROWS = 16; // rândurile 8..23 (data + 14 ore + total)
        int hourRows = LAST_HOUR - FIRST_HOUR; // 14

        float rowsTop = tableTop - HL; // marginea de sus a rândului „data" (i=0)

        // --- Antet (row 7) ---
        // A7:A8, B7:B8, J7:J8 ocupă antetul + rândul de date (HL + R)
        headerCell(cs, fb, xA, wA, rowsTop - R, HL + R, "Nume și prenume cadru didactic");
        headerCell(cs, fb, xB, wB, rowsTop - R, HL + R, "Program de lucru");
        headerCell(cs, fb, xJ, wJ, rowsTop - R, HL + R, "Semnătură cadru didactic");
        // C7:I7 „Activități desfășurate"
        fillRect(cs, xDays, tableTop - HL, wDaysTotal, HL, HEADER_FILL);
        cellBorder(cs, xDays, tableTop - HL, wDaysTotal, HL);
        textCenter(cs, fb, 8.5f, UPT_BLUE, xDays, xDays + wDaysTotal, tableTop - HL / 2 - 3, "Activități desfășurate");
        // L7:N7 „Legendă"
        fillRect(cs, xLeg, tableTop - HL, legendW, HL, HEADER_FILL);
        cellBorder(cs, xLeg, tableTop - HL, legendW, HL);
        textCenter(cs, fb, 8.5f, UPT_BLUE, xLeg, xLeg + legendW, tableTop - HL / 2 - 3, "Legendă");

        // --- Rândul de date (row 8, i=0) ---
        for (int d = 0; d < 7; d++) {
            float cx = xDays + d * wDay;
            LocalDate day = weekStart.plusDays(d);
            boolean inMonth = day.getMonthValue() == timesheet.getMonth();
            Color dc = inMonth ? UPT_BLUE : MUTED;
            fillRect(cs, cx, rowsTop - R, wDay, R, HEADER_FILL);
            cellBorder(cs, cx, rowsTop - R, wDay, R);
            textCenter(cs, fb, 7f, dc, cx, cx + wDay, rowsTop - 8.5f, DAY_ABBR_RO[d] + " " + day.format(D_DM));
        }

        // --- Rânduri ore (row 9..22, i=1..14) ---
        int[] dayTotals = new int[7];
        for (int r = 0; r < hourRows; r++) {
            int i = r + 1;
            int h = FIRST_HOUR + r;
            float topY = rowsTop - i * R;
            // intervalul orar (col B)
            cellBorder(cs, xB, topY - R, wB, R);
            textCenter(cs, fr, 8f, Color.BLACK, xB, xB + wB, topY - R + 6.5f, String.format("%02d-%02d", h, h + 1));
            // celulele zilelor (cu cod activitate)
            for (int d = 0; d < 7; d++) {
                float cx = xDays + d * wDay;
                cellBorder(cs, cx, topY - R, wDay, R);
                TimesheetEntry e = findEntry(byDate.get(weekStart.plusDays(d)), h);
                if (e != null) {
                    textCenter(cs, fr, 7.5f, Color.BLACK, cx, cx + wDay, topY - R + 6.5f, activityCode(e));
                    dayTotals[d] += e.getDurationHours();
                }
            }
        }

        // --- Celula nume (A9:A22) și semnătură (J9:J22), comasate vertical ---
        float bodyTop = rowsTop - R;            // sub rândul de date
        float bodyH = hourRows * R;             // 14 rânduri
        cellBorder(cs, xA, bodyTop - bodyH, wA, bodyH);
        drawWrapCentered(cs, fb, 8.5f, UPT_BLUE, xA, xA + wA, bodyTop - bodyH, bodyTop, user.getFullName());
        cellBorder(cs, xJ, bodyTop - bodyH, wJ, bodyH);

        // --- Rând total (row 23, i=15) ---
        float tTop = rowsTop - 15 * R;
        fillRect(cs, xA, tTop - R, wA + wB, R, TOTAL_FILL);
        cellBorder(cs, xA, tTop - R, wA + wB, R);
        text(cs, fb, 8f, UPT_BLUE, xA + 5, tTop - R + 6.5f, "Total ore lucrate:");
        int weekTotal = 0;
        for (int d = 0; d < 7; d++) {
            float cx = xDays + d * wDay;
            fillRect(cs, cx, tTop - R, wDay, R, TOTAL_FILL);
            cellBorder(cs, cx, tTop - R, wDay, R);
            textCenter(cs, fb, 8f, UPT_BLUE, cx, cx + wDay, tTop - R + 6.5f, String.valueOf(dayTotals[d]));
            weekTotal += dayTotals[d];
        }
        fillRect(cs, xJ, tTop - R, wJ, R, TOTAL_FILL);
        cellBorder(cs, xJ, tTop - R, wJ, R);
        textCenter(cs, fb, 8f, UPT_BLUE, xJ, xJ + wJ, tTop - R + 6.5f, String.valueOf(weekTotal));

        // --- Legenda (M/N pe cele 16 rânduri, L cu etichete verticale) ---
        drawLegendBlock(cs, fr, fb, xLeg, wL, wM, wN, rowsTop, R);

        // ====== Observații ======
        float obsTop = (rowsTop - NROWS * R) - 20;
        text(cs, fb, 9f, UPT_BLUE, xA, obsTop, "Observații:");
        text(cs, fr, 8f, Color.DARK_GRAY, xA, obsTop - 15,
                "1. Cine a fost în delegație (internă/externă) să completeze coloana respectivă și să specifice tipul.");
        text(cs, fr, 8f, Color.DARK_GRAY, xA, obsTop - 28,
                "2. Concediile și absențele se marchează conform codurilor din legendă.");

        // dată întocmire
        textRight(cs, fr, 8f, Color.GRAY, right, obsTop - 28, "Întocmit: " + LocalDate.now().format(D_DMY));
    }

    // denumirile și codurile din legenda oficială Anexa 1 (în ordinea rândurilor 8..23)
    private static final String[][] LEGEND_ITEMS = {
            {"CURS", "A I (C)"},
            {"SEMINAR", "A I (S)"},
            {"LABORATOR", "A I (L)"},
            {"PROIECT", "A I (P)"},
            {"cap. A II", "A II"},
            {"cap. B", "B"},
            {"cap. C", "C"},
            {"Deplasare internă", "DI"},
            {"Deplasare externă", "DE"},
            {"Nu se aplică", "-"},
            {"CONCEDIU DE ODIHNĂ", "CO"},
            {"CONCEDIU MEDICAL", "CME"},
            {"CONCEDIU FĂRĂ SALARIU", "CFS"},
            {"CONCEDIU CREȘTERE COPIL", "CCC"},
            {"CONCEDIU DE MATERNITATE", "CMA"},
            {"ABSENȚE NEMOTIVATE", "ABS"},
    };

    private void drawLegendBlock(PDPageContentStream cs, PDFont fr, PDFont fb,
                                 float xLeg, float wL, float wM, float wN,
                                 float rowsTop, float R) throws IOException {
        float xM = xLeg + wL;
        float xN = xM + wM;
        for (int i = 0; i < LEGEND_ITEMS.length; i++) {
            float topY = rowsTop - i * R;
            // denumire (M)
            cellBorder(cs, xM, topY - R, wM, R);
            text(cs, fr, 6.8f, Color.BLACK, xM + 4, topY - R + 6.5f, LEGEND_ITEMS[i][0]);
            // cod (N)
            cellBorder(cs, xN, topY - R, wN, R);
            textCenter(cs, fb, 6.8f, UPT_BLUE, xN, xN + wN, topY - R + 6.5f, LEGEND_ITEMS[i][1]);
        }
        // eticheta verticală L: secțiunea 1 (rândurile 0..9) și secțiunea 2 (10..15)
        float s1Top = rowsTop;
        float s1H = 10 * R;
        cellBorder(cs, xLeg, s1Top - s1H, wL, s1H);
        textRotated(cs, fr, 6.5f, UPT_BLUE, xLeg + 11, s1Top - s1H + 6,
                "Activități desfășurate conf. Fișei Postului");
        float s2Top = rowsTop - 10 * R;
        float s2H = 6 * R;
        cellBorder(cs, xLeg, s2Top - s2H, wL, s2H);
        textRotated(cs, fr, 6.5f, UPT_BLUE, xLeg + 11, s2Top - s2H + 6, "Întreruperi activitate");
    }

    /** Mapează o intrare de pontaj la un cod din legenda Anexa 1, după textul activității. */
    private String activityCode(TimesheetEntry e) {
        String a = e.getActivity() == null ? "" : e.getActivity().toLowerCase();
        if (a.contains("curs")) return "A I (C)";
        if (a.contains("seminar")) return "A I (S)";
        if (a.contains("laborator") || a.contains("lab")) return "A I (L)";
        if (a.contains("proiect")) return "A I (P)";
        if (a.contains("concediu")) {
            if (a.contains("medical")) return "CME";
            if (a.contains("odihn")) return "CO";
            return "CO";
        }
        if (a.contains("deplasare") || a.contains("delega")) return "DI";
        // implicit, după tipul orei
        return e.getHourType() == HourType.NORMA ? "A I (C)" : "A I (L)";
    }

    // ---- celulă de antet cu text încadrat și centrat vertical ----
    private void headerCell(PDPageContentStream cs, PDFont font, float x, float w,
                            float yBottom, float h, String label) throws IOException {
        fillRect(cs, x, yBottom, w, h, HEADER_FILL);
        cellBorder(cs, x, yBottom, w, h);
        drawWrapCentered(cs, font, 7.5f, UPT_BLUE, x, x + w, yBottom, yBottom + h, label);
    }

    // ---- text încadrat (wrap) și centrat pe verticală/orizontală într-un dreptunghi ----
    private void drawWrapCentered(PDPageContentStream cs, PDFont font, float size, Color color,
                                  float x0, float x1, float yBottom, float yTop, String s) throws IOException {
        List<String> lines = wrap(font, size, s, (x1 - x0) - 6);
        float lineH = size + 2.5f;
        float blockTop = (yBottom + yTop) / 2 + (lines.size() * lineH) / 2;
        float baseline = blockTop - size;
        for (String line : lines) {
            textCenter(cs, font, size, color, x0, x1, baseline, line);
            baseline -= lineH;
        }
    }

    // ---- împarte un text în linii care încap în maxW ----
    private List<String> wrap(PDFont font, float size, String s, float maxW) throws IOException {
        List<String> lines = new ArrayList<>();
        if (s == null || s.isEmpty()) return lines;
        StringBuilder cur = new StringBuilder();
        for (String word : s.split("\\s+")) {
            String trial = cur.length() == 0 ? word : cur + " " + word;
            if (font.getStringWidth(trial) / 1000 * size > maxW && cur.length() > 0) {
                lines.add(cur.toString());
                cur = new StringBuilder(word);
            } else {
                cur = new StringBuilder(trial);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }

    // ---- text rotit 90° (pentru etichetele verticale din legendă) ----
    private void textRotated(PDPageContentStream cs, PDFont font, float size, Color color,
                             float x, float y, String s) throws IOException {
        if (s == null) s = "";
        cs.beginText();
        cs.setNonStrokingColor(color);
        cs.setFont(font, size);
        cs.setTextMatrix(Matrix.getRotateInstance(Math.PI / 2, x, y));
        cs.showText(s);
        cs.endText();
    }

    // ====================== HELPER GRAFIC ======================

    private void text(PDPageContentStream cs, PDFont font, float size, Color color,
                      float x, float y, String s) throws IOException {
        if (s == null) s = "";
        cs.beginText();
        cs.setNonStrokingColor(color);
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(s);
        cs.endText();
    }

    private void textRight(PDPageContentStream cs, PDFont font, float size, Color color,
                           float xRight, float y, String s) throws IOException {
        float w = font.getStringWidth(s) / 1000 * size;
        text(cs, font, size, color, xRight - w, y, s);
    }

    private void textCenter(PDPageContentStream cs, PDFont font, float size, Color color,
                            float x0, float x1, float y, String s) throws IOException {
        float w = font.getStringWidth(s) / 1000 * size;
        text(cs, font, size, color, x0 + (x1 - x0 - w) / 2, y, s);
    }

    private void textCenterWrap(PDPageContentStream cs, PDFont font, float size, Color color,
                                float x0, float x1, float yTop, String s) throws IOException {
        String[] lines = s.split("\n");
        float ly = yTop;
        for (String line : lines) {
            textCenter(cs, font, size, color, x0, x1, ly, line);
            ly -= size + 2;
        }
    }

    private void fillRect(PDPageContentStream cs, float x, float y, float w, float h, Color c) throws IOException {
        cs.setNonStrokingColor(c);
        cs.addRect(x, y, w, h);
        cs.fill();
    }

    private void cellBorder(PDPageContentStream cs, float x, float y, float w, float h) throws IOException {
        cs.setStrokingColor(GRID);
        cs.setLineWidth(0.5f);
        cs.addRect(x, y, w, h);
        cs.stroke();
    }

    private TimesheetEntry findEntry(List<TimesheetEntry> dayEntries, int hour) {
        if (dayEntries == null) return null;
        for (TimesheetEntry e : dayEntries) {
            String start = e.getStartTime();
            if (start != null) {
                try {
                    if (Integer.parseInt(start.split(":")[0]) == hour) return e;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    private String safe(String s) {
        if (s == null) return "x";
        return s.toLowerCase().replaceAll("\\s+", "_").replaceAll("[^a-z0-9_]", "");
    }

    // ====================== MERGE / DOWNLOAD ======================

    public byte[] mergePdfs(List<Document> documents) throws IOException {
        if (documents.isEmpty()) {
            throw new BadRequestException("Nu sunt documente de concatenat");
        }

        documents.sort(Comparator.comparing(d -> d.getUser().getLastName()));

        // Folosim PDFMergerUtility — gestionează corect fonturile încorporate (subset),
        // spre deosebire de importPage care corupe codarea caracterelor la concatenare.
        PDFMergerUtility merger = new PDFMergerUtility();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        merger.setDestinationStream(baos);

        boolean hasSource = false;
        for (Document doc : documents) {
            File file = new File(doc.getFilePath());
            if (!file.exists()) {
                log.warn("Fișierul PDF nu există pe disc pentru documentul {} - path: {}", doc.getId(), doc.getFilePath());
                continue;
            }
            merger.addSource(file);
            hasSource = true;
        }

        if (!hasSource) {
            throw new BadRequestException("Nu există pagini valide de concatenat");
        }

        merger.mergeDocuments(IOUtils.createMemoryOnlyStreamCache());
        return baos.toByteArray();
    }

    public byte[] downloadDocument(UUID documentId) throws IOException {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documentul nu a fost găsit"));

        File file = new File(doc.getFilePath());
        if (!file.exists()) {
            throw new ResourceNotFoundException("Fișierul nu a fost găsit pe server");
        }

        return Files.readAllBytes(file.toPath());
    }
}
