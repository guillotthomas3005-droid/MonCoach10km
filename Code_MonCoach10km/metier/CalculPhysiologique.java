package metier;

import modele.Utilisateur;

public class CalculPhysiologique {

    /* Calcule de l'IMC avec la formule classique (kg/taille_en_cm**2) 
     * avec gestion des cas critiques définis arbitrairement dans le but de limiter les risques de la pratique sportive   */
    public static double calculerIMC(double poidsKg, double tailleCm) throws ValidationException {
        if (poidsKg <= 0 || tailleCm <= 0) throw new ValidationException("Les valeurs doivent être positives.");

        if (tailleCm < 120 || tailleCm > 240) {
            throw new ValidationException("Taille hors limites (" + (int)tailleCm + " cm). Doit être comprise entre 120 et 240 cm.");
        }
        if (poidsKg < 40 || poidsKg > 150) {
            throw new ValidationException("Poids hors limites (" + poidsKg + " kg). Doit être compris entre 40 et 150 kg.");
        }

        double tailleM = tailleCm / 100.0;
        double imc = poidsKg / (tailleM * tailleM);

        if (imc < 12 || imc > 38) {
            throw new ValidationException(String.format("IMC calculé (%.1f) hors normes (12-38). Vérifiez la cohérence taille/poids.", imc));
        }
        return imc;
    }

    /* Gestion des valeurs aberrantes de fc nous avons déterminé que la fcrepos devait être d'au moins 20 et la max ne devait pas dépasser 225
    * De même on a définit que la fcmax > fcrecpos + 60 et que le max devait être d'au moins 100
    */
    public static void validerDonneesCardiaques(int fcRepos, int fcMax) throws ValidationException {
        if (fcRepos <= 20) {
            throw new ValidationException("FC Repos trop basse (" + fcRepos + "). Elle doit être supérieure à 20 bpm.");
        }
        if (fcMax > 225) {
            throw new ValidationException("FC Max trop élevée (" + fcMax + "). Elle doit être inférieure ou égale à 225 bpm.");
        }
        if (fcMax < 100) {
            throw new ValidationException("FC Max trop basse (" + fcMax + "). Le minimum est de 100 bpm.");
        }
        if (fcMax < fcRepos + 60) {
            throw new ValidationException("L'écart entre FC Repos (" + fcRepos + ") et FC Max (" + fcMax + ") est insuffisant. Il faut au moins 60 bpm d'écart.");
        }
    }

    /* Permet de généraliser l'obtention d'une zone cardiaque à partir des paramètres de l'utilisateur
    * Utile pour une approche où l'on veut s'entrainer avec comme métrique principale la fc ce qui est 
    * est le cas pour l'ef
    */
    public static String getZoneCardiaque(Utilisateur u, double pMin, double pMax) {
        if (u.getFcMax() <= 0) return "FC Inv.";
        
        int bpmMin = (int) (u.getFcMax() * pMin);
        int bpmMax = (int) (u.getFcMax() * pMax);
        
        return (int)(pMin*100) + "-" + (int)(pMax*100) + "% FCMax : " + bpmMin + "-" + bpmMax + " bpm";
    }

    /* Définition du pourcentage de vma */
    public static double getPourcentageVMA(double v, double vma) throws ValidationException { 
        if (vma < 8.0 || vma > 20.0) {
            throw new ValidationException("VMA invalide (" + vma + " km/h). Elle doit être comprise entre 8 et 20 km/h."); 
        }
        return v / vma; 
    }

    /* Utilisation d'une formule standard pour déterminer la vo2max nous n'avons pas les moyens ici d'appliquer des formules plus complexes   */
    public static double estimerVO2max(double vma) { 
        return vma * 3.5; 
    }

    /* Permet de convertir l'allure en sec utile pour l'affichage min/km ou en km/h moins utilisé ici mais ça
    * dépend de la préférence de chacun 
    */
    public static double convertirKmHEnAllureSec(double kmh) { 
        return (kmh <= 0) ? 0 : 3600.0 / kmh; 
    }
    
    public static double convertirAllureEnKmH(double sec) { 
        return (sec <= 0) ? 0 : 3600.0 / sec; 
    }

    /* Conversion respectivement du temps objectif de l'utilisateur en min et en km/h */
    public static double parseObjectifEnMinutes(String o) {
        if (o.contains("Finir")) return 60.0;
        String[] p = o.split(":"); 
        try { 
            if (p.length == 2) return Double.parseDouble(p[0]) + (Double.parseDouble(p[1])/60.0);
            else if (p.length == 3) return (Double.parseDouble(p[0])*60) + Double.parseDouble(p[1]) + (Double.parseDouble(p[2])/60.0);
        } catch (Exception e) {} 
        return 0;
    }

    public static double parseObjectifEnVitesseKmH(String o, double m) {
        return (m <= 0) ? 0 : 10.0 / (m/60.0); 
    }

    /* Permet de généraliser l'obtention des zones d'allures à partir de la vma et des valeurs de % types de conversion pour chaque effort spécifique*/
    public static String getAllurePlage(double vma, double pMin, double pMax) {
        double s1 = convertirKmHEnAllureSec(vma*pMax), s2 = convertirKmHEnAllureSec(vma*pMin);
        if(s2-s1<5) s2=s1+5; 
        return String.format("%d:%02d - %d:%02d min/km", (int)s1/60, (int)s1%60, (int)s2/60, (int)s2%60);
    }
    
    /* Application de la méthode précédente pour respectivement l'allure ef , seuil , as10, vma, 
    et RecupMarche (défini arbitrairement pour gérer les temps de repos dans les séances d'intensités) pour les autres pourcentages nous nous sommes inspirés
    des valeurs classiques  
    */
    public static String getAllureEF(double vma) {
        return getAllurePlage(vma, 0.60, 0.70); 
    }

    public static String getAllureSeuil(double vma) {
        return getAllurePlage(vma, 0.83, 0.88); 
    }

    public static String getAllureAS10(double vma) {
        return getAllurePlage(vma, 0.88, 0.92); 
    }

    public static String getAllureVMA(double vma) {
        return getAllurePlage(vma, 1.00, 1.05); 
    }

    public static String getAllureRecupMarche() {
        return getAllurePlage(10, 0.4, 0.6); 
    }
}