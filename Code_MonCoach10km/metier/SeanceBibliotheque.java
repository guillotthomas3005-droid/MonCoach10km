/* Cette classe agit comme une "bibliothèque". */
/* On centralise la logique de création de toutes les séances types en s'adaptant aux données physiologiques du coureur. */

package metier;

import modele.*;
import java.time.LocalDate;

public class SeanceBibliotheque {

    /* Ces méthodes servent à éviter la répétition de code pour les calculs d'échauffement et d'allure EF. */

    /* Calcule l'allure en secondes/km à 65% de la VMA (Endurance Fondamentale). */
    private static double getAllureMoyenneEF(Utilisateur u) { 
        return CalculPhysiologique.convertirKmHEnAllureSec(u.getVma() * 0.65); }

    /* On fixe la durée d'échauffement à 20 minutes et on fournie la distance à parcourir durant ces 20 minutes. */
    private static double getDistEchauf(Utilisateur u) { 
        return (20.0 * 60.0) / getAllureMoyenneEF(u); }
    private static int getDureeEchauf() { 
        return 20; }

    /* Création d'une séance d'Endurance Fondamentale (EF). */
    public static Seance creerSeanceEF(LocalDate date, Utilisateur u, double distKm) {
        if (distKm < 3.0) distKm = 3.0; // On impose 3km par sortie (minimum).
        
        int duree = (int) ((distKm * getAllureMoyenneEF(u)) / 60.0);
        
        // Construction de la description avec les zones cardiaques (65-75% FCM).
        String desc = duree + " min EF (" + String.format("%.1f", distKm) + " km) - " + CalculPhysiologique.getZoneCardiaque(u, 0.65, 0.75);
        
        return new Seance(date, TypeSeance.ENDURANCE_FONDAMENTALE, desc, CalculPhysiologique.getAllureEF(u.getVma()), duree, distKm);
    }
    
    /* Création d'une séance de VMA (Vitesse Maximale Aérobie). */
    public static Seance creerSeanceVMA(LocalDate date, Utilisateur u, int numSem) {
        String desc; int duree; double dist; 
        double all = CalculPhysiologique.convertirKmHEnAllureSec(u.getVma()); // 100% VMA.
        
        // Structure d'entraînement classique : Echauffement, Corps de séance, Récupération.
        String prefix = "10 min échauffement allure EF + ";
        String suffix = " + 10 min récup allure EF";

        // modulo 5 (%) permet de sélectionner une séance différente selon le numéro de la semaine.
        switch (numSem % 5) {
            case 0: 
                desc = prefix + "10x400m (r: 1'15)" + suffix; 
                dist = 4.0; // Distance du corps de séance (hors échauf/récup).
                duree = (int)(10*(0.4*all/60.0)+9*1.25+getDureeEchauf()); 
                break;
            case 1: 
                desc = prefix + "8x500m (r: 1'30)" + suffix; 
                dist = 4.0; 
                duree = (int)(8*(0.5*all/60.0)+7*1.5+getDureeEchauf()); 
                break;
            case 2: 
                desc = prefix + "20x (30s/30s)" + suffix; 
                dist = 20*(30.0/all); // Distance approximative pour du 30/30.
                duree = 20+getDureeEchauf(); 
                break;
            case 3: 
                desc = prefix + "12x300m (r: 1')" + suffix; 
                dist = 3.6; 
                duree = (int)(12*(0.3*all/60.0)+11*1.0+getDureeEchauf()); 
                break;
            default: 
                desc = prefix + "15x (45s/45s)" + suffix; 
                dist = 15*(45.0/all); 
                duree = (int)(15*1.5+getDureeEchauf()); 
                break;
        }
        return new Seance(date, TypeSeance.VMA, desc, CalculPhysiologique.getAllureVMA(u.getVma()), duree, getDistEchauf(u)+dist);
    }
    
