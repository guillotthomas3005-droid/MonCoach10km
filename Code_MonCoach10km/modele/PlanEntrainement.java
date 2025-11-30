/* La classe PlanEntrainement est notre structure principale, elle regroupe toutes les informations d'un programme de course. */

package modele;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/* Sérialisation nécessaire pour la sauvegarde et la reconstruction de tout le profil utilisateur (semaines,séances). */
public class PlanEntrainement implements Serializable {
    
    private static final long serialVersionUID = 1L; // Identifiant pour la sérialisation.
    private String nomDuPlan;
    private LocalDate dateDebut;
    private String objectifChrono;
    private List<Semaine> semaines; // Structure du plan.

    /* Constructeur initialisant le plan avec les objectifs. */
    public PlanEntrainement(String nomDuPlan, LocalDate dateDebut, String objectifChrono) {
        this.nomDuPlan = nomDuPlan;
        this.dateDebut = dateDebut;
        this.objectifChrono = objectifChrono;
        this.semaines = new ArrayList<>(); // Initialise une liste vide pour les Semaines.
    }
    
    /* Méthode pour ajouter une nouvelle semaine au plan. */
    public void ajouterSemaine(Semaine semaine) { 
        this.semaines.add(semaine); 
    }
    
    /* Getteurs de la classe. */
    public String getNomDuPlan() { 
        return nomDuPlan; 
    }
    public LocalDate getDateDebut() { 
        return dateDebut; 
    }
    public String getObjectifChrono() { 
        return objectifChrono; 
    }
    public List<Semaine> getSemaines() { 
        return semaines; 
    }
    
    /* Méthode retournant le nombre total de semaines prévues dans le plan. */
    public int getDureeEnSemaines() { 
        return semaines.size(); 
    }
}
