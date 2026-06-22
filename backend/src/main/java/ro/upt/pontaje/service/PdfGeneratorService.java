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

        // Idempotent: dacă există deja un document pentru (pontaj, tip anexă),
        // reutilizăm rândul și suprascriem fișierul cu formatul curent (evităm duplicate
        // și asigurăm că secretariatul primește mereu versiunea nouă).
        Document doc = documentRepository
                .findByTimesheetIdAndAnnexType(timesheet.getId(), annexType)
                .orElseGet(() -> Document.builder()
                        .user(user)
                        .timesheet(timesheet)
                        .annexType(annexType)
                        .build());
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

        // ====== TABEL ======
        float tableTop = titleY - 46;
        float wProgram = 64f;          // "Program de lucru"
        float wSign = 92f;             // "Semnătură cadru didactic"
        float wDays = (right - left) - wProgram - wSign;
        float wDay = wDays / 7f;
        float headH = 28f;
        float rowH = 18f;
        int hourRows = LAST_HOUR - FIRST_HOUR; // 14

        // --- Antet tabel ---
        float hy = tableTop;
        fillRect(cs, left, hy - headH, right - left, headH, HEADER_FILL);

        cellBorder(cs, left, hy - headH, wProgram, headH);
        textCenterWrap(cs, fb, 8f, UPT_BLUE, left, left + wProgram, hy - 10, "Program\nde lucru");

        for (int d = 0; d < 7; d++) {
            float cx = left + wProgram + d * wDay;
            cellBorder(cs, cx, hy - headH, wDay, headH);
            LocalDate day = weekStart.plusDays(d);
            boolean inMonth = day.getMonthValue() == timesheet.getMonth();
            Color dc = inMonth ? UPT_BLUE : MUTED;
            textCenter(cs, fb, 8.5f, dc, cx, cx + wDay, hy - 12, DAY_NAMES_RO[d]);
            textCenter(cs, fr, 8f, dc, cx, cx + wDay, hy - 24, day.format(D_DM));
        }

        float sx = left + wProgram + wDays;
        cellBorder(cs, sx, hy - headH, wSign, headH);
        textCenterWrap(cs, fb, 8f, UPT_BLUE, sx, sx + wSign, hy - 10, "Semnătură\ncadru didactic");

        // banda cu numele cadrului
        hy -= headH;
        float nameBandH = 16f;
        fillRect(cs, left, hy - nameBandH, right - left, nameBandH, new Color(0xF3, 0xF6, 0xFB));
        cellBorder(cs, left, hy - nameBandH, right - left, nameBandH);
        text(cs, fb, 9f, UPT_BLUE, left + 6, hy - nameBandH + 5, "Cadru didactic:  " + user.getFullName());
        hy -= nameBandH;

        // --- Rânduri ore ---
        int[] dayTotals = new int[7];
        for (int r = 0; r < hourRows; r++) {
            int h = FIRST_HOUR + r;
            float ry = hy - r * rowH;
            String slot = String.format("%02d-%02d", h, h + 1);
            cellBorder(cs, left, ry - rowH, wProgram, rowH);
            textCenter(cs, fr, 8.5f, Color.BLACK, left, left + wProgram, ry - rowH + 7, slot);

            for (int d = 0; d < 7; d++) {
                float cx = left + wProgram + d * wDay;
                LocalDate day = weekStart.plusDays(d);
                TimesheetEntry e = findEntry(byDate.get(day), h);
                if (e != null) {
                    Color fill = e.getHourType() == HourType.NORMA ? NORMA_FILL : PLATA_FILL;
                    fillRect(cs, cx, ry - rowH, wDay, rowH, fill);
                }
                cellBorder(cs, cx, ry - rowH, wDay, rowH);
                if (e != null) {
                    String code = e.getHourType() == HourType.NORMA ? "N" : "P";
                    Color cc = e.getHourType() == HourType.NORMA ? NORMA_TXT : PLATA_TXT;
                    textCenter(cs, fb, 8.5f, cc, cx, cx + wDay, ry - rowH + 7, code);
                    dayTotals[d] += e.getDurationHours();
                }
            }
        }

        // semnătură (celulă mare pe verticală)
        float gridBottom = hy - hourRows * rowH;
        cellBorder(cs, sx, gridBottom, wSign, hourRows * rowH);

        // --- Rând total ---
        float ty = gridBottom;
        fillRect(cs, left, ty - rowH, wProgram + wDays, rowH, TOTAL_FILL);
        cellBorder(cs, left, ty - rowH, wProgram, rowH);
        text(cs, fb, 8.5f, UPT_BLUE, left + 5, ty - rowH + 7, "Total ore:");
        int weekTotal = 0;
        for (int d = 0; d < 7; d++) {
            float cx = left + wProgram + d * wDay;
            cellBorder(cs, cx, ty - rowH, wDay, rowH);
            textCenter(cs, fb, 8.5f, UPT_BLUE, cx, cx + wDay, ty - rowH + 7, String.valueOf(dayTotals[d]));
            weekTotal += dayTotals[d];
        }
        cellBorder(cs, sx, ty - rowH, wSign, rowH);
        textCenter(cs, fb, 8.5f, UPT_BLUE, sx, sx + wSign, ty - rowH + 7, "Σ " + weekTotal + " ore");

        // ====== Legendă + observații ======
        float belowY = ty - rowH - 18;
        drawLegend(cs, fr, fb, left, belowY);
        drawObservatii(cs, fr, fb, left + 290f, belowY, right - (left + 290f));

        // ====== Semnături jos ======
        text(cs, fr, 9f, Color.BLACK, left, 44, "Semnătură cadru didactic: ______________________");
        textRight(cs, fr, 9f, Color.BLACK, right, 44, "Data: " + LocalDate.now().format(D_DMY));
        textRight(cs, fr, 8f, Color.GRAY, right, 28, "Director departament: ______________________");
    }

    private void drawLegend(PDPageContentStream cs, PDFont fr, PDFont fb, float x, float topY)
            throws IOException {
        text(cs, fb, 9f, UPT_BLUE, x, topY, "Legendă");
        float yy = topY - 16;
        legendItem(cs, fr, x, yy, NORMA_FILL, NORMA_TXT, "N", "N — oră în normă (activitate didactică din normă)");
        yy -= 16;
        legendItem(cs, fr, x, yy, PLATA_FILL, PLATA_TXT, "P", "P — oră în regim de plată cu ora");
        yy -= 16;
        text(cs, fr, 7.5f, Color.GRAY, x, yy, "Valorile din rândul „Total ore” = ore lucrate pe zi.");
    }

    private void legendItem(PDPageContentStream cs, PDFont fr, float x, float y,
                            Color fill, Color codeColor, String code, String label) throws IOException {
        fillRect(cs, x, y - 3, 14, 12, fill);
        cellBorder(cs, x, y - 3, 14, 12);
        textCenter(cs, fr, 8f, codeColor, x, x + 14, y, code);
        text(cs, fr, 8f, Color.BLACK, x + 20, y, label);
    }

    private void drawObservatii(PDPageContentStream cs, PDFont fr, PDFont fb, float x, float topY, float w)
            throws IOException {
        text(cs, fb, 9f, UPT_BLUE, x, topY, "Observații");
        cellBorder(cs, x, topY - 56, w, 50);
        text(cs, fr, 8f, Color.GRAY, x + 6, topY - 16,
                "1. Sărbătorile legale și concediile se marchează conform legislației în vigoare.");
        text(cs, fr, 8f, Color.GRAY, x + 6, topY - 30,
                "2. Delegațiile (internă / externă) se specifică în prezenta evidență.");
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
