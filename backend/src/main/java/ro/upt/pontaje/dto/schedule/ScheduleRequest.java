package ro.upt.pontaje.dto.schedule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import ro.upt.pontaje.model.ActivityType;
import ro.upt.pontaje.model.DayOfWeek;

/**
 * DTO pentru crearea/actualizarea intrării în orar
 */

public class ScheduleRequest {

    @NotNull(message = "Ziua săptămânii este obligatorie")
    private DayOfWeek dayOfWeek;

    @NotBlank(message = "Intervalul orar este obligatorie")
    @Pattern(regexp = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]-([0-1]?[0-9]|2[0-3]):[0-5][0-9]$", 
             message = "Intervalul orar trebuie să fie în format HH:MM-HH:MM")
    private String timeSlot;

    @NotBlank(message = "Disciplina este obligatorie")
    private String discipline;

    private String room;

    @NotNull(message = "Tipul activității este obligatorie")
    private ActivityType activityType;

    public ScheduleRequest() {}

    public ScheduleRequest(DayOfWeek dayOfWeek, String timeSlot, String discipline, String room, ActivityType activityType) {
        this.dayOfWeek = dayOfWeek;
        this.timeSlot = timeSlot;
        this.discipline = discipline;
        this.room = room;
        this.activityType = activityType;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    public String getDiscipline() {
        return discipline;
    }

    public void setDiscipline(String discipline) {
        this.discipline = discipline;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public void setActivityType(ActivityType activityType) {
        this.activityType = activityType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private DayOfWeek dayOfWeek;
        private String timeSlot;
        private String discipline;
        private String room;
        private ActivityType activityType;

        public Builder dayOfWeek(DayOfWeek dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
            return this;
        }

        public Builder timeSlot(String timeSlot) {
            this.timeSlot = timeSlot;
            return this;
        }

        public Builder discipline(String discipline) {
            this.discipline = discipline;
            return this;
        }

        public Builder room(String room) {
            this.room = room;
            return this;
        }

        public Builder activityType(ActivityType activityType) {
            this.activityType = activityType;
            return this;
        }

        public ScheduleRequest build() {
            return new ScheduleRequest(dayOfWeek, timeSlot, discipline, room, activityType);
        }
    }
}
