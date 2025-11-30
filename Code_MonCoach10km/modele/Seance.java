/* On gère tous les paramètres que peut avoir une séance, de la création jusqu'au statut de réalisation. */

package modele;

import java.io.Serializable;
import java.time.LocalDate;

/* On a besoin de la sérialisation pour conserver les données de chaque séance préalablement générée. */
public class Seance implements Serializable {
    
    private static final long serialVersionUID = 2L; // Identifiant pour la sérialisation.
    
    // Attributs définis à la création.
    private LocalDate date;
    private TypeSeance type;
    private String description;
    private String intensitePrevue;
    private int dureePrevueEnMinutes;
    private double distancePrevueEnKm;
    
    // Attributs modifiables par l'utilisateur.
    private StatutSeance statut;
    private String motifAnnulation;

    /* Constructeur initialisant la séance avec les données de planification et le statut PREVUE (par défaut). */
    public Seance(LocalDate date, TypeSeance type, String description, String intensitePrevue, int dureePrevueEnMinutes, double distancePrevueEnKm) {
        this.date = date;
        this.type = type;
        this.description = description;
        this.intensitePrevue = intensitePrevue;
        this.dureePrevueEnMinutes = dureePrevueEnMinutes;
        this.distancePrevueEnKm = distancePrevueEnKm;
        this.statut = StatutSeance.PREVUE;
        this.motifAnnulation = "";
    }

    /* Getteurs et Setteurs de la classe. */
    
    public LocalDate getDate() { 
        return date; } 
    public void setDate(LocalDate date) { 
        this.date = date; }
    
    public TypeSeance getType() { 
        return type; }
    
    public String getDescription() { 
        return description; }
    public void setDescription(String description) { 
        this.description = description; }
    
    public String getIntensitePrevue() { 
        return intensitePrevue; }
    
    public int getDureePrevueEnMinutes() { 
        return dureePrevueEnMinutes; }
    
    public double getDistancePrevueEnKm() { 
        return distancePrevueEnKm; }
    
    /* Gestion du statut de la séance (Validation / Annulation). */
    public StatutSeance getStatut() { 
        return statut; } 
    public void setStatut(StatutSeance statut) { 
        this.statut = statut; }
    
    public String getMotifAnnulation() { 
        return motifAnnulation; } 
    public void setMotifAnnulation(String m) { 
        this.motifAnnulation = m; }
}
