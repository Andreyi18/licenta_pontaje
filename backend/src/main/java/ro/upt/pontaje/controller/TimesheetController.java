package ro.upt.pontaje.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ro.upt.pontaje.dto.timesheet.TimesheetEntryRequest;
import ro.upt.pontaje.dto.timesheet.TimesheetRequest;
import ro.upt.pontaje.dto.timesheet.TimesheetResponse;
import ro.upt.pontaje.model.User;
import ro.upt.pontaje.service.TimesheetService;

import java.util.List;
import java.util.UUID;

/**
 * Controller pentru gestionarea pontajelor
 */
@RestController
@RequestMapping("/api/timesheets")
public class TimesheetController {

    private final TimesheetService timesheetService;

    public TimesheetController(TimesheetService timesheetService) {
        this.timesheetService = timesheetService;
    }

    /**
     * Returnează toate pontajele utilizatorului curent
     * GET /api/timesheets
     */
    @GetMapping
    public ResponseEntity<List<TimesheetResponse>> getMyTimesheets(@AuthenticationPrincipal User user) {
        List<TimesheetResponse> timesheets = timesheetService.findByUser(user);
        return ResponseEntity.ok(timesheets);
    }

    /**
     * Returnează pontajul pentru o lună și an specific
     * GET /api/timesheets/{month}/{year}
     */
    @GetMapping("/{month}/{year}")
    public ResponseEntity<TimesheetResponse> getTimesheetByPeriod(
            @AuthenticationPrincipal User user,
            @PathVariable Integer month,
            @PathVariable Integer year) {
        TimesheetResponse timesheet = timesheetService.findByUserAndPeriod(user, month, year);
        return ResponseEntity.ok(timesheet);
    }

    /**
     * Creează sau returnează pontajul existent pentru o perioadă
     * POST /api/timesheets
     */
    @PostMapping
    public ResponseEntity<TimesheetResponse> getOrCreateTimesheet(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TimesheetRequest request) {
        TimesheetResponse timesheet = timesheetService.getOrCreate(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(timesheet);
    }

    /**
     * Adaugă sau actualizează o intrare în pontaj (marcare ore)
     * POST /api/timesheets/{id}/entries
     */
    @PostMapping("/{id}/entries")
    public ResponseEntity<TimesheetResponse> addEntry(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TimesheetEntryRequest request) {
        TimesheetResponse timesheet = timesheetService.addEntry(id, user, request);
        return ResponseEntity.ok(timesheet);
    }

    /**
     * Șterge o intrare din pontaj
     * DELETE /api/timesheets/{id}/entries/{entryId}
     */
    @DeleteMapping("/{id}/entries/{entryId}")
    public ResponseEntity<TimesheetResponse> deleteEntry(
            @PathVariable UUID id,
            @PathVariable UUID entryId,
            @AuthenticationPrincipal User user) {
        TimesheetResponse timesheet = timesheetService.deleteEntry(id, entryId, user);
        return ResponseEntity.ok(timesheet);
    }

    /**
     * Trimite pontajul către secretariat
     * POST /api/timesheets/{id}/submit
     */
    @PostMapping("/{id}/submit")
    public ResponseEntity<TimesheetResponse> submitTimesheet(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        TimesheetResponse timesheet = timesheetService.submit(id, user);
        return ResponseEntity.ok(timesheet);
    }
}
