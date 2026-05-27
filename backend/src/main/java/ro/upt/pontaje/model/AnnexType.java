package ro.upt.pontaje.model;

/**
 * Enum pentru tipul de anexă salariala
 */
public enum AnnexType {
    /**
     * Anexa 1 - pentru titulari si CMDD (Contract de Munca pe Durată Determinata)
     */
    ANEXA_1("Anexa 1", "Evidența numărului de ore lucrate de către cadrele didactice titulare sau cu contract de muncă pe durată determinată"),
    
    /**
     * Anexa 3 - pentru doctoranzi si pensionari
     */
    ANEXA_3("Anexa 3", "Evidența numărului de ore de conducere doctorat");
    
    private final String displayName;
    private final String fullTitle;
    
    AnnexType(String displayName, String fullTitle) {
        this.displayName = displayName;
        this.fullTitle = fullTitle;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getFullTitle() {
        return fullTitle;
    }
}
