/* Cette classe gère la persistance des données sous forme de fichiers texte CSV (Comma-Separated Values). */
/* Contrairement à la sérialisation binaire, cela permet de rendre les sauvegardes lisibles et modifiables par l'humain (pour un projet futur par exemple avec de l'analyse de données). */

package donnees;

import modele.*;
import java.io.*; 
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.text.Normalizer; 

public class GestionnaireProfils {
    
    private static final String DOSSIER = "sauvegardes"; // Dossier de stockage.
    private static final String SEP = ";"; // Séparateur pour le CSV.

    /* Constructeur vérifiant l'existence du dossier de sauvegarde et le créant le cas échéant. */
    public GestionnaireProfils() {
        File dossier = new File(DOSSIER);
        if (!dossier.exists()) dossier.mkdirs();
    }

    /* On 'scanne' le dossier "sauvegardes" pour lister tous les fichiers .csv disponibles. */
    public List<String> listerNomsProfils() {
        List<String> noms = new ArrayList<>();
        File[] fichiers = new File(DOSSIER).listFiles((dir, name) -> name.endsWith(".csv"));
        if (fichiers != null) for (File f : fichiers) noms.add(f.getName().replace(".csv", ""));
        return noms;
    }

    /* Méthodes utilitaires pour vérifier l'existence d'un profil ou en supprimer un. */
    public boolean existe(String nomBase) { return new File(DOSSIER, nomBase + ".csv").exists(); }
    public void supprimer(String nomBase) { new File(DOSSIER, nomBase + ".csv").delete(); }

