package ro.upt.pontaje.model;

/**
 * Enum pentru tipul de activitate didactica
 */
public enum ActivityType {
    CURS("Curs", "C"),
    SEMINAR("Seminar", "S"),
    LABORATOR("Laborator", "L"),
    PROIECT("Proiect", "P");
    
    private final String displayName;
    private final String shortCode;
    
    ActivityType(String displayName, String shortCode) {
        this.displayName = displayName;
        this.shortCode = shortCode;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getShortCode() {
        return shortCode;
    }
}
