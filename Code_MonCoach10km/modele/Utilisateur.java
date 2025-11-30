/* On regroupe ici les données personnelles et physiologiques de l'utilisateur. */

package modele;

import java.io.Serializable;

/* Sérialisation nécessaire pour garantir la persistance et le rechargement des données de nos objets (utilisateurs). */
public class Utilisateur implements Serializable {
    
    private static final long serialVersionUID = 3L; // Identifiant pour la sérialisation.
    
    // Données d'Identité.
    private String nom;
    private String prenom;
    private String ville;
    private int age;
    private Sexe sexe; 
    
    // Données Physiologiques.
    private double taille;
    private double poids;
    private double vma; 
    private int fcMax;  
    private int fcRepos; 
    private double volumeHebdoSouhaite;
    
    /* Constructeur par défaut. 
     * Permet de créer une instance vide en attente de la saisie des informations. */
    public Utilisateur() {}

    /* Getteurs et Setteurs pour l'identité de l'utilisateur. */

    public String getNom() { 
        return nom; } 
    public void setNom(String nom) { 
        this.nom = nom; }

    public String getPrenom() { 
        return prenom; } 
    public void setPrenom(String prenom) { 
        this.prenom = prenom; }

    public String getVille() { 
        return ville; } 
    public void setVille(String ville) { 
        this.ville = ville; }

    /* Reformulation complète du nom. */
    public String getNomComplet() { 
        return (prenom != null ? prenom : "") + " " + (nom != null ? nom : ""); 
    }

    public int getAge() { 
        return age; } 
    public void setAge(int age) { 
        this.age = age; }

    public Sexe getSexe() { 
        return sexe; } 
    public void setSexe(Sexe sexe) { 
        this.sexe = sexe; }

    /* Getteurs et Setteurs physiologiques de la classe. */
    public double getTaille() { 
        return taille; } 
    public void setTaille(double taille) { 
        this.taille = taille; }

    public double getPoids() { 
        return poids; } 
    public void setPoids(double poids) { 
        this.poids = poids; }

    public int getFcMax() { 
        return fcMax; } 
    public void setFcMax(int fcMax) { 
        this.fcMax = fcMax; }

    public int getFcRepos() { 
        return fcRepos; } 
    public void setFcRepos(int fcRepos) { 
        this.fcRepos = fcRepos; }

    public double getVma() { 
        return vma; } 
        
    /* En dessous de 8km/h ou au delà de 20 km/h, on considère que l'utilisateur nécessite un coaching particulier. */
    /* La méthode inclut une validation, ainsi on évite les valeurs aberrantes. */
    public void setVma(double vma) { 
        if (vma < 8.0 || vma > 20.0) {
            throw new IllegalArgumentException("La VMA doit être comprise entre 8 et 20 km/h.");
        }
        this.vma = vma; 
    }

    public double getVolumeHebdoSouhaite() { 
        return volumeHebdoSouhaite; } 
    public void setVolumeHebdoSouhaite(double v) { 
        this.volumeHebdoSouhaite = v; }
}
