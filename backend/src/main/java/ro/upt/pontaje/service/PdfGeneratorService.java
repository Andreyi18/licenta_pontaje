package ro.upt.pontaje.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviciu pentru generarea documentelor PDF (Anexa 1, Anexa 3, pontaje)
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

    // Configurări PDF
    private static final float MARGIN = 50;
    private static final float HEADER_HEIGHT = 80;
    private static final float ROW_HEIGHT = 20;
    private static final float CELL_PADDING = 5;

    // Intervale orare standard
    private static final String[] TIME_SLOTS = {
        "08:00-09:00", "09:00-10:00", "10:00-11:00", "11:00-12:00",
        "12:00-13:00", "13:00-14:00", "14:00-15:00", "15:00-16:00",
        "16:00-17:00", "17:00-18:00", "18:00-19:00", "19:00-20:00",
        "20:00-21:00", "21:00-22:00"
    };

    // Zile săptămână
    private static final String[] DAYS = {"Luni", "Marți", "Miercuri", "Joi", "Vineri", "Sâmbătă", "Duminică"};

    /**
     * Generează PDF pentru Anexa 1 sau Anexa 3
     */
    @Transactional
    public Document generateAnnex(Timesheet timesheet, AnnexType annexType) throws IOException {
        User user = timesheet.getUser();
        
        // Creează directorul pentru documente dacă nu există
        Path storageDir = Paths.get(storagePath);
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }

        // Generează numele fișierului
        String fileName = String.format("%s_%s_%s_%d_%d.pdf",
            annexType.name().toLowerCase(),
            user.getLastName().toLowerCase().replaceAll("\\s+", "_"),
            user.getFirstName().toLowerCase().replaceAll("\\s+", "_"),
            timesheet.getMonth(),
            timesheet.getYear());
        
        String filePath = Paths.get(storagePath, fileName).toString();

        // Generează PDF-ul
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                float pageWidth = page.getMediaBox().getWidth();
                float pageHeight = page.getMediaBox().getHeight();
                float currentY = pageHeight - MARGIN;

                // Header - Logo și informații UPT
                currentY = drawHeader(contentStream, fontBold, fontRegular, pageWidth, currentY);

                // Titlu anexă
                currentY = drawTitle(contentStream, fontBold, annexType, pageWidth, currentY);

                // Informații utilizator
                currentY = drawUserInfo(contentStream, fontRegular, user, timesheet, pageWidth, currentY);

                // Tabel cu ore
                currentY = drawTimesheetTable(contentStream, fontBold, fontRegular, 
                    timesheet, pageWidth, currentY);

                // Footer cu totale
                drawFooter(contentStream, fontBold, fontRegular, timesheet, pageWidth, currentY);
            }

            // Salvează documentul
            document.save(filePath);
        }

        // Salvează înregistrarea în baza de date
        Document doc = Document.builder()
            .user(user)
            .timesheet(timesheet)
            .annexType(annexType)
            .filePath(filePath)
            .fileName(fileName)
            .build();

        return documentRepository.save(doc);
    }

    /**
     * Concatenează mai multe PDF-uri într-unul singur (pentru secretariat)
     */
    public byte[] mergePdfs(List<Document> documents) throws IOException {
        if (documents.isEmpty()) {
            throw new BadRequestException("Nu sunt documente de concatenat");
        }

        // Sortează alfabetic după nume
        documents.sort(Comparator.comparing(d -> d.getUser().getLastName()));

        try (PDDocument mergedDoc = new PDDocument()) {
            boolean hasPages = false;

            for (Document doc : documents) {
                File file = new File(doc.getFilePath());
                if (!file.exists()) {
                    log.warn("Fișierul PDF nu există pe disc pentru documentul {} - path: {}", doc.getId(), doc.getFilePath());
                    continue;
                }

                try (PDDocument pdf = Loader.loadPDF(file)) {
                    if (pdf.getNumberOfPages() == 0) {
                        log.warn("Document PDF fără pagini pentru documentul {} - path: {}", doc.getId(), doc.getFilePath());
                        continue;
                    }

                    for (PDPage page : pdf.getPages()) {
                        mergedDoc.addPage(mergedDoc.importPage(page));
                        hasPages = true;
                    }
                }
            }

            if (!hasPages) {
                throw new BadRequestException("Nu există pagini valide de concatenat");
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            mergedDoc.save(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Descarcă un document PDF
     */
    public byte[] downloadDocument(UUID documentId) throws IOException {
        Document doc = documentRepository.findById(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("Documentul nu a fost găsit"));

        File file = new File(doc.getFilePath());
        if (!file.exists()) {
            throw new ResourceNotFoundException("Fișierul nu a fost găsit pe server");
        }

        return Files.readAllBytes(file.toPath());
    }

    // ========== Helper Methods pentru generare PDF ==========

    private float drawHeader(PDPageContentStream cs, PDType1Font fontBold, PDType1Font fontRegular,
                            float pageWidth, float y) throws IOException {
        // Universitatea Politehnica Timișoara
        cs.beginText();
        cs.setFont(fontBold, 14);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(sanitizeForPdf("UNIVERSITATEA POLITEHNICA TIMISOARA"));
        cs.endText();

        y -= 20;
        cs.beginText();
        cs.setFont(fontRegular, 10);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(sanitizeForPdf("Piata Victoriei nr. 2, Timisoara 300006, Romania"));
        cs.endText();

        return y - 40;
    }

    private float drawTitle(PDPageContentStream cs, PDType1Font fontBold, 
                           AnnexType annexType, float pageWidth, float y) throws IOException {
        String rawTitle = annexType.getDisplayName().toUpperCase();
        String title = sanitizeForPdf(rawTitle);
        cs.beginText();
        cs.setFont(fontBold, 12);
        
        // Centrează titlul
        float titleWidth = fontBold.getStringWidth(title) / 1000 * 12;
        cs.newLineAtOffset((pageWidth - titleWidth) / 2, y);
        cs.showText(title);
        cs.endText();

        y -= 15;
        
        // Subtitlu
        String subtitle = sanitizeForPdf(annexType.getFullTitle());
        cs.beginText();
        cs.setFont(fontBold, 9);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(subtitle);
        cs.endText();

        return y - 30;
    }

    /**
     * Înlocuiește caracterele românești care nu pot fi reprezentate în Helvetica/WinAnsi
     * pentru a evita IllegalArgumentException în PDFBox.
     */
    private String sanitizeForPdf(String text) {
        if (text == null) return "";
        return text
            .replace("ă", "a").replace("Ă", "A")
            .replace("â", "a").replace("Â", "A")
            .replace("î", "i").replace("Î", "I")
            .replace("ș", "s").replace("Ș", "S")
            .replace("ţ", "t").replace("Ţ", "T")
            .replace("ț", "t").replace("Ț", "T");
    }

    private float drawUserInfo(PDPageContentStream cs, PDType1Font fontRegular,
                              User user, Timesheet timesheet, float pageWidth, float y) throws IOException {
        String[] months = {"", "Ianuarie", "Februarie", "Martie", "Aprilie", "Mai", "Iunie",
                          "Iulie", "August", "Septembrie", "Octombrie", "Noiembrie", "Decembrie"};

        String departmentName = "N/A";
        if (user.getDepartment() != null && Hibernate.isInitialized(user.getDepartment())) {
            departmentName = user.getDepartment().getName();
        }

        cs.beginText();
        cs.setFont(fontRegular, 10);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(sanitizeForPdf("Departament: " + departmentName));
        cs.endText();

        y -= 15;
        cs.beginText();
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(sanitizeForPdf("Cadru didactic: " + user.getFullName()));
        cs.endText();

        y -= 15;
        cs.beginText();
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(sanitizeForPdf("Perioada: " + months[timesheet.getMonth()] + " " + timesheet.getYear()));
        cs.endText();

        return y - 25;
    }

    private float drawTimesheetTable(PDPageContentStream cs, PDType1Font fontBold, PDType1Font fontRegular,
                                    Timesheet timesheet, float pageWidth, float y) throws IOException {
        float tableWidth = pageWidth - 2 * MARGIN;
        float colWidth = tableWidth / 8; // Prima coloană pentru ore + 7 zile

        // Header tabel
        float tableY = y;
        
        // Prima linie - header cu zilele săptămânii
        cs.setStrokingColor(0, 0, 0);
        cs.addRect(MARGIN, tableY - ROW_HEIGHT, tableWidth, ROW_HEIGHT);
        cs.stroke();

        // Celula "Interval"
        drawCell(cs, fontBold, "Interval", MARGIN, tableY, colWidth, ROW_HEIGHT, true);
        
        // Zile
        for (int i = 0; i < 7; i++) {
            drawCell(cs, fontBold, DAYS[i], MARGIN + (i + 1) * colWidth, tableY, colWidth, ROW_HEIGHT, true);
        }

        tableY -= ROW_HEIGHT;

        // Grupează intrările pe dată și interval
        Map<String, Map<LocalDate, TimesheetEntry>> entriesBySlotAndDate = new HashMap<>();
        for (TimesheetEntry entry : timesheet.getEntries()) {
            entriesBySlotAndDate
                .computeIfAbsent(entry.getTimeSlot(), k -> new HashMap<>())
                .put(entry.getEntryDate(), entry);
        }

        // Rânduri pentru fiecare interval orar
        for (String slot : TIME_SLOTS) {
            cs.addRect(MARGIN, tableY - ROW_HEIGHT, tableWidth, ROW_HEIGHT);
            cs.stroke();

            drawCell(cs, fontRegular, slot, MARGIN, tableY, colWidth, ROW_HEIGHT, false);

            Map<LocalDate, TimesheetEntry> slotEntries = entriesBySlotAndDate.getOrDefault(slot, new HashMap<>());
            
            // Pentru fiecare zi din lună, găsește intrarea corespunzătoare
            for (int dayIndex = 0; dayIndex < 7; dayIndex++) {
                String cellContent = "";
                String color = "";
                
                // Găsește toate intrările pentru această zi a săptămânii
                for (Map.Entry<LocalDate, TimesheetEntry> entry : slotEntries.entrySet()) {
                    if (entry.getKey().getDayOfWeek().getValue() == dayIndex + 1) {
                        TimesheetEntry te = entry.getValue();
                        cellContent = te.getHourType() == HourType.NORMA ? "N" : "P";
                        color = te.getHourType().getColorCode();
                        break;
                    }
                }
                
                drawCell(cs, fontRegular, cellContent, MARGIN + (dayIndex + 1) * colWidth, 
                        tableY, colWidth, ROW_HEIGHT, false);
            }

            tableY -= ROW_HEIGHT;
            
            // Verifică dacă am depășit pagina
            if (tableY < MARGIN + 100) {
                break; // În viitor: adaugă pagină nouă
            }
        }

        return tableY;
    }

    private void drawCell(PDPageContentStream cs, PDType1Font font, String text,
                         float x, float y, float width, float height, boolean header) throws IOException {
        // Desenează marginile celulei
        cs.addRect(x, y - height, width, height);
        cs.stroke();

        // Adaugă text
        if (text != null && !text.isEmpty()) {
            text = sanitizeForPdf(text);
            cs.beginText();
            cs.setFont(font, header ? 8 : 7);
            
            // Centrează textul
            float textWidth = font.getStringWidth(text) / 1000 * (header ? 8 : 7);
            float textX = x + (width - textWidth) / 2;
            float textY = y - height + CELL_PADDING;
            
            cs.newLineAtOffset(textX, textY);
            cs.showText(text);
            cs.endText();
        }
    }

    private void drawFooter(PDPageContentStream cs, PDType1Font fontBold, PDType1Font fontRegular,
                           Timesheet timesheet, float pageWidth, float y) throws IOException {
        y -= 30;
        
        // Totale
        cs.beginText();
        cs.setFont(fontBold, 10);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(sanitizeForPdf("TOTAL ORE:"));
        cs.endText();

        y -= 15;
        cs.beginText();
        cs.setFont(fontRegular, 10);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(sanitizeForPdf("Ore in norma: " + timesheet.getTotalNormaHours()));
        cs.endText();

        y -= 15;
        cs.beginText();
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(sanitizeForPdf("Ore plata cu ora: " + timesheet.getTotalPlataOraHours()));
        cs.endText();

        y -= 15;
        cs.beginText();
        cs.setFont(fontBold, 10);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(sanitizeForPdf("TOTAL: " + timesheet.getTotalHours() + " ore"));
        cs.endText();

        // Semnături
        y -= 40;
        cs.beginText();
        cs.setFont(fontRegular, 9);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(sanitizeForPdf("Semnatura cadru didactic: ____________________"));
        cs.endText();

        cs.beginText();
        cs.newLineAtOffset(pageWidth / 2, y);
        cs.showText(sanitizeForPdf("Data: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))));
        cs.endText();
    }
}
