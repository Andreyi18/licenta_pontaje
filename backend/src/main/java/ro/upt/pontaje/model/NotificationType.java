package ro.upt.pontaje.model;

/**
 * Enum pentru tipul de notificare
 */
public enum NotificationType {
    /**
     * Reminder automat pentru deadline pontaj
     */
    REMINDER("Reminder"),
    
    /**
     * Notificare de sistem (ex: confirmare trimitere)
     */
    SYSTEM("Sistem"),
    
    /**
     * Notificare despre deadline apropiat
     */
    DEADLINE("Deadline");
    
    private final String displayName;
    
    NotificationType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
