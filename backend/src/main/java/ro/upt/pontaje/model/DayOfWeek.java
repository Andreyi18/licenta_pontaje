package ro.upt.pontaje.model;

/**
 * Enum pentru zilele saptamânii
 */
public enum DayOfWeek {
    LUNI("Luni", 1),
    MARTI("Marți", 2),
    MIERCURI("Miercuri", 3),
    JOI("Joi", 4),
    VINERI("Vineri", 5),
    SAMBATA("Sâmbătă", 6),
    DUMINICA("Duminică", 7);
    
    private final String displayName;
    private final int order;
    
    DayOfWeek(String displayName, int order) {
        this.displayName = displayName;
        this.order = order;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getOrder() {
        return order;
    }
}
