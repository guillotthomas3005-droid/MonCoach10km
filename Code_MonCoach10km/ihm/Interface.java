package ihm;

import modele.*;
import metier.*;
import donnees.GestionnaireProfils;
import api.ServiceMeteo;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.geom.Arc2D; 
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.text.Normalizer;


public class Interface extends JFrame {
    // Dépendance et services
    // Objets pour gérer la logique
    private GestionnaireProfils managerProfils; 
    private GenerateurPlan generateur;
    private ServiceMeteo serviceMeteo; 
    
    // Etat de l'application
    private Utilisateur utilisateur; // L'utilisateur courant
    private PlanEntrainement planActuel; // Le plan généré ou chargé
    private Map<LocalDate, String> cacheMeteo = new HashMap<>();  // Pour éviter de rappeler l'API à chaque rafraichissement

    // Variables pour le calcul du ratio de séances effectués
    private long seancesTotal = 0;
    private long seancesFaites = 0;

    // -Composants graphiques
    private CardLayout cardLayout; // Permet de changer d'écran comme des cartes
    private JPanel mainContainer; // Le panneau qui contient toutes les cartes
    // Panneaux de structure du Dashboard ( Barre de nar et contenu)
    private JPanel navBar; 
    private JPanel contentPanel; 
    
    // Différentes Vues de l'application
    // On les garde en attributs pour pouvoir les rafraichir dynamiquement
    private JPanel viewLanding;      // Page d'accueil (Choix profil)
    private JPanel viewAccueil;      // Onglet "Accueil" du Dashboard (Jauge)
    private JPanel viewCalendrier;   // Onglet "Calendrier" (Grille de boutons)
    private JPanel viewProfil;       // Onglet "Mon Profil" (Formulaire édition)
    private JPanel viewStats;        // Onglet "Statistiques" (Graphiques)
    // --- Composants du Formulaire de Création (Wizard) ---
    // Regroupement logique des boutons radio pour exclusion mutuelle
    private ButtonGroup groupSexe, groupObjectif;
    private JRadioButton radioHomme, radioFemme, radioFinir, radioPerf;
    
    // Champs de saisie texte
    private JTextField txtNom, txtPrenom, txtVille, txtAge, txtTaille, txtPoids;
    private JTextField txtFcMax, txtFcRepos, txtVma, txtVolume;
    private JTextField txtDateDebut; 
    
    // Sélection de la durée (Integer pour éviter le parsing inutile)
    private JComboBox<Integer> comboDuree; 
    
    // Champs spécifiques au mode Performance
    private JTextField txtChrono;
    private JLabel lblNbSeancesAuto; // Label dynamique calculé selon le volume hebdo
    
    // Calendrier & Édition
    private JTabbedPane tabbedPanePlan;  // Onglets par semaine (S1, S2, etc.)
    private JTextArea detailsTextArea;   // Zone de texte pour le détail de la séance cliquée
    private JPanel panelActionButtons;   // Conteneur des boutons "Valider" / "Annuler"
    // Bouton Toggle pour activer le mode "Drag & Drop" (Swap)
    private JToggleButton btnModeEdition;
    // État du mode Édition (Machine à états simple pour le swap de 2 séances)
    private boolean modeEdition = false;
    private JButton premierBoutonSelectionne = null;     // 1er clic (Source)
    private Semaine premiereSemaineSelectionnee = null;
    private int premierIndexJour = -1;
    
    // Composantes pour la liste visuelle des profils et modèle de données de la listes
    private JList<String> listProfils;
    private DefaultListModel<String> listModelProfils;
    
    // Champs d'édition du profil (Modifiables dans l'onglet Profil)
    private JTextField editPoids, editVma, editTaille, editVille, editFcMax, editFcRepos;

    // -Constantes de configuration
    // Identifiants des Cartes pour le CardLayout
    private static final String VIEW_LANDING = "Landing";
    private static final String VIEW_WIZARD_1 = "Wiz1";
    private static final String VIEW_WIZARD_2 = "Wiz2";
    private static final String VIEW_WIZARD_3 = "Wiz3";
    private static final String VIEW_LOADING = "Loading";
    private static final String VIEW_DASHBOARD = "Dashboard";
    // Pallette de couleurs
    private static final Color COLOR_BG = new Color(30, 33, 36); // Fond principal sombre
    private static final Color COLOR_PANEL = new Color(45, 48, 53); // Fond des panneaux
    private static final Color COLOR_TEXT = new Color(240, 240, 240); // Texte clair
    private static final Color COLOR_SUCCESS = new Color(46, 204, 113); // Vert (Succès/validé)
    private static final Color COLOR_MISSED = new Color(231, 76, 60);  //Rouge ( Non réalisation)
    private static final Color COLOR_ACCENT = new Color(52, 152, 219); // Bleu 
    private static final Color COLOR_SELECTION = new Color(241, 196, 15); // Jaune ( Mode édition)
    private static final Color COLOR_AS10 = new Color(155, 89, 182);  // Violet(Allure spécifique)

