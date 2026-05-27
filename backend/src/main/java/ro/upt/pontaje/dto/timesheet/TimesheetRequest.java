// ...existing code...
package ro.upt.pontaje.dto.timesheet;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO pentru crearea pontajului lunar
 */
public class TimesheetRequest {
    public Integer getMonth() { return month; }
    public Integer getYear() { return year; }

    public TimesheetRequest() {}
    public TimesheetRequest(Integer month, Integer year) {
        this.month = month;
        this.year = year;
    }

    @NotNull(message = "Luna este obligatorie")
    @Min(value = 1, message = "Luna trebuie să fie între 1 și 12")
    @Max(value = 12, message = "Luna trebuie să fie între 1 și 12")
    private Integer month;

    @NotNull(message = "Anul este obligatoriu")
    @Min(value = 2020, message = "Anul trebuie să fie cel puțin 2020")
    private Integer year;
}
