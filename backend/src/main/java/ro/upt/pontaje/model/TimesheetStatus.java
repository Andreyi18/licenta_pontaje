package ro.upt.pontaje.model;

/**
 * Enum pentru statusul pontajului
 */
public enum TimesheetStatus {
    /**
     * Pontaj în lucru - poate fi editat
     */
    DRAFT("Draft"),
    
    /**
     * Pontaj trimis către secretariat
     */
    SUBMITTED("Trimis"),
    
    /**
     * Pontaj aprobat de secretariat
     */
    APPROVED("Aprobat");
    
    private final String displayName;
    
    TimesheetStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