    /* Sauvegarde complète d'un utilisateur et de son plan dans un fichier .csv. */
    /* Le format est divisé en deux sections, le profil et le plan. */
    public void sauvegarder(Utilisateur u, PlanEntrainement p) throws IOException {
        if (u == null || u.getNom() == null) return;
        
        // Nettoyage du nom pour créer un nom de fichier valide (pas d'accents ni d'espaces).
        String baseName = nettoyerNom(u.getNom() + "_" + u.getPrenom());
        
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(DOSSIER, baseName + ".csv")), StandardCharsets.UTF_8))) {
            
            // On écrit les données de l'utilsateur.
            writer.write("SECTION_PROFIL"); writer.newLine();
            writer.write("Nom;Prenom;Ville;Age;Sexe;Taille;Poids;VMA;Volume;FCMax;FCRepos"); writer.newLine();
            
            // Utilisation de clean pour que les ";" ne casse pas les fichiers .csv.
            writer.write(clean(u.getNom()) + SEP + clean(u.getPrenom()) + SEP + (u.getVille()!=null?clean(u.getVille()):"") + SEP + u.getAge() + SEP + u.getSexe() + SEP + u.getTaille() + SEP + u.getPoids() + SEP + formatDouble(u.getVma()) + SEP + formatDouble(u.getVolumeHebdoSouhaite()) + SEP + u.getFcMax() + SEP + u.getFcRepos());
            writer.newLine();
            
            // On écrit le plan d'entraînement. 
            if (p != null) {
                writer.write("SECTION_PLAN"); writer.newLine();
                
                // Métadonnées utilisateur et plan (Nom, Date début, Objectif)
                writer.write("META" + SEP + clean(p.getNomDuPlan()) + SEP + p.getDateDebut() + SEP + (p.getObjectifChrono()!=null?clean(p.getObjectifChrono()):"null")); writer.newLine();
                
                // On écrit less descriptions pour les colonnes. 
                writer.write("Date;Type;Description;Intensite;Duree;Distance;Statut;Motif"); writer.newLine();
                
                // On parcourt toutes les semaines et on écrit les séances les unes après les autres.
                for (Semaine s : p.getSemaines()) for (Seance se : s.getSeances()) {
                    if (se.getType() == TypeSeance.REPOS) {
                        // Pour le repos, on écrit des valeurs vides ou nulles pour simplifier la lecture
                        writer.write(se.getDate() + SEP + se.getType() + SEP + "" + SEP + "" + SEP + "0" + SEP + "0.0" + SEP + "" + SEP + "");
                    } else {
                        writer.write(se.getDate() + SEP + se.getType() + SEP + clean(se.getDescription()) + SEP + clean(se.getIntensitePrevue()) + SEP + se.getDureePrevueEnMinutes() + SEP + formatDouble(se.getDistancePrevueEnKm()) + SEP + se.getStatut() + SEP + clean(se.getMotifAnnulation()));
                    }
                    writer.newLine();
                }
            }
        }
    }

    /* Charge un profil depuis le fichier CSV et reconstruit les objets Utilisateur et PlanEntrainement. */
    public DonneeSauvegardee charger(String baseName) throws IOException {
        File fichier = new File(DOSSIER, baseName + ".csv");
        Utilisateur u = new Utilisateur(); 
        PlanEntrainement p = null; 
        List<Seance> lst = new ArrayList<>(); // Liste temporaire pour stocker toutes les séances lues.
        
        // Ouverture sécurisée du fichier".
        // On empile les flux : Fichier brut (FileInputStream) -> Traduction UTF-8 pour les accents (InputStreamReader) -> Tampon de lecture (BufferedReader).
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fichier), StandardCharsets.UTF_8))) {
            
            String line, section = ""; // Variables pour stocker la ligne en cours et suivre la section active (PROFIL ou PLAN).
            
            // Lecture ligne par ligne (tant qu'il y en a).
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue; // Ignorer les lignes vides
                
                // Détection des balises de section pour changer le mode de parsing.
                if (line.startsWith("SECTION_PROFIL")) { section = "PROFIL"; reader.readLine(); continue; }
                else if (line.startsWith("SECTION_PLAN")) { section = "PLAN_META"; continue; }
                
                // Traitement selon la section active
                if (section.equals("PROFIL")) {
                    String[] parts = line.split(SEP); if(parts.length<11) continue;
                    // Reconstruction de l'utilisateur
                    u.setNom(parts[0]); u.setPrenom(parts[1]); u.setVille(parts[2]); u.setAge(parseInt(parts[3])); u.setSexe(Sexe.valueOf(parts[4])); u.setTaille(parseDouble(parts[5])); u.setPoids(parseDouble(parts[6])); u.setVma(parseDouble(parts[7])); u.setVolumeHebdoSouhaite(parseDouble(parts[8])); u.setFcMax(parseInt(parts[9])); u.setFcRepos(parseInt(parts[10]));
                    section = ""; // Fin de lecture profil
                } else if (section.equals("PLAN_META")) {
                    // Lecture des infos générales du plan
                    String[] meta = line.split(SEP);
                    p = new PlanEntrainement(meta[1], LocalDate.parse(meta[2]), meta[3].equals("null")?null:meta[3]);
                    reader.readLine(); // Sauter la ligne d'en-tête des colonnes
                    section = "PLAN_SEANCES";
                } else if (section.equals("PLAN_SEANCES")) {
                    // Parsing d'une ligne de séance
                    String[] parts = line.split(SEP); if(parts.length<2) continue;
                    Seance s = new Seance(LocalDate.parse(parts[0]), TypeSeance.valueOf(parts[1]), (parts.length>2?parts[2]:""), (parts.length>3?parts[3]:""), (parts.length>4?parseInt(parts[4]):0), (parts.length>5?parseDouble(parts[5]):0.0));
                    
                    // Récupération du statut (fait/annulé) et motif si présents
                    if(parts.length>6 && !parts[6].isEmpty() && !parts[6].equals("INFO")) try { s.setStatut(StatutSeance.valueOf(parts[6])); } catch(Exception e){}
                    if(parts.length>7) s.setMotifAnnulation(parts[7]);
                    
                    lst.add(s); // Ajout à la liste temporaire
                }
            }
        }
        
        // Reconstruction de la structure hiérarchique (Plan, semaines puis séances).
        if (p != null && !lst.isEmpty()) {
            Semaine currentSemaine = new Semaine(1);
            p.ajouterSemaine(currentSemaine);
            int numSem = 1;
            
            // On redistribue les séances lues dans des objets Semaine (par paquets de 7 jours logiques).
            for (int i = 0; i < lst.size(); i++) {
                if (currentSemaine.getSeances().size() == 7) {
                    numSem++;
                    currentSemaine = new Semaine(numSem);
                    p.ajouterSemaine(currentSemaine);
                }
                currentSemaine.ajouterSeance(lst.get(i));
            }
        }
        return new DonneeSauvegardee(u, p);
    }
    
    // On nettoie une chaîne pour le CSV , remplace les points-virgules par des virgules et supprime les sauts de ligne. */
    private String clean(String s) { if(s==null) return ""; return s.replace(";", ",").replace("\n", " ").trim(); }
    
    /* On formate un double avec un point décimal (Locale.US) pour éviter les erreurs de parsing CSV. */
    private String formatDouble(double d) { return String.format(Locale.US, "%.1f", d); }
    
    /* On normalise le nom de fichier : supprime les accents (NFD) et remplace les caractères spéciaux par des underscores. */
    private String nettoyerNom(String nom) { return Normalizer.normalize(nom, Normalizer.Form.NFD).replaceAll("[\\p{InCombiningDiacriticalMarks}]", "").replaceAll("[^a-zA-Z0-9_]", "_"); }
    
    /* Helpers pour parser les nombres de manière sécurisée (0 si erreur). */
    private double parseDouble(String s) { try { return Double.parseDouble(s.replace(",", ".")); } catch(Exception e){return 0.0;} }
    private int parseInt(String s) { try { return Integer.parseInt(s); } catch(Exception e){return 0;} }
    
    /* Classe interne simple (DTO) pour renvoyer à la fois l'utilisateur et son plan après chargement des données. */
    public static class DonneeSauvegardee { 
        public Utilisateur utilisateur; 
        public PlanEntrainement plan; 
        public DonneeSauvegardee(Utilisateur u, PlanEntrainement p) { this.utilisateur = u; this.plan = p; } 
    }
}