    // Constructeur et Initialisation
    public Interface() {
        /// Instanciation des services métier dès le démarrage
        this.managerProfils = new GestionnaireProfils();
        this.generateur = new GenerateurPlan();
        this.serviceMeteo = new ServiceMeteo();
        this.utilisateur = new Utilisateur();  // Objet vide en attendant le chargement/création

        setTitle("Mon Coach 10km");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Pour forcer la sauvegarde on désactive la fermeture par défaut
        // Pour intercepter la fermeture de la fenêtre
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                sauvegarderEtQuitter(); // Appel de la méthode de persistance avant de tuer le processus
            }
        });

        setSize(1150, 780); // Dimensions fixes
        setLocationRelativeTo(null); // Centre l'écran
        // Initialisation du conteneur principal
        // Permet d'afficher qu'un seul panneau à la fois
        mainContainer = new JPanel(new CardLayout());
        cardLayout = (CardLayout) mainContainer.getLayout();
        
        // Instanciation immédiate des différents écrans 
        viewLanding = createPanelLanding(); // Ecran d'accueil
        JPanel pWiz1 = createPanelEtape1(); // Etape 1 : Choix du sexe
        JPanel pWiz2 = createPanelEtape2(); // Etape 2 : Données perso
        JPanel pWiz3 = createPanelEtape3(); // Paramètres du plan
        JPanel pLoad = createPanelLoading(); // Ecran d'attente on ne le voit pas vraiment car ça charge vite
        JPanel pDash = createDashboardPanel(); // L'interface principale
        // Enregistrement des vues dans le CardLayout avec une clé unique qui permet de naviguer
        mainContainer.add(viewLanding, VIEW_LANDING);
        mainContainer.add(pWiz1, VIEW_WIZARD_1);
        mainContainer.add(pWiz2, VIEW_WIZARD_2);
        mainContainer.add(pWiz3, VIEW_WIZARD_3);
        mainContainer.add(pLoad, VIEW_LOADING);
        mainContainer.add(pDash, VIEW_DASHBOARD);
        // Le mainContainer est définit comme le contenu racine 
        setContentPane(mainContainer);
        // Chargement initial des données depuis le disque (CSV)
        rafraichirListeProfils();
        cardLayout.show(mainContainer, VIEW_LANDING); // Affichage de l'écran de démarrage par défaut
    }
    
    // Logique métier
    // Ces méthodes agissent comme un "Contrôleur" interne : elles font le lien entre 
    // les actions de l'utilisateur (Vue) et le Modèle (Données/Métier).

    //Met à jour le modèle de données de la JList (Vue) à partir du Manager (Données).
    private void rafraichirListeProfils() {
        listModelProfils.clear(); // Nettoyage de l'interface
        // Récupération des noms de profils et ajout de ces derniers
        for (String nom : managerProfils.listerNomsProfils()) {
            listModelProfils.addElement(nom);
        }
    }
    // Gère la sécuritée de la fermeture
    private void sauvegarderEtQuitter() {
        try {
            // On ne sauvegarde que si un profil valide est chargé
            if (utilisateur != null && utilisateur.getNom() != null) {
                managerProfils.sauvegarder(utilisateur, planActuel); // Sérialise l'objet Utilisateur et le plan en CSV
            }
        } catch (Exception e) { e.printStackTrace(); } // Gestion d'une erreur critique
        System.exit(0); // Arrêt de la JVM
    }
    // Charge un profil existant de décide la vue à afficher
    private void chargerProfilSelectionne(String nom) {
        try {
            GestionnaireProfils.DonneeSauvegardee data = managerProfils.charger(nom);
            this.utilisateur = data.utilisateur;
            this.planActuel = data.plan;

            if (planActuel != null) {
                allerAuDashboard(); // Si le plan existe déjà on va direct au tableau de bord
            } else {
                cardLayout.show(mainContainer, VIEW_WIZARD_3);  // Cas rare d'un profil créé mais sans plan on reprend le wiz à la fin
            }
        } catch (Exception e) { // Gestion d'erreur utilisateur
            JOptionPane.showMessageDialog(this, "Erreur chargement : " + e.getMessage());
        }
    }
    // Prépare et affichage le Dashboard principal c'est ici qu'on iniatialise les vues lourdes.
    private void allerAuDashboard() {
        buildPlanCalendrier(planActuel); // Construction dynamique de l'interface utilisateur du calendrier selon le plan
        // Rafraichissement des jauges , graphiques , formulaire profil
        updateViewAccueil();
        updateViewStats();
        updateViewProfilData();
        // Reset de la navigation interne du Dashboard sur l'onglet accueil
        ((CardLayout)contentPanel.getLayout()).show(contentPanel, "ACCUEIL");
        // Bascule de la vie racine
        cardLayout.show(mainContainer, VIEW_DASHBOARD);
    }
    
    /**
     * Recalcule les descriptions des séances EF existantes
     * pour mettre à jour les zones cardiaques (bpm) si la FCMax change.
     */
    private void mettreAJourDescriptionsPlan(PlanEntrainement plan, Utilisateur u) {
        if (plan == null) return;
        // Itération sur toutes les séances
        for (Semaine s : plan.getSemaines()) {
            for (Seance se : s.getSeances()) {
                if (se.getType() == TypeSeance.ENDURANCE_FONDAMENTALE) {
                    String newDesc = se.getDureePrevueEnMinutes() + " min EF (" 
                                   + String.format("%.1f", se.getDistancePrevueEnKm()) + " km) - " 
                                   + CalculPhysiologique.getZoneCardiaque(u, 0.65, 0.75);
                    se.setDescription(newDesc);
                }
            }
        }
    }
    // Réinitialise tous les composants du formulaire de création pour éviter de devoir supprimer le texte du profil précédent
    private void resetFormulaire() {
        // Reset des sélections
        if(groupSexe != null) groupSexe.clearSelection();
        // Reset des champs textes
        if(txtNom != null) txtNom.setText("");
        if(txtPrenom != null) txtPrenom.setText("");
        if(txtVille != null) txtVille.setText("");
        if(txtAge != null) txtAge.setText("");
        if(txtTaille != null) txtTaille.setText("");
        if(txtPoids != null) txtPoids.setText("");
        if(txtFcMax != null) txtFcMax.setText("");
        if(txtFcRepos != null) txtFcRepos.setText("");
        if(txtVma != null) txtVma.setText("");
        if(txtVolume != null) txtVolume.setText("");
        // Valeur par défaut (date du jour)
        if(txtDateDebut != null) txtDateDebut.setText(LocalDate.now().toString());
        // Gestion des modes
        if(txtChrono != null) { txtChrono.setText(""); txtChrono.setEnabled(false); }
        if(groupObjectif != null) groupObjectif.clearSelection();
        if(comboDuree != null) comboDuree.setSelectedIndex(0);
        // Réactivation des champs potentiellement grisé
        if(radioPerf != null) {
            radioPerf.setEnabled(true);
            radioPerf.setToolTipText(null);
        }
    }

    // Construction des vues

    // Crée le panneau d'accueil et utilise un centrage précis des élément
    private JPanel createPanelLanding() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(COLOR_BG); // Thème sobre
        // COnfiguration des contraintes de placement pour le GridBagLaoyout
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.gridx = 0; gbc.gridy = 0;
        // Titre principal malheuresement nous n'arrivons pas à faire afficher le bonhomme qui court
        JLabel l = new JLabel("🏃 MON COACH 10KM");
        l.setFont(new Font("SansSerif", Font.BOLD, 32));
        l.setForeground(COLOR_ACCENT);
        p.add(l, gbc);
        // Liste des profils existants
        gbc.gridy++;
        listModelProfils = new DefaultListModel<>();
        listProfils = new JList<>(listModelProfils);
        listProfils.setBackground(COLOR_PANEL);
        listProfils.setForeground(COLOR_TEXT);
        listProfils.setFont(new Font("SansSerif", Font.PLAIN, 18));
        // Ajout d'une fonctionnalité pour scroller si il y a bcp de profils
        JScrollPane scroll = new JScrollPane(listProfils);
        scroll.setPreferredSize(new Dimension(400, 250)); // Taille fixe de la zone de liste
        scroll.setBorder(new LineBorder(COLOR_ACCENT, 1));
        p.add(scroll, gbc);
        // Panneau de boutons ( Charger , Nouveau , Supprimer)
        gbc.gridy++;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        btnPanel.setBackground(COLOR_BG);
        // Bouton Charger
        JButton btnLoad = createStyledButton("📂 Charger Profil", COLOR_SUCCESS);
        btnLoad.addActionListener(e -> {
            String sel = listProfils.getSelectedValue();
            if (sel != null) chargerProfilSelectionne(sel);
        });
        // Bouton nouveau
        JButton btnNew = createStyledButton("➕ Créer Nouveau", COLOR_ACCENT);
        btnNew.addActionListener(e -> {
            utilisateur = new Utilisateur();
            planActuel = null;
            resetFormulaire();
            cardLayout.show(mainContainer, VIEW_WIZARD_1);
        });
        // Bouton supprimer
        JButton btnDel = createStyledButton("🗑 Supprimer", COLOR_MISSED);
        btnDel.addActionListener(e -> {
             String sel = listProfils.getSelectedValue();
             if (sel != null && JOptionPane.showConfirmDialog(this, "Supprimer " + sel + " ?") == 0) {
                 managerProfils.supprimer(sel);
                 rafraichirListeProfils();
             }
        });

        btnPanel.add(btnLoad);
        btnPanel.add(btnNew);
        btnPanel.add(btnDel);
        p.add(btnPanel, gbc);

        return p;
    }
    // Structure principale du Dashboard ( Barre de navig + zone de contenu)
    private JPanel createDashboardPanel() {
        JPanel dashboard = new JPanel(new BorderLayout());
        // barre de navigation
        navBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        navBar.setBackground(COLOR_PANEL);
        // Création des boutons de navigation
        navBar.add(createNavButton("Accueil", "ACCUEIL"));
        navBar.add(createNavButton("Calendrier", "CALENDRIER"));
        navBar.add(createNavButton("Statistiques", "STATS"));
        navBar.add(createNavButton("Mon Profil", "PROFIL"));
        // Bouton de déconnexion
        JButton btnLogout = new JButton("Changer de Profil");
        btnLogout.setBackground(Color.GRAY);
        btnLogout.setForeground(Color.WHITE);
        btnLogout.addActionListener(e -> {
            // Sauvegarde de précaution
            try { managerProfils.sauvegarder(utilisateur, planActuel); } catch(Exception ex){}
            rafraichirListeProfils();
            cardLayout.show(mainContainer, VIEW_LANDING); // Retour à l'accueil
        });
        
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.add(navBar, BorderLayout.CENTER);
        topBar.add(btnLogout, BorderLayout.EAST);
        dashboard.add(topBar, BorderLayout.NORTH);
        // Zone de contenu utilise un CardLayout imbriqué pour changer d'onglet sans recharger toute la fenêtre
        contentPanel = new JPanel(new CardLayout());
        // Instanciation des panneaux enfants
        viewAccueil = new JPanel(); // Rempli dynamiquement
        viewCalendrier = createViewCalendrier();  // Construit une seule fois
        viewStats = new JPanel(); 
        viewProfil = createViewProfil(); 
        // Enregistrement des onglets
        contentPanel.add(viewAccueil, "ACCUEIL");
        contentPanel.add(viewCalendrier, "CALENDRIER");
        contentPanel.add(viewStats, "STATS");
        contentPanel.add(viewProfil, "PROFIL");
        dashboard.add(contentPanel, BorderLayout.CENTER);

        return dashboard;
    }
    // Méthode pour avoir le même style de boutons de navigation à chaque fois
    private JButton createNavButton(String text, String viewName) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setBackground(COLOR_PANEL);
        btn.setForeground(Color.WHITE);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.addActionListener(e -> {
            CardLayout cl = (CardLayout) contentPanel.getLayout();
            // On rafraichit les données juste avant d'afficher la vue
            if(viewName.equals("ACCUEIL")) updateViewAccueil();
            if(viewName.equals("STATS")) updateViewStats();
            if(viewName.equals("PROFIL")) updateViewProfilData();
            // Si on quitte le calendrier on désactive le mode édition par sécurité
            if(!viewName.equals("CALENDRIER")) resetModeEdition();
            cl.show(contentPanel, viewName);
        });
        return btn;
    }
    // Construction de la vue d'accueil avec le cercle de complétion
    private void updateViewAccueil() {
        viewAccueil.removeAll(); // Reset complet
        viewAccueil.setLayout(new GridBagLayout());
        viewAccueil.setBackground(COLOR_BG);

        if (planActuel == null) return; // Sécurité

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 20, 20, 20);
        gbc.gridx = 0; gbc.gridy = 0;
        // Message de bienvenue
        JLabel lbl = new JLabel("Bonjour, " + utilisateur.getNomComplet());
        lbl.setFont(new Font("SansSerif", Font.BOLD, 32));
        lbl.setForeground(COLOR_TEXT);
        viewAccueil.add(lbl, gbc);
        // Calcul des indicateurs de performance
        this.seancesTotal = 0;
        this.seancesFaites = 0;
        for (Semaine s : planActuel.getSemaines()) {
            for (Seance sc : s.getSeances()) {
                if (sc.getType() != TypeSeance.REPOS) {
                    this.seancesTotal++;
                    if (sc.getStatut() == StatutSeance.REALISEE) this.seancesFaites++;
                }
            }
        }
        // Calcul du pourcentage d'avancement (entier)
        int pct = (seancesTotal > 0) ? (int)((seancesFaites * 100) / seancesTotal) : 0;

        gbc.gridy++;
        // Construction du cercle de complétion
        JPanel circle = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Calcul des dimensions
                int s = Math.min(getWidth(), getHeight()) - 20;
                int x = (getWidth()-s)/2, y = (getHeight()-s)/2;
                // Dessin du fond du cercle
                g2.setColor(COLOR_PANEL);
                g2.setStroke(new BasicStroke(25, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawOval(x+15, y+15, s-30, s-30);
                // Dessin de l'arc de progression
                g2.setColor(COLOR_SUCCESS);
                g2.draw(new Arc2D.Float(x+15, y+15, s-30, s-30, 90, -(360*pct/100), Arc2D.OPEN));
                // Dessin du texte ( pourcentage)
                g2.setColor(COLOR_TEXT);
                g2.setFont(new Font("SansSerif", Font.BOLD, 48));
                String t = pct + "%";
                // Centrage précis du texte grâce à FontMetrics
                g2.drawString(t, getWidth()/2 - g2.getFontMetrics().stringWidth(t)/2, getHeight()/2 + 10);
            }
        };
        circle.setPreferredSize(new Dimension(300,300));
        circle.setBackground(COLOR_BG);
        viewAccueil.add(circle, gbc);
        viewAccueil.revalidate(); viewAccueil.repaint(); // Force le dessin
    }
    // Gestion du calendrier

    // Initialise la structure du calendrier et utilise JSplitPane pour séparer la partie haute des détails en bas
    private JPanel createViewCalendrier() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(COLOR_BG);
        p.setBorder(new EmptyBorder(10,10,10,10));
        // Panneau des détails en bas
        JPanel detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.setBackground(COLOR_PANEL);
        // Zone de texte pour afficher les infos de la séance
        detailsTextArea = new JTextArea(5, 60);
        detailsTextArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        detailsTextArea.setEditable(false);
        detailsTextArea.setBackground(COLOR_PANEL);
        detailsTextArea.setForeground(COLOR_TEXT);
        // Conteneur pour les boutons valider non réalisé
        panelActionButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelActionButtons.setBackground(COLOR_PANEL);
        
        detailsPanel.add(new JScrollPane(detailsTextArea), BorderLayout.CENTER);
        detailsPanel.add(panelActionButtons, BorderLayout.SOUTH);
        // Panneau onglets du haut pour naviguer entre les semaines
        tabbedPanePlan = new JTabbedPane();
        // Mode édition
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        toolbar.setBackground(COLOR_BG);
        btnModeEdition = new JToggleButton("Mode Édition (Déplacer)"); // Pour le drag & drop
        btnModeEdition.addActionListener(e -> {
            modeEdition = btnModeEdition.isSelected();
            if(!modeEdition) {
                resetModeEdition(); // On nettoie l'état
            } else {
                // Entrée en mode édition : changement de couleur
                btnModeEdition.setBackground(COLOR_SELECTION);
                btnModeEdition.setForeground(Color.BLACK);
                btnModeEdition.setText("ÉDITION ACTIVE (Arrêter)");
                detailsTextArea.setText("MODE ÉDITION :\nCliquez sur 2 cases pour échanger.");
                panelActionButtons.removeAll();
                panelActionButtons.repaint();
            }
        });
        toolbar.add(btnModeEdition);
        // Assemblage final
        p.add(toolbar, BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabbedPanePlan, detailsPanel);
        split.setResizeWeight(0.7); // 70% de l'espace pour le calendrier
        p.add(split, BorderLayout.CENTER);
        return p;
    }

    // Construit dynamiquement la grille des boutons selon le plan on s'adapte aux données

    private void buildPlanCalendrier(PlanEntrainement plan) {
        tabbedPanePlan.removeAll(); // Reset des onglets
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM");
        
        if(utilisateur.getVille()!=null && !utilisateur.getVille().isEmpty()) {
            new Thread(() -> { // Lancement d'un thread dédié pour ne pas bloquer l'EDT
                // Utilise la référence au service météo
                ServiceMeteo sm = serviceMeteo;
                try {
                    cacheMeteo = sm.getPrevisionsMeteo(utilisateur.getVille());
                    // Une fois les données reçues on les mets à jour plus tard
                    SwingUtilities.invokeLater(this::rafraichirIconesMeteo);
                } catch (ServiceMeteo.VilleNonTrouveeException ex) {
                    System.err.println("Météo non récupérable pour " + utilisateur.getVille() + ": " + ex.getMessage());
                }
            }).start();
        }
        // Construction des Semaines
        for (Semaine sem : plan.getSemaines()) {
            int nbJours = sem.getSeances().size();
            JPanel wp = new JPanel(new GridLayout(1, nbJours, 5, 5)); 
            wp.setBackground(COLOR_BG); 
            wp.setBorder(new EmptyBorder(5,5,5,5));
            Semaine sRef = sem;
            // Pour chaque séance , création d'un bouton représentant le jour
            for (int i=0; i<sem.getSeances().size(); i++) {
                int idx = i; 
                Seance se = sem.getSeances().get(i);
                JButton b = new JButton(genererTexteBouton(se));
                b.setName("BTN_"+se.getDate().toString()); // On utilise le nom du composant pour stocker sa date
                b.setBackground(getColorForSeance(se)); // Code couleur des séances
                b.setForeground(Color.WHITE);
                if(se.getType() == TypeSeance.REPOS) {
                    b.setForeground(Color.LIGHT_GRAY);
                }
                // Listener qui gère le clic ( Détail ou Swap)
                b.addActionListener(ev -> handleDayClick(b, sRef, idx, se));
                wp.add(b);
            }
            tabbedPanePlan.addTab("S" + sem.getNumero(), wp);
        }
    }
    // Génère le label HTML d'un bouton de séance
    private String genererTexteBouton(Seance se) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM");
        StringBuilder sb = new StringBuilder("<html><center>"); //Centrage
        // Date en gris clair
        sb.append("<font size='3' color='#e0e0e0'>").append(se.getDate().format(dtf)).append("</font><br>");
        // Type de séance en gras
        sb.append("<font size='4'><b>").append(getAbbr(se.getType())).append("</b></font>");
        // Météo si disponible
        if(cacheMeteo.containsKey(se.getDate())) {
            sb.append("<br><font size='3' color='#dddddd'><i>")
              .append(cacheMeteo.get(se.getDate()))
              .append("</i></font>");
        }
        // Indicateur d'état(coche, croix , cercle vide)
        if (se.getType() != TypeSeance.REPOS) {
            sb.append("<br><br>"); 
            if (se.getStatut() == StatutSeance.REALISEE) {
                sb.append("<font size='6' color='white'>&#10003;</font>"); // Petit check
            } else if (se.getStatut() == StatutSeance.NON_REALISEE) {
                sb.append("<font size='6' color='#ffcccc'>&#10007;</font>"); // Croix
            } else {
                sb.append("<font size='5' color='#a0a0a0'>&#9675;</font>"); // Cercle vide
            }
        }
        sb.append("</center></html>");
        return sb.toString();
    }
    // Callback exécuté une fois la météo reçue parcourt l'interface pour mettre à jour le texte 
    private void rafraichirIconesMeteo() {
        SwingUtilities.invokeLater(() -> {
            // Parcours des semaines
            for(int i=0; i<tabbedPanePlan.getTabCount(); i++) {
                JPanel p = (JPanel) tabbedPanePlan.getComponentAt(i);
                // Parcours des boutons
                for(Component c : p.getComponents()) {
                    if(c instanceof JButton) {
                        JButton b = (JButton)c;
                        // Identification du bouton par son nom
                        if(b.getName()!=null && b.getName().startsWith("BTN_")) {
                            LocalDate d = LocalDate.parse(b.getName().replace("BTN_",""));
                            Seance se = trouverSeanceParDate(d);
                            // Maj du texte HTML avec la météo
                            if(se != null) b.setText(genererTexteBouton(se));
                        }
                    }
                }
                p.revalidate(); p.repaint();
            }
            tabbedPanePlan.revalidate(); tabbedPanePlan.repaint();
        });
    }
    // Permet de retrouver l'objet correspondant à la date
    private Seance trouverSeanceParDate(LocalDate d) {
        if(planActuel == null) return null;
        for(Semaine s : planActuel.getSemaines()) {
            for(Seance se : s.getSeances()) {
                if(se.getDate().equals(d)) return se;
            }
        }
        return null;
    }
    // Logique d'interaction

    //Gestionnaire des clics sur les jours du calendrier
    private void handleDayClick(JButton btn, Semaine sem, int idx, Seance se) {
        // Cas du mode lecture/ validation 
        if(!modeEdition) {
            panelActionButtons.removeAll();
            if(se.getType()==TypeSeance.REPOS) detailsTextArea.setText("REPOS");
            else {
                // Construction du résumé textuel détaillé 
                String details = "DATE: "+se.getDate()+"\nTYPE: "+se.getType()+"\nDESC: "+se.getDescription()
                    +"\nINTENSITÉ: "+se.getIntensitePrevue()+"\nDIST: "+String.format("%.1f",se.getDistancePrevueEnKm())+"km\nSTATUT: "+se.getStatut();
                // Ajout du motif si la séance n'est pas réalisé
                if (se.getStatut() == StatutSeance.NON_REALISEE && se.getMotifAnnulation() != null && !se.getMotifAnnulation().isEmpty()) {
                    details += "\n⚠️ MOTIF : " + se.getMotifAnnulation();
                }
                detailsTextArea.setText(details);
                // Bouton valider
                JButton bVal = createStyledButton("✅ Valider", COLOR_SUCCESS);
                bVal.addActionListener(e->{ 
                    se.setStatut(StatutSeance.REALISEE);  // maj
                    se.setMotifAnnulation(""); 
                    btn.setText(genererTexteBouton(se)); // maj vue du texte bouton
                    updateViewAccueil(); updateViewStats();  // Rafraichissement des vues dépendantes
                    try{managerProfils.sauvegarder(utilisateur,planActuel);}catch(Exception ex){} 
                    handleDayClick(btn, sem, idx, se); // appel pour rafraichir le panneau de détails
                });
                //Bouton non réalisé
                JButton bMiss = createStyledButton("❌ Non réalisé", COLOR_MISSED);
                bMiss.addActionListener(e->{ 
                    // Appel pour qualifier l'échec pour avoir un feedback
                    String motif = demanderMotifAnnulation();
                    if (motif != null) {
                        se.setStatut(StatutSeance.NON_REALISEE); se.setMotifAnnulation(motif); 
                        btn.setText(genererTexteBouton(se)); updateViewAccueil(); updateViewStats(); 
                        try{managerProfils.sauvegarder(utilisateur,planActuel);}catch(Exception ex){} 
                        handleDayClick(btn, sem, idx, se);
                    }
                });
                // Ajout dynamique des boutons
                panelActionButtons.add(bVal); panelActionButtons.add(bMiss);
            }
            panelActionButtons.revalidate(); panelActionButtons.repaint(); // Forcer le redessin du panneau d'action
        } else { // Autre cas mode edtion avec gestion des interdictions ( course , veille, séance cochée)
             if(se.getType() == TypeSeance.COURSE_10KM || se.getType() == TypeSeance.VEILLE_DE_COURSE) {
                 JOptionPane.showMessageDialog(this, "Impossible de déplacer la course ou la veille de course !");
                 resetModeEdition(); return; 
             }
             if(se.getStatut() != StatutSeance.PREVUE) {
                 JOptionPane.showMessageDialog(this, "Impossible de déplacer une séance déjà validée ou annulée !");
                 resetModeEdition(); return;
             }
             // Pour établir la première séance cliquée pour le swap
             if(premierBoutonSelectionne == null) {
                premierBoutonSelectionne = btn; premiereSemaineSelectionnee = sem; premierIndexJour = idx; btn.setBorder(new LineBorder(Color.WHITE, 2));
            } else { // Deuxième séance cliquée et swap 
                if(sem == premiereSemaineSelectionnee && btn != premierBoutonSelectionne) { // Vérification qu'on est dans la même semaine contrainte imposé par nos soins
                    int currentTab = tabbedPanePlan.getSelectedIndex(); // Mémorise l'onglet actif
                    Collections.swap(sem.getSeances(), premierIndexJour, idx); // echange
                    // Changement des dates
                    LocalDate d1 = sem.getSeances().get(premierIndexJour).getDate();
                    LocalDate d2 = sem.getSeances().get(idx).getDate();
                    sem.getSeances().get(premierIndexJour).setDate(d2);
                    sem.getSeances().get(idx).setDate(d1);
                    // Calendrier complet pour refleter l'échanger
                    buildPlanCalendrier(planActuel);
                    // Restauration de l'onglet actif
                    if(currentTab >= 0 && currentTab < tabbedPanePlan.getTabCount()) tabbedPanePlan.setSelectedIndex(currentTab);
                    try{managerProfils.sauvegarder(utilisateur,planActuel);}catch(Exception ex){}
                }
                // On sort du mode édition peut importe si ça marche ou pas
                resetModeEdition();
            }
        }
    }
    // Dialogue pour qualifier la raison de la non réalisation
    private String demanderMotifAnnulation() {
        String[] options1 = {"Allure trop rapide", "Pas disponible", "Trop fatigué"};
        int c1 = JOptionPane.showOptionDialog(this, "Raison principale ?", "Séance non réalisée", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options1, options1[0]);
        if (c1 == 0) return "Allure trop rapide"; if (c1 == 1) return "Pas disponible";
        if (c1 == 2) { // On essaye de mieux comprendre c'est dans le but d'une évolution de l'application avec de la data analyse
            String[] options2 = {"Fatigue (Entraînement)", "Maladie/Travail"};
            int c2 = JOptionPane.showOptionDialog(this, "Origine de la fatigue ?", "Précision", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options2, options2[0]);
            return (c2 == 0) ? "Fatigue (Entraînement)" : "Fatigue (Maladie/Travail)";
        }
        return null;
    }
    // Réinitialise la machine à états du mode edition et nettoie les variables temporaires et l'interface ( bordures , texte bouton)
    private void resetModeEdition() {
        modeEdition = false;
        // Suppresion du feedback sur le 1 er bouton
        if(premierBoutonSelectionne != null) premierBoutonSelectionne.setBorder(null);
        // Reset des pointeurs
        premierBoutonSelectionne = null; premiereSemaineSelectionnee = null; premierIndexJour = -1;
        if(btnModeEdition != null) {
            btnModeEdition.setSelected(false);
            btnModeEdition.setText("Mode Édition (Déplacer)");
            btnModeEdition.setBackground(UIManager.getColor("Button.background"));
            btnModeEdition.setForeground(COLOR_TEXT);
        }
        // Reset de la zone d'info
        if(detailsTextArea != null) {
            detailsTextArea.setText("Cliquez sur un jour pour voir les détails.");
            panelActionButtons.removeAll(); panelActionButtons.repaint();
        }
    }
    // Séparation de la logique de rendu ( Couleur/texte)
    private Color getColorForSeance(Seance s) {
        if(s.getType()==TypeSeance.REPOS) return Color.GRAY;
        if(s.getType()==TypeSeance.VMA) return new Color(192, 57, 43);
        if(s.getType()==TypeSeance.SEUIL) return new Color(243, 156, 18);
        if(s.getType()==TypeSeance.COURSE_10KM) return new Color(41, 128, 185);
        if(s.getType()==TypeSeance.ALLURE_SPECIFIQUE) return COLOR_AS10;
        return new Color(39, 174, 96);
    }

    private String getAbbr(TypeSeance t) {
        // Texte court pour les boutons
        if(t==TypeSeance.ENDURANCE_FONDAMENTALE) return "EF";
        if(t==TypeSeance.ALLURE_SPECIFIQUE) return "AS10";
        if(t==TypeSeance.VEILLE_DE_COURSE) return "Veille";
        if(t==TypeSeance.COURSE_10KM) return "COURSE";
        return t.toString();
    }
    // Vues Statistiques
    // Génère un graphique à barres comparant le volume prévu vs réalisé
    private void updateViewStats() {
        viewStats.removeAll(); // Reset de la vue
        viewStats.setLayout(new BorderLayout());
        viewStats.setBackground(COLOR_BG);
        if(planActuel==null) return;
        // Panneau graphique personnalisé
        JPanel graph = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                // Pour des graphiques nets
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight(), pad=50;
                int barW = (w-2*pad) / Math.max(1, planActuel.getDureeEnSemaines()) - 20;
                // On cherche la valeur max pour l'échelle Y
                double maxVal = 0;
                for(Semaine s : planActuel.getSemaines()) {
                    double volP = s.getTotalDistancePrevue();
                    double volR = 0;
                    // On somme uniquement les séances effectivement réalisées
                    for(Seance sc : s.getSeances()) if(sc.getStatut() == StatutSeance.REALISEE) volR += sc.getDistancePrevueEnKm();
                    maxVal = Math.max(maxVal, Math.max(volP, volR));
                }
                if(maxVal==0) maxVal=1; // Evite la division par 0
                // Boucle de dessins
                for(int i=0; i<planActuel.getDureeEnSemaines(); i++) {
                    Semaine s = planActuel.getSemaines().get(i);
                    // Calcul des données brutes
                    double volP = s.getTotalDistancePrevue();
                    double volR = 0;
                    for(Seance sc : s.getSeances()) if(sc.getStatut() == StatutSeance.REALISEE) volR += sc.getDistancePrevueEnKm();
                    // Calcul des coords X et hauteurs
                    int x = pad + i*(barW+20);
                    int hP = (int)((volP/maxVal)*(h-2*pad));
                    // Dessin barre prévu 
                    g2.setColor(new Color(80, 80, 80));
                    g2.fillRoundRect(x, h-pad-hP, barW, hP, 8, 8); // Coins arrondis
                    // Calcul hauteur réalisé
                    int hR = (int)((volR/maxVal)*(h-2*pad));
                    // Vert si objectif atteint de la semaine attein , bleu sinon
                    if(volR >= volP) g2.setColor(COLOR_SUCCESS); else g2.setColor(COLOR_ACCENT);
                    g2.fillRoundRect(x+4, h-pad-hR, barW-8, hR, 8, 8); // Pour des coins arrondis
                    // Labels 
                    g2.setColor(COLOR_TEXT);
                    String txt = String.format("%.0f", volR);
                    // Centrage horizontal du texte au dessus de la barre
                    g2.drawString(txt, x + barW/2 - g2.getFontMetrics().stringWidth(txt)/2, h-pad-hR-5);
                    g2.drawString("S"+(i+1), x + barW/2 - g2.getFontMetrics().stringWidth("S"+(i+1))/2, h-pad+20);
                }
                // Légende
                g2.setColor(Color.GRAY);
                g2.drawLine(pad, h-pad, w-pad, h-pad); // Axe X
                // dessin manuel
                g2.setColor(new Color(80, 80, 80)); g2.fillRect(w-150, 20, 15, 15);
                g2.setColor(COLOR_TEXT); g2.drawString("Prévu", w-130, 32);
                g2.setColor(COLOR_ACCENT); g2.fillRect(w-150, 40, 15, 15);
                g2.setColor(COLOR_TEXT); g2.drawString("Réalisé", w-130, 52);
            }
        };
        graph.setBackground(COLOR_BG);
        // Titre du graphique
        viewStats.add(new JLabel("Volume : Prévu vs Réalisé (km)", SwingConstants.CENTER), BorderLayout.NORTH);
        viewStats.getComponent(0).setForeground(COLOR_TEXT);
        viewStats.add(graph, BorderLayout.CENTER);
        viewStats.revalidate(); viewStats.repaint();
    }
    

    // Gestion du profil
    private JPanel createViewProfil() {
        // Conteneur, le contenu est généré dynamiquement par updateViewProfilDate()
        JPanel p = new JPanel(new GridBagLayout()); p.setBackground(COLOR_BG); return p;
    }
    
    // Construit le formulaire d'édition du profil pour modifier les constantes physiologiques et recalculer les métriques
    private void updateViewProfilData() {
        viewProfil.removeAll();
        viewProfil.setLayout(new GridBagLayout());
        viewProfil.setBackground(COLOR_BG);
        
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 10, 10, 10);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;
        // En tête
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        JLabel t = new JLabel("Mon Profil & Physiologie"); 
        t.setForeground(COLOR_ACCENT); 
        t.setFont(new Font("SansSerif", Font.BOLD, 22));
        t.setHorizontalAlignment(SwingConstants.CENTER);
        viewProfil.add(t, g);
        
        g.gridy++;
        JSeparator sep = new JSeparator();
        sep.setForeground(Color.GRAY);
        viewProfil.add(sep, g);

        g.gridwidth = 1; // retour à une colonne
        // Champs de saisies
        g.gridy++; g.gridx = 0;
        viewProfil.add(createLabel("Ville (Météo) :"), g);
        g.gridx = 1; 
        editVille = new JTextField(utilisateur.getVille(), 10); 
        viewProfil.add(editVille, g);

        g.gridy++; g.gridx = 0;
        viewProfil.add(createLabel("Poids (kg) :"), g);
        g.gridx = 1; 
        editPoids = new JTextField(String.valueOf(utilisateur.getPoids()), 10); 
        viewProfil.add(editPoids, g);
        // Calcul dynamique de l'IMC
        g.gridy++; g.gridx = 0;
        viewProfil.add(createLabel("Taille (cm) :"), g);
        g.gridx = 1; 
        editTaille = new JTextField(String.valueOf(utilisateur.getTaille()), 10); 
        viewProfil.add(editTaille, g);
        
        g.gridy++; g.gridx = 0;
        viewProfil.add(createLabel("IMC (kg/m²) :"), g);
        g.gridx = 1;
        String imcStr = "N/A";
        Color imcColor = COLOR_TEXT;
        try {
            double imc = CalculPhysiologique.calculerIMC(utilisateur.getPoids(), utilisateur.getTaille());
            imcStr = String.format("%.1f", imc);
            // Code couleur santé Vert = Normal , Rouge obésité/maigreur
            if (imc >= 18.5 && imc <= 25) imcColor = COLOR_SUCCESS;
            else if (imc > 30 || imc < 16) imcColor = COLOR_MISSED;
            else imcColor = Color.ORANGE;
        } catch (ValidationException e) { imcStr = "Inv."; }
        
        JLabel lblImc = new JLabel(imcStr);
        lblImc.setForeground(imcColor);
        lblImc.setFont(new Font("SansSerif", Font.BOLD, 14));
        viewProfil.add(lblImc, g);
        // Section données cardio
        g.gridy++; g.gridx = 0; g.gridwidth = 2;
        JLabel subT = new JLabel("--- Données Cardiaques & Performance ---");
        subT.setForeground(Color.GRAY);
        subT.setHorizontalAlignment(SwingConstants.CENTER);
        viewProfil.add(subT, g);
        g.gridwidth = 1;
        // Vma en lecture seule car si on la modifie ça casse le plan
        g.gridy++; g.gridx = 0;
        viewProfil.add(createLabel("VMA (km/h) :"), g);
        g.gridx = 1; 
        editVma = new JTextField(String.valueOf(utilisateur.getVma()), 10); 
        editVma.setEnabled(false); 
        editVma.setBackground(new Color(60, 63, 65)); 
        editVma.setForeground(Color.LIGHT_GRAY);
        viewProfil.add(editVma, g);
        // VO2max à titre indicatif
        g.gridy++; g.gridx = 0;
        viewProfil.add(createLabel("VO2Max (est.) :"), g);
        g.gridx = 1;
        double vo2 = CalculPhysiologique.estimerVO2max(utilisateur.getVma());
        JTextField txtVo2 = new JTextField(String.format("%.1f ml/kg/min", vo2), 10);
        txtVo2.setEditable(false);
        txtVo2.setBorder(null);
        txtVo2.setBackground(COLOR_BG);
        txtVo2.setForeground(COLOR_SUCCESS);
        txtVo2.setFont(new Font("SansSerif", Font.BOLD, 14));
        viewProfil.add(txtVo2, g);
        // Champs modifiables pour la FC
        g.gridy++; g.gridx = 0;
        viewProfil.add(createLabel("FC Max (bpm) :"), g);
        g.gridx = 1;
        editFcMax = new JTextField(String.valueOf(utilisateur.getFcMax()), 10);
        viewProfil.add(editFcMax, g);

        g.gridy++; g.gridx = 0;
        viewProfil.add(createLabel("FC Repos (bpm) :"), g);
        g.gridx = 1;
        editFcRepos = new JTextField(String.valueOf(utilisateur.getFcRepos()), 10);
        viewProfil.add(editFcRepos, g);
        // Boutons de sauvegarde
        g.gridy++; g.gridx = 0; g.gridwidth = 2; g.insets = new Insets(20, 10, 10, 10);
        JButton b = createStyledButton("Enregistrer les modifications", COLOR_ACCENT);
        b.setPreferredSize(new Dimension(200, 40));
        
        b.addActionListener(e -> {
            try { // Parsing des entrées 
                double newPoids = Double.parseDouble(editPoids.getText());
                double newTaille = Double.parseDouble(editTaille.getText());
                int newFcMax = Integer.parseInt(editFcMax.getText());
                int newFcRepos = Integer.parseInt(editFcRepos.getText());
                // Vérifie si les règles sont respectées
                CalculPhysiologique.calculerIMC(newPoids, newTaille);
                CalculPhysiologique.validerDonneesCardiaques(newFcRepos, newFcMax);
                // API Météo
                String newVille = editVille.getText().trim();
                
                if (newVille.isEmpty()) throw new ValidationException("La ville est obligatoire.");

                // Validation de la ville par l'API avant de sauvegarder et l'on appelle que si la ville a changé
                if (!newVille.equals(utilisateur.getVille())) {
                    ServiceMeteo tempMeteo = serviceMeteo; // Utilisation du service existant
                    try {
                        tempMeteo.getCoordonnees(newVille); 
                    } catch (ServiceMeteo.VilleNonTrouveeException ex) {
                        throw new ValidationException("La ville '" + newVille + "' n'est pas reconnue. Veuillez vérifier l'orthographe.");
                    }
                }
                // Mise à jour du modèle
                utilisateur.setVille(newVille); 
                utilisateur.setPoids(newPoids);
                utilisateur.setTaille(newTaille);
                utilisateur.setFcMax(newFcMax);
                utilisateur.setFcRepos(newFcRepos);
                // Mise à jour sur le plan notamment pour la Fcmax avec l'ef les desc changent
                if (planActuel != null) {
                    mettreAJourDescriptionsPlan(planActuel, utilisateur);
                    buildPlanCalendrier(planActuel);
                }

                managerProfils.sauvegarder(utilisateur, planActuel); // Persistance des données
                // Rafraichissement asynchrone de la météo si changement
                cacheMeteo.clear();
                if(!newVille.isEmpty()) {
                     new Thread(() -> {
                        ServiceMeteo sm = serviceMeteo; // Utilisation du service existant
                        try {
                            cacheMeteo = sm.getPrevisionsMeteo(utilisateur.getVille());
                            SwingUtilities.invokeLater(this::rafraichirIconesMeteo);
                        } catch (ServiceMeteo.VilleNonTrouveeException ex) {
                             System.err.println("Météo non récupérable pour " + utilisateur.getVille() + ": " + ex.getMessage());
                        }
                    }).start();
                }
                
                JOptionPane.showMessageDialog(this, "Profil mis à jour !\nDonnées physiologiques et zones cardiaques actualisées.");
                updateViewProfilData(); // Maj de la vue pour confirmer
                
            } catch(ValidationException ex) {
                // Gestion des erreurs
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Données Invalides", JOptionPane.WARNING_MESSAGE);
                // On remet les dernières infos correctes
                editPoids.setText(String.valueOf(utilisateur.getPoids()));
                editTaille.setText(String.valueOf(utilisateur.getTaille()));
                editFcMax.setText(String.valueOf(utilisateur.getFcMax()));
                editFcRepos.setText(String.valueOf(utilisateur.getFcRepos()));
                editVille.setText(utilisateur.getVille());
                
            } catch(NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Veuillez entrer des valeurs numériques valides.", "Erreur de format", JOptionPane.ERROR_MESSAGE);
                editPoids.setText(String.valueOf(utilisateur.getPoids()));
                editTaille.setText(String.valueOf(utilisateur.getTaille()));
                editFcMax.setText(String.valueOf(utilisateur.getFcMax()));
                editFcRepos.setText(String.valueOf(utilisateur.getFcRepos()));
                
            } catch(Exception ex) { 
                JOptionPane.showMessageDialog(this, "Erreur lors de la sauvegarde : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE); 
            }
        });
        viewProfil.add(b, g);
        
        viewProfil.revalidate(); 
        viewProfil.repaint();
    }
    // Wizard de création

    //Etape 1 : Choix du sexe
    private JPanel createPanelEtape1() {
        JPanel p = new JPanel(new GridBagLayout()); p.setBackground(COLOR_BG);
        GridBagConstraints g = new GridBagConstraints(); g.insets=new Insets(10,10,10,10);
        
        JLabel l = new JLabel("Bienvenue ! Pour commencer, veuillez sélectionner votre sexe :"); 
        l.setForeground(COLOR_TEXT); l.setFont(new Font("SansSerif", Font.BOLD, 18));
        p.add(l, g);
        // ButtongGroup pour assurer l'exclusion mutuelle(Homme ou Femme)
        groupSexe = new ButtonGroup();
        radioHomme = new JRadioButton("Homme"); radioHomme.setBackground(COLOR_BG); radioHomme.setForeground(COLOR_TEXT);
        radioFemme = new JRadioButton("Femme"); radioFemme.setBackground(COLOR_BG); radioFemme.setForeground(COLOR_TEXT);
        groupSexe.add(radioHomme); groupSexe.add(radioFemme);
        // Sous panneau pour aligner les radios horizontalement
        JPanel sub = new JPanel(); sub.setBackground(COLOR_BG); sub.add(radioHomme); sub.add(radioFemme);
        g.gridy=1; p.add(sub, g);
        // Bouton de navigation
        JButton b = createStyledButton("Suivant", COLOR_ACCENT);
        b.addActionListener(e -> {
            if(radioHomme.isSelected()) utilisateur.setSexe(Sexe.HOMME);
            else if(radioFemme.isSelected()) utilisateur.setSexe(Sexe.FEMME);
            else return; // Bloque si rien de sélectionné
            // Transition vers l'étape 2
            cardLayout.show(mainContainer, VIEW_WIZARD_2);
        });
        g.gridy=2; p.add(b, g);
        return p;
    }
    
    // Etape 2 : Saisie des données personnelles et physio
    private JPanel createPanelEtape2() {
        JPanel p = new JPanel(new GridBagLayout()); p.setBackground(COLOR_BG);
        GridBagConstraints g = new GridBagConstraints(); g.insets=new Insets(5,5,5,5); g.anchor=GridBagConstraints.WEST;
        // Helper pour avoir une meilleur expérience utilisateur
        txtNom=addFormRow(p, g, "Nom:", "ex: Dupont", 0);
        txtPrenom=addFormRow(p, g, "Prénom:", "ex: Jean", 1);
        txtVille=addFormRow(p, g, "Ville (Météo):", "ex: Paris", 2);
        txtAge=addFormRow(p, g, "Age:", "ex: 25", 3);
        txtTaille=addFormRow(p, g, "Taille (cm):", "ex: 180", 4);
        txtPoids=addFormRow(p, g, "Poids (kg):", "ex: 75.0", 5);
        txtFcMax=addFormRow(p, g, "FCMax:", "ex: 195", 6);
        txtFcRepos=addFormRow(p, g, "FCRepos:", "ex: 60", 7);
        txtVma=addFormRow(p, g, "VMA (km/h):", "ex: 12.5", 8);
        txtVolume=addFormRow(p, g, "Vol Hebdo (km):", "ex: 30.0", 9);
        
        JButton b = createStyledButton("Suivant", COLOR_ACCENT);
        b.addActionListener(e -> {
            try { // Validation des entrées
                if (txtNom.getText().trim().isEmpty()) throw new ValidationException("Le champ 'Nom' est obligatoire.");
                if (txtPrenom.getText().trim().isEmpty()) throw new ValidationException("Le champ 'Prénom' est obligatoire.");
                String villeSaisie = txtVille.getText().trim();
                if (villeSaisie.isEmpty()) throw new ValidationException("Le champ 'Ville' est obligatoire.");
                // Normalisation du nom de fichier et transforme les caractères spéciaux pour éviter les bugs
                String nomFichier = (java.text.Normalizer.normalize(txtNom.getText() + "_" + txtPrenom.getText(), java.text.Normalizer.Form.NFD).replaceAll("[\\p{InCombiningDiacriticalMarks}]", "").replaceAll("[^a-zA-Z0-9_]", "_")); 
                if(managerProfils.existe(nomFichier) && utilisateur.getNom()==null) { if(JOptionPane.showConfirmDialog(this, "Profil existant. Écraser ?", "Attention", JOptionPane.YES_NO_OPTION)!=0) return; } // Vérif si il y a conflit de nom
                
                // Validation API Météo
                ServiceMeteo tempMeteo = serviceMeteo; // Utilisation du service existant
                try {
                    tempMeteo.getCoordonnees(villeSaisie); 
                } catch (ServiceMeteo.VilleNonTrouveeException ex) {
                    throw new ValidationException("La ville '" + villeSaisie + "' n'est pas reconnue. Veuillez vérifier l'orthographe.");
                }
                // Parsing des valeurs numériques
                double poids = Double.parseDouble(txtPoids.getText());
                double taille = Double.parseDouble(txtTaille.getText());
                double vma = Double.parseDouble(txtVma.getText());
                double vol = Double.parseDouble(txtVolume.getText());
                int fcMax = Integer.parseInt(txtFcMax.getText());
                int fcRepos = Integer.parseInt(txtFcRepos.getText());
                // Validation Métier via la couche logique
                CalculPhysiologique.calculerIMC(poids, taille);
                CalculPhysiologique.getPourcentageVMA(10, vma); 
                CalculPhysiologique.validerDonneesCardiaques(fcRepos, fcMax);
                // Pré-calcul du nombre de séances pour l'étape suivante
                if (lblNbSeancesAuto != null) {
                    int minSeances = GenerateurPlan.calculerNbSeancesMin(vol);
                    lblNbSeancesAuto.setText(minSeances + " séances (Calculé selon volume)");
                }
                // Désactivation conditionnelle du mode perf en fonction du volume
                if (vol < 25.0) {
                     if(radioPerf != null) { 
                         radioPerf.setEnabled(false); 
                         radioPerf.setToolTipText("Volume insuffisant pour le mode Performance (min 25km).");
                         radioFinir.setSelected(true);
                         txtChrono.setEnabled(false);
                     }
                } else {
                     if(radioPerf != null) { 
                         radioPerf.setEnabled(true); 
                         radioPerf.setToolTipText(null);
                     }
                }
                
                utilisateur.setNom(txtNom.getText().trim()); 
                utilisateur.setPrenom(txtPrenom.getText().trim()); 
                utilisateur.setVille(villeSaisie); 
                utilisateur.setAge(Integer.parseInt(txtAge.getText())); 
                utilisateur.setTaille(taille); 
                utilisateur.setPoids(poids); 
                utilisateur.setFcMax(fcMax); 
                utilisateur.setFcRepos(fcRepos); 
                utilisateur.setVma(vma); 
                utilisateur.setVolumeHebdoSouhaite(vol); 
                // Passage à l'étape d'après
                cardLayout.show(mainContainer, VIEW_WIZARD_3); 
                
            } catch(NumberFormatException ex) { 
                JOptionPane.showMessageDialog(this, "Veuillez entrer des nombres valides."); 
            }
            catch(ValidationException ex) { 
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Données Invalides", JOptionPane.WARNING_MESSAGE); 
            }
            catch(IllegalArgumentException ex) { 
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Erreur", JOptionPane.WARNING_MESSAGE); 
            }
            catch(Exception ex) { 
                JOptionPane.showMessageDialog(this, "Erreur : " + ex.getMessage()); 
            } 
        });
        g.gridy=10; p.add(b, g); return p;
    }
    // Etape 3: Paramétrage final du plan
    private JPanel createPanelEtape3() {
        JPanel p = new JPanel(new GridBagLayout()); p.setBackground(COLOR_BG);
        GridBagConstraints g = new GridBagConstraints(); g.insets=new Insets(5,5,5,5); g.anchor=GridBagConstraints.WEST;
        // Date par défaut est celle du jour
        g.gridx=0; g.gridy=0; p.add(createLabel("Date (AAAA-MM-JJ):"), g);
        txtDateDebut=new JTextField(LocalDate.now().toString(), 10); g.gridx=1; p.add(txtDateDebut, g);
        // Durée du plan  ( liste déroulante)
        g.gridx=0; g.gridy=1; p.add(createLabel("Durée:"), g);
        comboDuree=new JComboBox<>(new Integer[]{6,7,8,9,10}); g.gridx=1; p.add(comboDuree, g);
        // Sélection du mode
        g.gridx=0; g.gridy=2; p.add(createLabel("Mode:"), g);
        radioFinir=new JRadioButton("Finir"); radioFinir.setBackground(COLOR_BG); radioFinir.setForeground(COLOR_TEXT);
        radioPerf=new JRadioButton("Perf"); radioPerf.setBackground(COLOR_BG); radioPerf.setForeground(COLOR_TEXT);
        groupObjectif=new ButtonGroup(); groupObjectif.add(radioFinir); groupObjectif.add(radioPerf);
        JPanel sub=new JPanel(); sub.setBackground(COLOR_BG); sub.add(radioFinir); sub.add(radioPerf);
        g.gridx=1; p.add(sub, g);
        
        // Affichae du nb séances calculé à l'étape précédente
        g.gridx=0; g.gridy=3; p.add(createLabel("Nb Séances:"), g);
        lblNbSeancesAuto = new JLabel("-");
        lblNbSeancesAuto.setForeground(COLOR_TEXT);
        lblNbSeancesAuto.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.gridx=1; p.add(lblNbSeancesAuto, g);
        // Chrono cible uniquement pour le mode perf
        g.gridx=0; g.gridy=4; p.add(createLabel("Chrono (hh:mm:ss) [ex: 00:50:00]:"), g);
        txtChrono=new JTextField(10); txtChrono.setEnabled(false);  // Désactivé pour mode finir
        txtChrono.setToolTipText("Format Heures:Minutes:Secondes (ex: 00:50:00)");
        g.gridx=1; p.add(txtChrono, g);
        // ZOne d'explication contextuelle
        JTextArea lblInfoMode = new JTextArea(3, 30); lblInfoMode.setEditable(false); lblInfoMode.setBackground(COLOR_BG); lblInfoMode.setForeground(Color.GRAY);
        g.gridx=0; g.gridy=5; g.gridwidth=2; p.add(lblInfoMode, g);
        // Gestion dynamique selon le choix
        radioFinir.addActionListener(e -> { 
            txtChrono.setEnabled(false); 
            lblInfoMode.setText("MODE FINIR :\n• Objectif : Terminer la course.\n• Nb Séances : Auto (selon volume).\n• Allure : ~80% VMA."); 
        });
        radioPerf.addActionListener(e -> { 
            txtChrono.setEnabled(true); 
            lblInfoMode.setText("MODE PERF :\n• Objectif : Battre un chrono.\n• Nb Séances : Auto (selon volume).\n• Allure : > 80% VMA."); 
        });
        radioFinir.doClick(); 
        // Bouton générer lance le calcul final
        JButton b = createStyledButton("GÉNÉRER", COLOR_SUCCESS);
        b.addActionListener(e -> lancerGeneration()); g.gridx=1; g.gridy=6; g.gridwidth=1; p.add(b, g); return p;
    }
    // Vue simple d'attente pendant le calcul
    private JPanel createPanelLoading() { JPanel p=new JPanel(new GridBagLayout()); p.setBackground(COLOR_BG); JLabel l=new JLabel("Calcul en cours..."); l.setForeground(COLOR_TEXT); l.setFont(new Font("SansSerif",Font.BOLD,20)); p.add(l); return p; }
    
    // Génération asynchrone

    // Lance la génération du plan dans un thread séparé pour ne pas geler l'interface
    private void lancerGeneration() {
        try {
            LocalDate d = LocalDate.parse(txtDateDebut.getText());
            if(d.isBefore(LocalDate.now())) { JOptionPane.showMessageDialog(this, "La date de début ne peut pas être dans le passé !"); return; }
            
            // --- VALIDATION CHRONO OBLIGATOIRE EN MODE PERF ---
            if (radioPerf.isSelected() && (txtChrono.getText() == null || txtChrono.getText().trim().isEmpty())) {
                JOptionPane.showMessageDialog(this, "En mode 'Perf', vous devez obligatoirement spécifier un objectif chronométrique (hh:mm:ss).", "Validation requise", JOptionPane.WARNING_MESSAGE);
                return; // Bloque l'exécution
            }
            // Bascule visuelle vers l'écran de chargement
            cardLayout.show(mainContainer, VIEW_LOADING);
            SwingWorker<PlanEntrainement, Void> worker = new SwingWorker<>() {
                @Override
                protected PlanEntrainement doInBackground() throws Exception {
                    // C'est ici que tourne le code hors de l'edt
                    ParametresPlan pp = new ParametresPlan((Integer)comboDuree.getSelectedItem(), radioPerf.isSelected() ? txtChrono.getText() : null, d, 0);
                    return generateur.generer(utilisateur, pp);
                }
                @Override
                protected void done() {
                    // Retour sur l'EDT une fois fini
                    try {
                        planActuel = get(); // Récup du résultat
                        managerProfils.sauvegarder(utilisateur, planActuel); 
                        allerAuDashboard();
                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(Interface.this, "Erreur: " + e.getMessage());
                        cardLayout.show(mainContainer, VIEW_WIZARD_3); // Echec retour au formulaire
                    }
                }
            };
            worker.execute(); // Démarrage du thread
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Erreur de lecture : " + e.getMessage()); }
    }
    // Helpers
    private JTextField addFormRow(JPanel p, GridBagConstraints g, String l, String ex, int y) {
        g.gridx=0; g.gridy=y; p.add(createLabel(l + " [" + ex + "]"), g);
        g.gridx=1; JTextField t=new JTextField(10); p.add(t, g);
        return t;
    }
    
    private JLabel createLabel(String t) { JLabel l = new JLabel(t); l.setForeground(COLOR_TEXT); return l; }
    private JButton createStyledButton(String t, Color c) { JButton b = new JButton(t); b.setBackground(c); b.setForeground(Color.WHITE); b.setFocusPainted(false); return b; }
}
