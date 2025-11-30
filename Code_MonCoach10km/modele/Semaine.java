/* On gère ici les différentes semaines d'entraînement avec leur numéro et la liste des séances. */
/* Cette classe fait le lien entre le Plan et les Séances. */

package modele;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/* Sérialisation nécessaire pour conserver nos structures de semaines d'entraînement (numéro et liste de séances). */
public class Semaine implements Serializable {
    
    private static final long serialVersionUID = 1L; // Identifiant pour la sérialisation.
    private int numero;
    private List<Seance> seances;

    /* Constructeur initialisant la semaine avec son numéro. */
    public Semaine(int numero) {
        this.numero = numero;
        this.seances = new ArrayList<>();  // Initialise une liste vide (pour accueillir les suivantes) pour les Séances.
    }

    /* Méthode pour ajouter une séance à la semaine courante. */
    public void ajouterSeance(Seance seance) { 
        this.seances.add(seance); 
    }

    /* Getteurs de la classe. */
    public int getNumero() { 
        return numero; 
    }

    public List<Seance> getSeances() { 
        return seances; 
    }

    /* Parcourt de la liste de séance pour calculer le volumle total. */
    public double getTotalDistancePrevue() {
        double totalKm = 0.0;
        for (Seance s : seances) totalKm += s.getDistancePrevueEnKm();
        return totalKm;
    }
}
