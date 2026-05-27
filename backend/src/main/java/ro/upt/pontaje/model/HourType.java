package ro.upt.pontaje.model;

/**
 * Enum pentru tipul de ora (in normă sau plata cu ora)
 */
public enum HourType {
    /**
     * Ore in norma de baza
     */
    NORMA("În normă", "#22C55E"),
    
    /**
     * Ore la plata cu ora
     */
    PLATA_ORA("Plata cu ora", "#F97316");
    
    private final String displayName;
    private final String colorCode;
    
    HourType(String displayName, String colorCode) {
        this.displayName = displayName;
        this.colorCode = colorCode;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getColorCode() {
        return colorCode;
    }
}
