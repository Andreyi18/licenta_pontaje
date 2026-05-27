// ...existing code...
package ro.upt.pontaje.dto.timesheet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import ro.upt.pontaje.model.HourType;

import java.time.LocalDate;

/**
 * DTO pentru adăugarea unei intrări în pontaj (marcare ore)
 */
public class TimesheetEntryRequest {
    public LocalDate getEntryDate() { return entryDate; }
    public String getTimeSlot() { return timeSlot; }
    public HourType getHourType() { return hourType; }
    public String getActivity() { return activity; }

    public TimesheetEntryRequest() {}
    public TimesheetEntryRequest(LocalDate entryDate, String timeSlot, HourType hourType, String activity) {
        this.entryDate = entryDate;
        this.timeSlot = timeSlot;
        this.hourType = hourType;
        this.activity = activity;
    }

    @NotNull(message = "Data este obligatorie")
    private LocalDate entryDate;

    @NotBlank(message = "Intervalul orar este obligatoriu")
    @Pattern(regexp = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]-([0-1]?[0-9]|2[0-3]):[0-5][0-9]$", 
             message = "Intervalul orar trebuie să fie în format HH:MM-HH:MM")
    private String timeSlot;

    @NotNull(message = "Tipul de oră este obligatoriu")
    private HourType hourType;

    private String activity;
}