    /* Création d'une séance au Seuil (Anaérobie). */
    /* Même logique de rotation que la VMA, mais à 85% de la VMA. */
    public static Seance creerSeanceSeuil(LocalDate date, Utilisateur u, int numSem) {
        String desc; int duree; double dist; 
        double all = CalculPhysiologique.convertirKmHEnAllureSec(u.getVma()*0.85); // 85% VMA
        
        String prefix = "10 min échauffement allure EF + ";
        String suffix = " + 10 min récup allure EF";

        switch (numSem % 5) {
            case 0: 
                desc = prefix + "3x8 min (r: 2')" + suffix; 
                duree = 28+getDureeEchauf(); 
                dist = (24*60.0)/all; // Distance calculée selon le temps d'effort.
                break;
            case 1: 
                desc = prefix + "2x12 min (r: 3')" + suffix; 
                duree = 27+getDureeEchauf(); 
                dist = (24*60.0)/all; 
                break;
            case 2: 
                desc = prefix + "6x4 min (r: 1')" + suffix; 
                duree = 29+getDureeEchauf(); 
                dist = (24*60.0)/all; 
                break;
            case 3: 
                desc = prefix + "15min+5min (r: 1')" + suffix; 
                duree = 21+getDureeEchauf(); 
                dist = (20*60.0)/all; 
                break;
            default: 
                desc = prefix + "10+8+6 min (r: 2')" + suffix; 
                duree = 28+getDureeEchauf(); 
                dist = (24*60.0)/all; 
                break;
        }
        return new Seance(date, TypeSeance.SEUIL, desc, CalculPhysiologique.getAllureSeuil(u.getVma()), duree, getDistEchauf(u)+dist);
    }
    
    /* Création d'une séance à Allure Spécifique 10km (AS10). */
    /* Rotation sur 5 semaines, calculée à 90% de la VMA. */
    public static Seance creerSeanceAS10(LocalDate date, Utilisateur u, int numSem) {
        String desc; int duree; double dist; 
        double all = CalculPhysiologique.convertirKmHEnAllureSec(u.getVma()*0.90); // 90% VMA
        
        String prefix = "10 min échauffement allure EF + ";
        String suffix = " + 10 min récup allure EF";

        switch (numSem % 5) {
            case 0: 
                desc = prefix + "3x2000m (r: 2'30)" + suffix; 
                dist = 6.0; 
                duree = (int)(3*(2*all/60.0)+5+getDureeEchauf()); 
                break;
            case 1: 
                desc = prefix + "2x3000m (r: 3')" + suffix; 
                dist = 6.0; 
                duree = (int)(2*(3*all/60.0)+3+getDureeEchauf()); 
                break;
            case 2: 
                desc = prefix + "6000m Continu" + suffix; 
                dist = 6.0; 
                duree = (int)((6*all/60.0)+getDureeEchauf()); 
                break;
            case 3: 
                desc = prefix + "4x2000m (r: 2'30)" + suffix; 
                dist = 8.0; 
                duree = (int)(4*(2*all/60.0)+7.5+getDureeEchauf()); 
                break;
            default: 
                desc = prefix + "6x1000m (r: 1')" + suffix; 
                dist = 6.0; 
                duree = (int)(6*(1*all/60.0)+5+getDureeEchauf()); 
                break;
        }
        return new Seance(date, TypeSeance.ALLURE_SPECIFIQUE, desc, CalculPhysiologique.getAllureAS10(u.getVma()), duree, getDistEchauf(u)+dist);
    }
    
    /* Création d'une séance légère de "veille de course" (activation). */ 
    public static Seance creerSeanceVeilleDeCourse(LocalDate date, Utilisateur u) {
        return new Seance(date, TypeSeance.VEILLE_DE_COURSE, "15 min EF + Lignes Droites", CalculPhysiologique.getAllureEF(u.getVma()), 20, (20*60.0)/getAllureMoyenneEF(u));
    }
    
    /* Création de la séance finale : Le jour de la course. */
    /* Analyse l'objectif (ex: "50 min") pour définir l'allure cible précise. */ 
    public static Seance creerSeanceCourse(LocalDate date, Utilisateur u, String chrono) {
        // On nettoie la chaîne de caractères pour extraire le temps visé en minutes.
        double objMin = CalculPhysiologique.parseObjectifEnMinutes(chrono.replace(" (Obj. Finir)", ""));
        
        // Calcul de l'allure cible en min/km pour atteindre l'objectif sur 10km.
        double allureSecKm = (objMin * 60.0) / 10.0;
        String allureCible = String.format("%d:%02d min/km", (int)allureSecKm/60, (int)allureSecKm%60);
        
        int duree = (int) objMin;
        return new Seance(date, TypeSeance.COURSE_10KM, "JOUR J : Course 10km !", "Obj: " + allureCible, duree, 10.0);
    }
}
