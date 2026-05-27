package ro.upt.pontaje.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ro.upt.pontaje.dto.schedule.ScheduleRequest;
import ro.upt.pontaje.dto.schedule.ScheduleResponse;
import ro.upt.pontaje.exception.BadRequestException;
import ro.upt.pontaje.exception.ResourceNotFoundException;
import ro.upt.pontaje.model.*;
import ro.upt.pontaje.repository.ScheduleRepository;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviciu pentru gestionarea orarului
 */
@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;

    public ScheduleService(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    /**
     * Găsește toate intrările din orar pentru un utilizator
     */
    @Transactional(readOnly = true)
    public List<ScheduleResponse> findByUser(User user) {
        return scheduleRepository.findByUserIdOrderByDayOfWeekAscTimeSlotAsc(user.getId())
            .stream()
            .map(ScheduleResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Găsește intrările din orar pentru un utilizator și o zi specifică
     */
    @Transactional(readOnly = true)
    public List<ScheduleResponse> findByUserAndDay(User user, DayOfWeek dayOfWeek) {
        return scheduleRepository.findByUserIdAndDayOfWeekOrderByTimeSlotAsc(user.getId(), dayOfWeek)
            .stream()
            .map(ScheduleResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Găsește o intrare din orar după ID
     */
    @Transactional(readOnly = true)
    public Schedule findById(UUID id) {
        return scheduleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Intrarea din orar nu a fost găsită"));
    }

    /**
     * Creează o nouă intrare în orar
     */
    @Transactional
    public ScheduleResponse create(User user, ScheduleRequest request) {
        // Verifică conflictele de orar
        if (scheduleRepository.hasTimeConflict(user.getId(), request.getDayOfWeek(), request.getTimeSlot())) {
            throw new BadRequestException("Există deja o intrare în orar pentru această zi și interval orar");
        }

        Schedule schedule = Schedule.builder()
            .user(user)
            .dayOfWeek(request.getDayOfWeek())
            .timeSlot(request.getTimeSlot())
            .discipline(request.getDiscipline())
            .room(request.getRoom())
            .activityType(request.getActivityType())
            .build();

        Schedule savedSchedule = scheduleRepository.save(schedule);
        return ScheduleResponse.fromEntity(savedSchedule);
    }

    /**
     * Actualizează o intrare din orar
     */
    @Transactional
    public ScheduleResponse update(UUID id, User user, ScheduleRequest request) {
        Schedule schedule = findById(id);

        // Verifică dacă intrarea aparține utilizatorului
        if (!schedule.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Nu aveți permisiunea de a modifica această intrare");
        }

        // Verifică conflictele de orar (excluzând intrarea curentă)
        if (scheduleRepository.hasTimeConflict(user.getId(), request.getDayOfWeek(), 
                                               request.getTimeSlot(), id)) {
            throw new BadRequestException("Există deja o intrare în orar pentru această zi și interval orar");
        }

        schedule.setDayOfWeek(request.getDayOfWeek());
        schedule.setTimeSlot(request.getTimeSlot());
        schedule.setDiscipline(request.getDiscipline());
        schedule.setRoom(request.getRoom());
        schedule.setActivityType(request.getActivityType());

        Schedule savedSchedule = scheduleRepository.save(schedule);
        return ScheduleResponse.fromEntity(savedSchedule);
    }

    /**
     * Șterge o intrare din orar
     */
    @Transactional
    public void delete(UUID id, User user) {
        Schedule schedule = findById(id);

        // Verifică dacă intrarea aparține utilizatorului
        if (!schedule.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Nu aveți permisiunea de a șterge această intrare");
        }

        scheduleRepository.delete(schedule);
    }

    /**
     * Importă orarul din fișier CSV
     * Format: Zi,Interval,Disciplina,Sala,TipActivitate
     */
    @Transactional
    public List<ScheduleResponse> importFromCsv(User user, MultipartFile file) throws IOException {
        List<ScheduleResponse> imported = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> rows = reader.readAll();
            
            // Skip header row
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (row.length >= 4) {
                    try {
                        ScheduleRequest request = ScheduleRequest.builder()
                            .dayOfWeek(parseDayOfWeek(row[0].trim()))
                            .timeSlot(row[1].trim())
                            .discipline(row[2].trim())
                            .room(row[3].trim())
                            .activityType(row.length > 4 ? parseActivityType(row[4].trim()) : ActivityType.CURS)
                            .build();

                        imported.add(create(user, request));
                    } catch (Exception e) {
                        // Skip invalid rows, log error
                    }
                }
            }
        } catch (CsvException e) {
            throw new BadRequestException("Eroare la parsarea fișierului CSV: " + e.getMessage());
        }

        return imported;
    }

    /**
     * Importă orarul din fișier Excel
     */
    @Transactional
    public List<ScheduleResponse> importFromExcel(User user, MultipartFile file) throws IOException {
        List<ScheduleResponse> imported = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    try {
                        ScheduleRequest request = ScheduleRequest.builder()
                            .dayOfWeek(parseDayOfWeek(getCellValue(row.getCell(0))))
                            .timeSlot(getCellValue(row.getCell(1)))
                            .discipline(getCellValue(row.getCell(2)))
                            .room(getCellValue(row.getCell(3)))
                            .activityType(row.getCell(4) != null ? 
                                parseActivityType(getCellValue(row.getCell(4))) : ActivityType.CURS)
                            .build();

                        imported.add(create(user, request));
                    } catch (Exception e) {
                        // Skip invalid rows
                    }
                }
            }
        }

        return imported;
    }

    /**
     * Șterge toate intrările din orar pentru un utilizator
     */
    @Transactional
    public void deleteAllForUser(User user) {
        scheduleRepository.deleteByUserId(user.getId());
    }

    // ========== Helper Methods ==========

    private DayOfWeek parseDayOfWeek(String value) {
        if (value == null || value.isEmpty()) {
            throw new BadRequestException("Ziua săptămânii este obligatorie");
        }
        
        String normalized = value.toUpperCase()
            .replace("Ț", "T")
            .replace("Ș", "S")
            .replace("Ă", "A")
            .replace("Î", "I")
            .replace("Â", "A");

        return switch (normalized) {
            case "LUNI", "L" -> DayOfWeek.LUNI;
            case "MARTI", "MA" -> DayOfWeek.MARTI;
            case "MIERCURI", "MI" -> DayOfWeek.MIERCURI;
            case "JOI", "J" -> DayOfWeek.JOI;
            case "VINERI", "V" -> DayOfWeek.VINERI;
            case "SAMBATA", "S" -> DayOfWeek.SAMBATA;
            case "DUMINICA", "D" -> DayOfWeek.DUMINICA;
            default -> throw new BadRequestException("Ziua săptămânii '" + value + "' nu este validă");
        };
    }

    private ActivityType parseActivityType(String value) {
        if (value == null || value.isEmpty()) {
            return ActivityType.CURS;
        }
        
        String normalized = value.toUpperCase();
        return switch (normalized) {
            case "CURS", "C" -> ActivityType.CURS;
            case "SEMINAR", "S" -> ActivityType.SEMINAR;
            case "LABORATOR", "L", "LAB" -> ActivityType.LABORATOR;
            case "PROIECT", "P" -> ActivityType.PROIECT;
            default -> ActivityType.CURS;
        };
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            default -> "";
        };
    }
}
