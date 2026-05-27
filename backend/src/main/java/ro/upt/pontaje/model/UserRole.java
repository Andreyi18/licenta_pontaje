package ro.upt.pontaje.model;

/**
 * Enum pentru rolurile utilizatorilor în sistem
 */
public enum UserRole {
    /**
     * Cadru didactic - poate vizualiza/edita propriul orar și pontaj
     */
    CADRU_DIDACTIC,
    
    /**
     * Secretariat - poate vizualiza pontajele tuturor, genera documente consolidate
     */
    SECRETARIAT,
    
    /**
     * Administrator - acces complet la sistem, gestionare utilizatori
     */
    ADMIN
}
