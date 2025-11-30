package metier;

import modele.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GenerateurPlan {
    private enum TypePhase { DEBUT, MILIEU, AFFUTAGE } // Enumération des différentes phases
    /* Règle définissant le nombre de séance qu'aura l'utilisateur selon son volume hedbomadaire */
    public static int calculerNbSeancesMin(double volumeHebdo) {
        if (volumeHebdo < 30) return 3; 
        if (volumeHebdo < 40) return 4; 
        if (volumeHebdo < 50) return 5; 
        return 6;
    }

    /*  Gestion de la génération d'un plan entraînement pour les différents modes
    * On défini arbitrairement l'objectif chrono à 80% de vma pour le mode finir
    * Pour le mode perf on vérifie que l'objectif est compris et entre 80% et inférieur à 93% de vma qui sont des valeurs réalistes pour le coureur
    *    
    */
    public PlanEntrainement generer(Utilisateur utilisateur, ParametresPlan parametres) throws ValidationException {
        
        LocalDate dateDebut = parametres.getDateDeDebut();
        String objectifFinal = parametres.getObjectifChrono();
        
        // Mode Finir
        if (!parametres.hasObjectifChrono()) {
            double vit = utilisateur.getVma() * 0.80;
            double hrs = 10.0 / vit;
            objectifFinal = String.format("%d:%02d:%02d", (int)hrs, (int)((hrs-(int)hrs)*60), (int)((((hrs-(int)hrs)*60)-(int)((hrs-(int)hrs)*60))*60)) + " (Obj. Finir)";
        }
        
        double objMin = CalculPhysiologique.parseObjectifEnMinutes(objectifFinal.replace(" (Obj. Finir)", ""));
        
        // Mode Perf
        if (parametres.hasObjectifChrono()) {
            double vitObj = CalculPhysiologique.parseObjectifEnVitesseKmH(objectifFinal, objMin);
            double pVMA = CalculPhysiologique.getPourcentageVMA(vitObj, utilisateur.getVma());

            if (pVMA > 0.93) throw new ValidationException("Objectif irréaliste (> 93% VMA). Visez moins haut !");
            if (pVMA <= 0.80) throw new ValidationException("Pour un mode 'Perf', l'allure doit être > 80% VMA. Sinon choisissez le mode 'Finir'.");
        }
        // On prend le volume initial souhaité par l'utilisateur et on applique une modification dans le cas où ne respecte pas le volume minimal défini
        // 25 km pour le mode perf et 15 pour le mode finir
        double volumeInitial = utilisateur.getVolumeHebdoSouhaite();
        double volMin = parametres.hasObjectifChrono() ? 25.0 : 15.0;
        if (volumeInitial < volMin) volumeInitial = volMin;
        int nbSeances = calculerNbSeancesMin(volumeInitial);
        double volPlafond = volumeInitial * Math.pow(1.1, 4); // volume plafond défini par volume initial * 1.1**4 pour augmenter la charge progressivement puis se maintenir
        PlanEntrainement plan = new PlanEntrainement("Plan 10km - " + utilisateur.getNomComplet(), dateDebut, objectifFinal);
        double volPrec = volumeInitial;
        int tot = parametres.getDureeEnSemaines();
        int idxAff = tot - 2;  // Indice pour débuter l'affûtage
        int idxCrse = tot - 1; // Indice pour indiquer la semaine de course

        for (int i = 0; i < tot; i++) {
            Semaine sem = new Semaine(i + 1); 
            LocalDate d = dateDebut.plusWeeks(i);
            
            if (i == idxCrse) {
                genererSemaineCourse(sem, utilisateur, i, d, objectifFinal, nbSeances, volumeInitial, parametres.hasObjectifChrono());
            } else {
                double volCiblePourSemaine; // Définition du volume cible pour la semaine
                if (i == 0 || i == idxAff) { // Le  volume initial est repris pour l'affûtage
                    volCiblePourSemaine = volumeInitial; 
                } else {
                    volCiblePourSemaine = Math.min(volPrec * 1.10, volPlafond); // Dans les autres cas soit on augmente de 10% le volume sauf en cas d'atteinte du plafond
                }
                // Définition des différentes phases  2 premières semaines Début , puis Milieu jusqu'à l'affûtage
                TypePhase phase = (i < 2) ? TypePhase.DEBUT : (i == idxAff ? TypePhase.AFFUTAGE : TypePhase.MILIEU);
                genererSemaineStd(sem, utilisateur, i, nbSeances, d, volCiblePourSemaine, phase, parametres.hasObjectifChrono());
                volPrec = volCiblePourSemaine; 
            }
            plan.ajouterSemaine(sem);
        }
        return plan;
    }
    /* Génére une semaine standard ( hors course car nous faisons un cas spécial pour la semaine de course)    */
    private void genererSemaineStd(Semaine sem, Utilisateur u, int n, int nb, LocalDate d, double vol, TypePhase phase, boolean modePerf) {
        // Liste des 7 jours initialisés à null
        List<Seance> slots = new ArrayList<>(Collections.nCopies(7, null));
        // Creation des différents types de séances
        Seance sV = SeanceBibliotheque.creerSeanceVMA(d, u, n);
        Seance sS = SeanceBibliotheque.creerSeanceSeuil(d, u, n);
        Seance sA = SeanceBibliotheque.creerSeanceAS10(d, u, n);

        // Liste qui va contenir la répartition des séances que nous avons définie en fonction du nombre de séances et du mode
        List<Integer> types = new ArrayList<>();
        
        /* Pour le mode perf On a les 4 cas suivants:
        * Pour 3 séances: 
        Phase début: EF Seuil Vma
        Phase milieu: EF AS10 vma
        Phase affutage: EF AS10 Seuil

        * Pour 4 séances:
        Phase début: Ef seuil ef vma
        Phase milieu: Ef as10 ef vma
        Phase affutage: Ef as10 ef seuil

        * Pour 5 séances: 
        Phase début: Ef seuil ef vma ef
        Phase milieu: Ef AS10 ef vma ef
        Phase affutage: EF AS10 ef seuil ef

        * Pour 6 séances:
        Phase début: Ef seuil ef vma ef seuil
        Phase milieu: Ef AS10 ef vma ef seuil
        Phase affutage: EF AS10 ef seuil ef ef
        */
        if (modePerf) {
            if(nb==3) { 
                if(phase==TypePhase.DEBUT) Collections.addAll(types, 0, 2, 1);
                else if(phase==TypePhase.MILIEU) Collections.addAll(types, 0, 3, 1); 
                else Collections.addAll(types, 0, 3, 2); 
            }
            else if(nb==4) { 
                if(phase==TypePhase.DEBUT) Collections.addAll(types, 0, 2, 0, 1); 
                else if(phase==TypePhase.MILIEU) Collections.addAll(types, 0, 3, 0, 1); 
                else Collections.addAll(types, 0, 3, 0, 2); 
            }
            else if(nb==5) { 
                if(phase==TypePhase.DEBUT) Collections.addAll(types, 0, 2, 0, 1, 0); 
                else if(phase==TypePhase.MILIEU) Collections.addAll(types, 0, 3, 0, 1, 0); 
                else Collections.addAll(types, 0, 3, 0, 2, 0); 
            }
            else { 
                if(phase==TypePhase.DEBUT) Collections.addAll(types, 0, 2, 0, 1, 0, 2); 
                else if(phase==TypePhase.MILIEU) Collections.addAll(types, 0, 3, 0, 1, 0, 2); 
                else Collections.addAll(types, 0, 3, 0, 2, 0, 0); 
            }
        } else {
            /* Pour le mode Finir On a les 4 cas suivants:
            * Pour 3 séances:
            Phase début: EF Seuil EF
            Phase milieu: EF AS10 EF
            Phase affutage: EF Seuil EF

            * Pour 4 séances:
            Phase début: EF EF Seuil EF
            Phase milieu: EF EF AS10 EF
            Phase affutage: EF EF Seuil EF

            * Pour 5 séances:
            Phase début: EF Seuil EF Seuil EF
            Phase milieu: EF AS10 EF Seuil EF
            Phase affutage: EF EF EF AS10 EF

            * Pour 6 séances:
            Phase début: EF EF Seuil EF EF Seuil
            Phase milieu: EF EF AS10 EF EF Seuil
            Phase affutage: EF EF Seuil EF EF AS10
            */
            if(nb==3) {
                if(phase==TypePhase.DEBUT) Collections.addAll(types, 0, 2, 0);    
                else if(phase==TypePhase.MILIEU) Collections.addAll(types, 0, 3, 0); 
                else Collections.addAll(types, 0, 2, 0);                                    
            }
            else if(nb==4) {
                if(phase==TypePhase.DEBUT) Collections.addAll(types, 0, 0, 2, 0);    
                else if(phase==TypePhase.MILIEU) Collections.addAll(types, 0, 0, 3, 0); 
                else Collections.addAll(types, 0, 0, 2, 0);                                    
            }
            else if(nb==5) {
                if(phase==TypePhase.DEBUT) Collections.addAll(types, 0, 2, 0, 2, 0);    
                else if(phase==TypePhase.MILIEU) Collections.addAll(types, 0, 3, 0, 2, 0); 
                else Collections.addAll(types, 0, 0, 0, 3, 0);                                    
            }
            else { 
                if(phase==TypePhase.DEBUT) Collections.addAll(types, 0, 0, 2, 0, 0, 2);    
                else if(phase==TypePhase.MILIEU) Collections.addAll(types, 0, 0, 3, 0, 0, 2); 
                else Collections.addAll(types, 0, 0, 2, 0, 0, 3);                                    
            }
        }

        double vQ = 0; // Compteur pour le volume de qualité réalisé (AS10, seui, vma)
        int nE = 0; // Compteur pour le nombre de séances d'EF prévues
        // On parcourt la liste et on gère les cas soit qualité soit ef pour que les compteurs aient la bonne valeur
        for(int t : types) { 
            if(t==1) vQ+=sV.getDistancePrevueEnKm(); 
            else if(t==2) vQ+=sS.getDistancePrevueEnKm(); 
            else if(t==3) vQ+=sA.getDistancePrevueEnKm(); 
            else nE++; 
        }
        // Une séance d'ef fait au moins 3km et on répartit de manière uniforme le volume restant sur chaque séance d'ef
        double distEF = (nE > 0) ? Math.max(3.0, (vol - vQ) / nE) : 3.0;
        
        // Répartition des séances sur les jours de la semaine(de 0 à 6) selon les différents cas 
        int[] jours = (nb==3)? new int[]{1,3,5} 
                    : (nb==4)? new int[]{0,2,4,6}
                    : (nb==5)? new int[]{0,1,3,4,6} 
                    : new int[]{0,1,2,4,5,6};
        // On boucle sur les séances prévue et on les place dans le calendrier
        for(int i=0; i<types.size(); i++) {
            int t = types.get(i); // Le type de la séance (0,1,2,3)
            int jr = jours[i];    // Le jour de la semaine
            
            //on vérifie que  le jour est valide (0 à 6)
            if(jr < 7) {
                Seance s = null;
                if(t==1) s = sV;      // On reprend la séance VMA pré-calculée
                else if(t==2) s = sS; // On reprend la séance Seuil pré-calculée
                else if(t==3) s = sA; // On reprend la séance AS10 pré-calculée
                // Pour l'EF, on créer la séance avec le volume déterminé précedemment
                else s = SeanceBibliotheque.creerSeanceEF(d.plusDays(jr), u, distEF);
                
                // On donne la date précise (Lundi de la semaine d + jr jours)
                s.setDate(d.plusDays(jr)); 
                
                // On insère la séance dans le slot correspondant au jour
                slots.set(jr, s);
            }
        }
        // On parcourt les 7 jours de la semaine pour vérifier s'ils sont occupés ce qui ne sera pas le cas car on a au max 6 séances puis on met les jours de repos dans le reste
        for(int i=0; i<7; i++) {
            // Si un jour n'a pas reçu de séance,c'est un jour de REPOS
            if (slots.get(i) == null) {
                // On crée une séance de type REPOS pour l'affichage dans le calendrier
                slots.set(i, new Seance(d.plusDays(i), TypeSeance.REPOS, "Repos", "", 0, 0.0));
            }
            // On ajoute la séance à l'objet Semaine
            sem.ajouterSeance(slots.get(i));
        }
    }

    // Traitement du cas de la semaine de course qui possède une logique différente on préfère la séparer pour que ce soit plus fluide.
    private void genererSemaineCourse(Semaine sem, Utilisateur u, int n, LocalDate d, String objChrono, int nbSeances, double volInitial, boolean modePerf) {
        // Création de la liste vide
        List<Seance> slots = new ArrayList<>(Collections.nCopies(7, null));
        // Création de la séance du jour de course
        Seance s_course = SeanceBibliotheque.creerSeanceCourse(d.plusDays(6), u, objChrono);
        // Création de la séance de veille de course
        Seance s_veille = SeanceBibliotheque.creerSeanceVeilleDeCourse(d.plusDays(5), u);
        slots.set(6, s_course); 
        slots.set(5, s_veille);
        //  En mode perf on ajoute une séance de rappel d'allure en milieu de semaine.
        Seance s_j3_intensite = null;
        if (modePerf) {
            s_j3_intensite = SeanceBibliotheque.creerSeanceAS10(d.plusDays(3), u, n);
            slots.set(3, s_j3_intensite);
        }
        // Calcul du volume déjà pris (avec ou sans l'intensité du mercredi)
        double dejaPris = s_course.getDistancePrevueEnKm() + s_veille.getDistancePrevueEnKm();
        if (s_j3_intensite != null) {
            dejaPris += s_j3_intensite.getDistancePrevueEnKm();
        }
        // Volume restant pour ensuite calcul le volume des ef de la dernière semaine
        double reste = (volInitial - dejaPris);
        if(modePerf) {
            // Mode Performance : 3 séances fixes (Course, Veille, Intensité J-3)
            int nbEF = nbSeances - 3; 
            double distEF = (nbEF > 0) ? Math.max(0, reste / nbEF) : 0;
            // Placement des EF (évite J-3 qui a l'intensité)
            if (nbEF >= 1) { slots.set(1, SeanceBibliotheque.creerSeanceEF(d.plusDays(1), u, distEF)); } // Lundi
            if (nbEF >= 2) { slots.set(4, SeanceBibliotheque.creerSeanceEF(d.plusDays(4), u, distEF)); } // Jeudi
            if (nbEF >= 3) { slots.set(2, SeanceBibliotheque.creerSeanceEF(d.plusDays(2), u, distEF)); } // Mardi
            if (nbEF >= 4) { slots.set(0, SeanceBibliotheque.creerSeanceEF(d.plusDays(0), u, distEF)); } // Dimanche
            
        } else {
            // Mode Finir : Seulement 2 séances fixes (Course, Veille) puis on ajoute comme précedemment les séances restantes dans la dernière semaine
            int nbAutre = nbSeances - 2; 
            double distEF = (nbAutre > 0) ? Math.max(3.0, reste / nbAutre) : 3.0;
            
            if (nbSeances == 3) { slots.set(2, SeanceBibliotheque.creerSeanceEF(d.plusDays(2), u, distEF)); }
            else if (nbSeances == 4) { 
                slots.set(1, SeanceBibliotheque.creerSeanceEF(d.plusDays(1), u, distEF)); 
                slots.set(3, SeanceBibliotheque.creerSeanceEF(d.plusDays(3), u, distEF));
            }
            else if (nbSeances == 5) { 
                slots.set(0, SeanceBibliotheque.creerSeanceEF(d.plusDays(0), u, distEF)); 
                slots.set(1, SeanceBibliotheque.creerSeanceSeuil(d.plusDays(1), u, 1));
                slots.set(3, SeanceBibliotheque.creerSeanceEF(d.plusDays(3), u, distEF));
            }
            else { 
                slots.set(0, SeanceBibliotheque.creerSeanceEF(d.plusDays(0), u, distEF)); 
                slots.set(1, SeanceBibliotheque.creerSeanceEF(d.plusDays(1), u, distEF)); 
                slots.set(2, SeanceBibliotheque.creerSeanceSeuil(d.plusDays(2), u, 1));
                slots.set(3, SeanceBibliotheque.creerSeanceEF(d.plusDays(3), u, distEF));
            }
        }

        //les jours restants sont remplis par du repos et ajouter à la semaine
        for (int i=0; i<7; i++) {
            if (slots.get(i) == null) slots.set(i, new Seance(d.plusDays(i), TypeSeance.REPOS, "Repos", "", 0, 0.0));
            sem.ajouterSeance(slots.get(i));
        }
    }
}
