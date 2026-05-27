package ro.upt.pontaje.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.upt.pontaje.dto.timesheet.TimesheetEntryRequest;
import ro.upt.pontaje.dto.timesheet.TimesheetRequest;
import ro.upt.pontaje.dto.timesheet.TimesheetResponse;
import ro.upt.pontaje.exception.BadRequestException;
import ro.upt.pontaje.exception.ResourceNotFoundException;
import ro.upt.pontaje.model.*;
import ro.upt.pontaje.repository.TimesheetEntryRepository;
import ro.upt.pontaje.repository.TimesheetRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviciu pentru gestionarea pontajelor
 */
@Service
public class TimesheetService {

    private final TimesheetRepository timesheetRepository;
    private final TimesheetEntryRepository timesheetEntryRepository;
    private final NotificationService notificationService;

    public TimesheetService(TimesheetRepository timesheetRepository,
                            TimesheetEntryRepository timesheetEntryRepository,
                            NotificationService notificationService) {
        this.timesheetRepository = timesheetRepository;
        this.timesheetEntryRepository = timesheetEntryRepository;
        this.notificationService = notificationService;
    }

    /**
     * Găsește toate pontajele pentru un utilizator
     */
    @Transactional(readOnly = true)
    public List<TimesheetResponse> findByUser(User user) {
        return timesheetRepository.findByUserIdOrderByYearDescMonthDesc(user.getId())
            .stream()
            .map(TimesheetResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Găsește pontajul pentru o lună și an specific
     */
    @Transactional(readOnly = true)
    public TimesheetResponse findByUserAndPeriod(User user, Integer month, Integer year) {
        Timesheet timesheet = timesheetRepository.findByUserIdAndMonthAndYear(user.getId(), month, year)
            .orElseThrow(() -> new ResourceNotFoundException("Pontajul nu a fost găsit pentru perioada specificată"));
        return TimesheetResponse.fromEntity(timesheet, true);
    }

    /**
     * Găsește un pontaj după ID
     */
    @Transactional(readOnly = true)
    public Timesheet findById(UUID id) {
        return timesheetRepository.findByIdWithUser(id)
            .orElseThrow(() -> new ResourceNotFoundException("Pontajul nu a fost găsit"));
    }

    /**
     * Creează sau returnează pontajul existent pentru o perioadă
     */
    @Transactional
    public TimesheetResponse getOrCreate(User user, TimesheetRequest request) {
        // Verifică dacă există deja
        return timesheetRepository.findByUserIdAndMonthAndYear(
                user.getId(), request.getMonth(), request.getYear())
            .map(t -> TimesheetResponse.fromEntity(t, true))
            .orElseGet(() -> create(user, request));
    }

    /**
     * Creează un pontaj nou
     */
    @Transactional
    public TimesheetResponse create(User user, TimesheetRequest request) {
        // Verifică dacă există deja
        if (timesheetRepository.existsByUserIdAndMonthAndYear(
                user.getId(), request.getMonth(), request.getYear())) {
            throw new BadRequestException("Pontajul pentru această perioadă există deja");
        }

        Timesheet timesheet = Timesheet.builder()
            .user(user)
            .month(request.getMonth())
            .year(request.getYear())
            .status(TimesheetStatus.DRAFT)
            .build();

        Timesheet savedTimesheet = timesheetRepository.save(timesheet);
        // Reload cu user+department eager pentru a evita LazyInitializationException
        Timesheet reloaded = timesheetRepository.findByIdWithUser(savedTimesheet.getId())
            .orElse(savedTimesheet);
        return TimesheetResponse.fromEntity(reloaded, true);
    }

    /**
     * Adaugă sau actualizează o intrare în pontaj (marcare ore)
     */
    @Transactional
    public TimesheetResponse addEntry(UUID timesheetId, User user, TimesheetEntryRequest request) {
        Timesheet timesheet = findById(timesheetId);

        // Verifică dacă pontajul aparține utilizatorului
        if (!timesheet.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Nu aveți permisiunea de a modifica acest pontaj");
        }

        // Verifică dacă pontajul poate fi editat
        if (!timesheet.isEditable()) {
            throw new BadRequestException("Pontajul a fost deja trimis și nu mai poate fi modificat");
        }

        // Verifică dacă data este în luna corectă
        if (request.getEntryDate().getMonthValue() != timesheet.getMonth() ||
            request.getEntryDate().getYear() != timesheet.getYear()) {
            throw new BadRequestException("Data nu corespunde perioadei pontajului");
        }

        // Verifică dacă există deja intrare pentru această dată și interval
        TimesheetEntry existingEntry = timesheet.getEntries().stream()
            .filter(e -> e.getEntryDate().equals(request.getEntryDate()) && 
                        e.getTimeSlot().equals(request.getTimeSlot()))
            .findFirst()
            .orElse(null);

        if (existingEntry != null) {
            // Actualizează intrarea existentă
            existingEntry.setHourType(request.getHourType());
            existingEntry.setActivity(request.getActivity());
            timesheetEntryRepository.save(existingEntry);
        } else {
            // Creează intrare nouă
            TimesheetEntry entry = TimesheetEntry.builder()
                .timesheet(timesheet)
                .entryDate(request.getEntryDate())
                .timeSlot(request.getTimeSlot())
                .hourType(request.getHourType())
                .activity(request.getActivity())
                .build();
            timesheet.getEntries().add(entry);
            timesheetEntryRepository.save(entry);
        }

        return TimesheetResponse.fromEntity(timesheet, true);
    }

    /**
     * Șterge o intrare din pontaj
     */
    @Transactional
    public TimesheetResponse deleteEntry(UUID timesheetId, UUID entryId, User user) {
        Timesheet timesheet = findById(timesheetId);

        // Verifică dacă pontajul aparține utilizatorului
        if (!timesheet.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Nu aveți permisiunea de a modifica acest pontaj");
        }

        // Verifică dacă pontajul poate fi editat
        if (!timesheet.isEditable()) {
            throw new BadRequestException("Pontajul a fost deja trimis și nu mai poate fi modificat");
        }

        TimesheetEntry entry = timesheetEntryRepository.findById(entryId)
            .orElseThrow(() -> new ResourceNotFoundException("Intrarea nu a fost găsită"));

        if (!entry.getTimesheet().getId().equals(timesheetId)) {
            throw new BadRequestException("Intrarea nu aparține acestui pontaj");
        }

        timesheet.getEntries().remove(entry);
        timesheetEntryRepository.delete(entry);

        return TimesheetResponse.fromEntity(timesheet, true);
    }

    /**
     * Trimite pontajul către secretariat
     */
    @Transactional
    public TimesheetResponse submit(UUID timesheetId, User user) {
        Timesheet timesheet = findById(timesheetId);

        // Verifică dacă pontajul aparține utilizatorului
        if (!timesheet.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Nu aveți permisiunea de a trimite acest pontaj");
        }

        // Verifică dacă pontajul poate fi trimis
        if (timesheet.getStatus() != TimesheetStatus.DRAFT) {
            throw new BadRequestException("Pontajul a fost deja trimis");
        }

        // Verifică dacă are intrări
        if (timesheet.getEntries().isEmpty()) {
            throw new BadRequestException("Pontajul nu are ore marcate. Completați pontajul înainte de a-l trimite.");
        }

        timesheet.submit();
        Timesheet savedTimesheet = timesheetRepository.save(timesheet);

        notificationService.create(
            user,
            NotificationType.SYSTEM,
            "Pontaj trimis",
            "Pontajul tău pentru " + timesheet.getPeriodDisplay() + " a fost trimis cu succes și așteaptă aprobarea secretariatului."
        );

        return TimesheetResponse.fromEntity(savedTimesheet, true);
    }

    /**
     * Aprobă un pontaj (secretariat/admin)
     */
    @Transactional
    public TimesheetResponse approve(UUID timesheetId) {
        Timesheet timesheet = findById(timesheetId);

        if (timesheet.getStatus() != TimesheetStatus.SUBMITTED) {
            throw new BadRequestException("Doar pontajele trimise pot fi aprobate");
        }

        timesheet.setStatus(TimesheetStatus.APPROVED);
        Timesheet saved = timesheetRepository.save(timesheet);

        notificationService.create(
            timesheet.getUser(),
            NotificationType.SYSTEM,
            "Pontaj aprobat",
            "Pontajul tău pentru " + timesheet.getPeriodDisplay() + " a fost aprobat de secretariat."
        );

        return TimesheetResponse.fromEntity(saved, true);
    }

    // ========== Metode pentru Secretariat ==========

    /**
     * Găsește toate pontajele pentru o perioadă
     */
    @Transactional(readOnly = true)
    public List<TimesheetResponse> findAllByPeriod(Integer month, Integer year) {
        return timesheetRepository.findByMonthAndYearOrderByUserLastNameAsc(month, year)
            .stream()
            .map(TimesheetResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Găsește pontajele trimise pentru o perioadă
     */
    @Transactional(readOnly = true)
    public List<TimesheetResponse> findSubmittedByPeriod(Integer month, Integer year) {
        return timesheetRepository.findByMonthAndYearAndStatusOrderByUserLastNameAsc(
                month, year, TimesheetStatus.SUBMITTED)
            .stream()
            .map(t -> TimesheetResponse.fromEntity(t, true))
            .collect(Collectors.toList());
    }

    /**
     * Găsește pontajele dintr-un departament
     */
    @Transactional(readOnly = true)
    public List<TimesheetResponse> findByDepartmentAndPeriod(
            UUID departmentId, Integer month, Integer year) {
        return timesheetRepository.findByDepartmentAndPeriod(departmentId, month, year)
            .stream()
            .map(TimesheetResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Găsește pontajele pentru o perioadă și un anumit status
     */
    @Transactional(readOnly = true)
    public List<TimesheetResponse> findByPeriodAndStatus(Integer month, Integer year, TimesheetStatus status) {
        return timesheetRepository.findByMonthAndYearAndStatusOrderByUserLastNameAsc(month, year, status)
            .stream()
            .map(TimesheetResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Găsește entitățile Timesheet pentru o perioadă și o listă de statusuri
     * (util pentru generarea în masă a documentelor PDF pentru secretariat)
     */
    @Transactional(readOnly = true)
    public List<Timesheet> findEntitiesByPeriodAndStatuses(Integer month, Integer year, List<TimesheetStatus> statuses) {
        return timesheetRepository.findByMonthAndYearOrderByUserLastNameAsc(month, year)
            .stream()
            .filter(t -> statuses.contains(t.getStatus()))
            .collect(Collectors.toList());
    }

    /**
     * Returnează ID-urile utilizatorilor care nu au trimis pontajul
     */
    @Transactional(readOnly = true)
    public List<UUID> findUsersWithoutSubmittedTimesheet(Integer month, Integer year) {
        return timesheetRepository.findUsersWithoutSubmittedTimesheet(month, year);
    }
}
