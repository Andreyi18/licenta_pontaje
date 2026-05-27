package ro.upt.pontaje.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.upt.pontaje.dto.timesheet.TimesheetResponse;
import ro.upt.pontaje.exception.BadRequestException;
import ro.upt.pontaje.model.AnnexType;
import ro.upt.pontaje.model.Document;
import ro.upt.pontaje.model.Timesheet;
import ro.upt.pontaje.model.TimesheetStatus;
import ro.upt.pontaje.repository.DocumentRepository;
import ro.upt.pontaje.service.PdfGeneratorService;
import ro.upt.pontaje.service.TimesheetService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Controller pentru funcționalitățile secretariatului
 */
@RestController
@RequestMapping("/api/secretariat")
@PreAuthorize("hasAnyRole('SECRETARIAT', 'ADMIN')")
public class SecretariatController {

    private final TimesheetService timesheetService;
    private final DocumentRepository documentRepository;
    private final PdfGeneratorService pdfGeneratorService;

    public SecretariatController(TimesheetService timesheetService, DocumentRepository documentRepository, PdfGeneratorService pdfGeneratorService) {
        this.timesheetService = timesheetService;
        this.documentRepository = documentRepository;
        this.pdfGeneratorService = pdfGeneratorService;
    }

    /**
     * Returnează toate pontajele pentru o perioadă
     * GET /api/secretariat/timesheets
     */
    @GetMapping("/timesheets")
    public ResponseEntity<List<TimesheetResponse>> getAllTimesheets(
            @RequestParam Integer month,
            @RequestParam Integer year,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) TimesheetStatus status) {
        
        List<TimesheetResponse> timesheets;
        
        if (departmentId != null) {
            timesheets = timesheetService.findByDepartmentAndPeriod(departmentId, month, year);
        } else if (status != null) {
            // Filtrare explicită după status (SUBMITTED / APPROVED / DRAFT)
            timesheets = timesheetService.findByPeriodAndStatus(month, year, status);
        } else {
            timesheets = timesheetService.findAllByPeriod(month, year);
        }
        
        return ResponseEntity.ok(timesheets);
    }

    /**
     * Returnează statistici pontaje pentru o perioadă
     * GET /api/secretariat/timesheets/status
     */
    @GetMapping("/timesheets/status")
    public ResponseEntity<TimesheetStatusSummary> getTimesheetStatus(
            @RequestParam Integer month,
            @RequestParam Integer year) {
        
        List<TimesheetResponse> allTimesheets = timesheetService.findAllByPeriod(month, year);
        
        long draft = allTimesheets.stream()
            .filter(t -> t.getStatus() == TimesheetStatus.DRAFT).count();
        long submitted = allTimesheets.stream()
            .filter(t -> t.getStatus() == TimesheetStatus.SUBMITTED).count();
        long approved = allTimesheets.stream()
            .filter(t -> t.getStatus() == TimesheetStatus.APPROVED).count();
        
        List<UUID> missingUsers = timesheetService.findUsersWithoutSubmittedTimesheet(month, year);
        
        return ResponseEntity.ok(new TimesheetStatusSummary(
            allTimesheets.size(),
            (int) draft,
            (int) submitted,
            (int) approved,
            missingUsers.size()
        ));
    }

    /**
     * Concatenează PDF-urile selectate într-un singur document
     * POST /api/secretariat/documents/merge
     */
    @PostMapping("/documents/merge")
    public ResponseEntity<byte[]> mergeDocuments(
            @RequestParam Integer month,
            @RequestParam Integer year,
            @RequestParam(required = false) UUID departmentId) throws IOException {
        
        List<Document> documents;

        if (departmentId != null) {
            documents = documentRepository.findByDepartmentAndPeriod(departmentId, month, year);
        } else {
            documents = documentRepository.findByPeriod(month, year);
        }

        if (documents.isEmpty()) {
            // Nu există încă documente – generăm în masă pentru pontajele SUBMITTED/APPROVED
            List<Timesheet> timesheetsForDocs = timesheetService
                    .findEntitiesByPeriodAndStatuses(month, year,
                            List.of(TimesheetStatus.SUBMITTED, TimesheetStatus.APPROVED));

            if (timesheetsForDocs.isEmpty()) {
                throw new BadRequestException("Nu există pontaje trimise sau aprobate pentru perioada selectată");
            }

            for (Timesheet t : timesheetsForDocs) {
                if (!documentRepository.existsByTimesheetIdAndAnnexType(t.getId(), AnnexType.ANEXA_1)) {
                    pdfGeneratorService.generateAnnex(t, AnnexType.ANEXA_1);
                }
            }

            // Reîncărcăm documentele după generare
            if (departmentId != null) {
                documents = documentRepository.findByDepartmentAndPeriod(departmentId, month, year);
            } else {
                documents = documentRepository.findByPeriod(month, year);
            }

            if (documents.isEmpty()) {
                throw new BadRequestException("Nu există documente generate pentru perioada selectată");
            }
        }

        byte[] mergedPdf = pdfGeneratorService.mergePdfs(documents);
        
        String fileName = String.format("pontaje_consolidate_%d_%d_%s.pdf",
            month, year, 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(mergedPdf);
    }

    /**
     * Returnează pontajul detaliat pentru un utilizator
     * GET /api/secretariat/timesheets/{timesheetId}
     */
    @GetMapping("/timesheets/{timesheetId}")
    public ResponseEntity<TimesheetResponse> getTimesheetDetails(@PathVariable UUID timesheetId) {
        var timesheet = timesheetService.findById(timesheetId);
        return ResponseEntity.ok(TimesheetResponse.fromEntity(timesheet, true));
    }

    /**
     * Aprobă un pontaj
     * POST /api/secretariat/timesheets/{timesheetId}/approve
     */
    @PostMapping("/timesheets/{timesheetId}/approve")
    public ResponseEntity<TimesheetResponse> approveTimesheet(@PathVariable UUID timesheetId) {
        TimesheetResponse approved = timesheetService.approve(timesheetId);
        return ResponseEntity.ok(approved);
    }

    /**
     * DTO pentru sumar status pontaje
     */
    private record TimesheetStatusSummary(
        int total,
        int draft,
        int submitted,
        int approved,
        int missing
    ) {}
}
