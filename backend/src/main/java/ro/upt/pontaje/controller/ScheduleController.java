package ro.upt.pontaje.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ro.upt.pontaje.dto.schedule.ScheduleRequest;
import ro.upt.pontaje.dto.schedule.ScheduleResponse;
import ro.upt.pontaje.model.DayOfWeek;
import ro.upt.pontaje.model.User;
import ro.upt.pontaje.service.ScheduleService;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Controller pentru gestionarea orarului
 */
@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /**
     * Returnează orarul utilizatorului curent
     * GET /api/schedules
     */
    @GetMapping
    public ResponseEntity<List<ScheduleResponse>> getMySchedule(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) DayOfWeek day) {
        
        List<ScheduleResponse> schedules;
        if (day != null) {
            schedules = scheduleService.findByUserAndDay(user, day);
        } else {
            schedules = scheduleService.findByUser(user);
        }
        
        return ResponseEntity.ok(schedules);
    }

    /**
     * Adaugă o intrare în orar
     * POST /api/schedules
     */
    @PostMapping
    public ResponseEntity<ScheduleResponse> createScheduleEntry(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ScheduleRequest request) {
        ScheduleResponse schedule = scheduleService.create(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(schedule);
    }

    /**
     * Actualizează o intrare din orar
     * PUT /api/schedules/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ScheduleResponse> updateScheduleEntry(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ScheduleRequest request) {
        ScheduleResponse schedule = scheduleService.update(id, user, request);
        return ResponseEntity.ok(schedule);
    }

    /**
     * Șterge o intrare din orar
     * DELETE /api/schedules/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScheduleEntry(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        scheduleService.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    /**
     * Importă orarul din fișier CSV
     * POST /api/schedules/import/csv
     */
    @PostMapping(value = "/import/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ScheduleResponse>> importFromCsv(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) throws IOException {
        List<ScheduleResponse> schedules = scheduleService.importFromCsv(user, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(schedules);
    }

    /**
     * Importă orarul din fișier Excel
     * POST /api/schedules/import/excel
     */
    @PostMapping(value = "/import/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ScheduleResponse>> importFromExcel(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) throws IOException {
        List<ScheduleResponse> schedules = scheduleService.importFromExcel(user, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(schedules);
    }

    /**
     * Șterge toate intrările din orar
     * DELETE /api/schedules/all
     */
    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllScheduleEntries(@AuthenticationPrincipal User user) {
        scheduleService.deleteAllForUser(user);
        return ResponseEntity.noContent().build();
    }
}
