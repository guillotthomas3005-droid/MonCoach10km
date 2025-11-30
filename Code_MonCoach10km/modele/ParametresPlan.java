/* On encapsule ici les préférences de l'utilisateur pour la génération de son plan d'entraînement. */
/* La classe sert de "conteneur" pour transporter tous les choix renseignés avant la création effective du plan. */

package modele;

import java.io.Serializable;
import java.time.LocalDate;

/* Sérialisation nécessaire pour sauvegarder ou transmettre ces paramètres de configuration. */
public class ParametresPlan implements Serializable {
    
    private static final long serialVersionUID = 1L; // Identifiant pour la sérialisation.
    
    // Paramètres du plan propre à l'utilisateur.
    private int dureeEnSemaines;
    private String objectifChrono;
    private LocalDate dateDeDebut;
    private int nbSeancesChoisi; 
    
    /* Constructeur initialisant tous les paramètres pour la création du plan. */
    public ParametresPlan(int dureeEnSemaines, String objectifChrono, LocalDate dateDeDebut, int nbSeancesChoisi) {
        this.dureeEnSemaines = dureeEnSemaines;
        this.objectifChrono = objectifChrono;
        this.dateDeDebut = dateDeDebut;
        this.nbSeancesChoisi = nbSeancesChoisi; }

    /* Getteurs de la classe. */
    public int getDureeEnSemaines() { 
        return dureeEnSemaines; }
    
    public String getObjectifChrono() { 
        return objectifChrono; }
    
    public LocalDate getDateDeDebut() { 
        return dateDeDebut; }
    
    public int getNbSeancesChoisi() { 
        return nbSeancesChoisi; }
    
    /* On vérifie si l'utilisateur a bien défini un objectif de temps précis (non nul et non vide). */
    public boolean hasObjectifChrono() { 
        return objectifChrono != null && !objectifChrono.trim().isEmpty(); 
    }
}
