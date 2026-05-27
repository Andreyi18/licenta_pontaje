package ro.upt.pontaje.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ro.upt.pontaje.model.*;
import ro.upt.pontaje.model.Document;
import ro.upt.pontaje.repository.DocumentRepository;
import ro.upt.pontaje.service.PdfGeneratorService;
import ro.upt.pontaje.service.TimesheetService;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Controller pentru generarea și descărcarea documentelor PDF
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final PdfGeneratorService pdfGeneratorService;
    private final TimesheetService timesheetService;
    private final DocumentRepository documentRepository;

    public DocumentController(PdfGeneratorService pdfGeneratorService, TimesheetService timesheetService, DocumentRepository documentRepository) {
        this.pdfGeneratorService = pdfGeneratorService;
        this.timesheetService = timesheetService;
        this.documentRepository = documentRepository;
    }

    /**
     * Generează PDF pentru un pontaj
     * POST /api/documents/generate
     */
    @PostMapping("/generate")
    public ResponseEntity<DocumentResponse> generateDocument(
            @AuthenticationPrincipal User user,
            @RequestParam UUID timesheetId,
            @RequestParam AnnexType annexType) throws IOException {
        
        Timesheet timesheet = timesheetService.findById(timesheetId);
        
        // Verifică dacă pontajul aparține utilizatorului
        if (!timesheet.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }
        
        Document document = pdfGeneratorService.generateAnnex(timesheet, annexType);
        
        return ResponseEntity.ok(new DocumentResponse(
            document.getId(),
            document.getFileName(),
            document.getAnnexType(),
            document.getGeneratedAt().toString()
        ));
    }

    /**
     * Descarcă un document PDF
     * GET /api/documents/{id}/download
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable UUID id) throws IOException {
        Document document = documentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Document not found"));
        
        byte[] content = pdfGeneratorService.downloadDocument(id);
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + document.getFileName() + "\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(content);
    }

    /**
     * Returnează documentele unui utilizator
     * GET /api/documents
     */
    @GetMapping
    public ResponseEntity<List<DocumentListResponse>> getMyDocuments(@AuthenticationPrincipal User user) {
        List<DocumentListResponse> docs = documentRepository
                .findByUserIdOrderByGeneratedAtDesc(user.getId())
                .stream()
                .map(d -> new DocumentListResponse(
                        d.getId(),
                        d.getUser().getId(),
                        d.getTimesheet().getId(),
                        d.getAnnexType(),
                        d.getFilePath(),
                        d.getFileName(),
                        d.getGeneratedAt() != null ? d.getGeneratedAt().toString() : ""
                ))
                .toList();
        return ResponseEntity.ok(docs);
    }

    /**
     * DTO pentru răspuns generare document
     */
    private record DocumentResponse(
        UUID id,
        String fileName,
        AnnexType annexType,
        String generatedAt
    ) {}

    /**
     * DTO pentru lista documente (câmpuri plate, fără relații JPA)
     */
    private record DocumentListResponse(
        UUID id,
        UUID userId,
        UUID timesheetId,
        AnnexType annexType,
        String filePath,
        String fileName,
        String generatedAt
    ) {}
}
