PROJET JAVA : MON COACH 10KM
Guillot Thomas, Déchamps Gatien
Novembre 2025


1. DESCRIPTION

Application de bureau en Java de génération et de suivi de plans d'entraînement pour le 10km.
S'adresse aux coureurs amateurs voulant préparer une course de manière structurée.

Fonctionnalités principales :
- Génération personnalisée selon VMA et Objectif (Finir ou Performance) avec calcul automatique du nombre de séances optimal.
- Calendrier interactif (possibilité d'échanger 2 séances d'une semaine) avec affichage de la météo réelle via l'API Open-Meteo.
- Suivi de progression (Graphique Prévu vs Réalisé), validation des séances réalisées ou non (saisie obligatoire d'un motif en cas de non réalisation).
- Sauvegarde automatique des profils au format CSV lisible par Excel avec le ";" comme séparateur.
- Contrôle de cohérence physiologique (IMC, FC Repos/Max) pour éviter les profils à risque.


2. PRÉREQUIS

- Système d'exploitation Windows, macOS ou Linux.
- Java installé (version 8 ou supérieure).
- Connexion Internet (pour la récupération de la météo et la géolocalisation des villes).
- (Optionnel) Utilitaire "Make" pour la compilation automatique.


3. COMPILATION ET EXÉCUTION

Le code est organisé en packages. Ouvrir un terminal à la racine du projet (là où se trouve le fichier Makefile).

Via le Makefile (Recommandé)
- Pour compiler : 
  make

- Pour compiler et lancer l'application : 
  make run

- Pour nettoyer les fichiers compilés : 
  make clean


Compilation Manuelle
Si "Make" n'est pas installé, tapez les commandes suivantes :

Étape 1 : Créer le dossier de destination
mkdir bin

Étape 2 : Compiler le projet (avec encodage UTF-8)
javac -d bin -encoding UTF-8 main/*.java ihm/*.java metier/*.java modele/*.java donnees/*.java api/*.java

Étape 3 : Lancer l'application
java -cp bin main.MainApp


4. STRUCTURE DES FICHIERS
Le code est réparti en plusieurs packages organisés selon le principe suivant :

4.1 PACKAGE MAIN (Point d'entrée)
- MainApp.java : Configure le style visuel et lance l'interface graphique dans le thread approprié.

4.2 PACKAGE IHM (Interface)
- Interface.java: Gère l'intégralité de l'interface graphique. Elle orchestre les interactions entre l'utilisateur et les services métier.

4.3 PACKAGE METIER (Logique)
- GenerateurPlan.java : est la partie cerveau du projet. Il contient les règles de construction du plan : calcul du nombre de séances selon le volume, gestion de la semaine d'affûtage et placement des séances.
- CalculPhysiologique.java : contient toutes les formules mathématiques et de santé : IMC, conversion d'allures, calcul des zones cardiaques et validation des données physiologiques.
- SeanceBibliotheque.java : contient les modèles types de séances (VMA, Seuil, EF...) et les instancie dynamiquement en fonction des paramètres du coureur.
- ValidationException.java : est une exception personnalisée pour bloquer proprement les erreurs métier (ex: VMA invalide, Poids incohérent).

4.4 PACKAGE MODELE (Données)
- Utilisateur.java : représente le coureur. Il stocke les informations personnelles et physiologiques (VMA, FCMax, Poids, Taille...).
- PlanEntrainement.java : est l'objet racine du plan. Il contient la date de début, l'objectif et la liste des semaines.
- Semaine.java : est un conteneur logique regroupant une liste de 7 séances.
- Seance.java : contient la date précise, le type, la description, le statut et le motif d'annulation.
- ParametresPlan.java : objet temporaire pour transporter les choix du formulaire vers le générateur.
- Enumerations (TypeSeance, StatutSeance, Sexe) : pour garantir la cohérence des types dans tout le programme.

4.5 PACKAGE DONNEES
- GestionnaireProfils.java : permet l'écriture et la lecture des fichiers CSV dans un dossier dédié assurant la sauvegarde des données.

4.6 PACKAGE API (Service Externe)
- ServiceMeteo.java : effectue les appels HTTP vers l'API Open-Meteo, gère le géocodage des villes et récupère les prévisions météo.

4.7 FICHIERS DE CONFIGURATION
- Makefile : Script d'automatisation pour la compilation et l'exécution sous Linux/macOS.


5. NOTES IMPORTANTES

- Si le dossier sauvegardes n'est pas initialement vide il est recommandé de le vider
- Le nombre de séances hebdomadaires est calculé automatiquement en fonction du volume kilométrique pour éviter le surentraînement.
- La vma est verrouillée une fois le plan généré pour garantir la cohérence
- La modification des données physiologiques entraîne un recalcul automatique des zones cardiaques dans le plan en cours(utilisé pour l'endurance fondamentale).
- Les prévisions météo nécessitent une connexion internet et une ville valide reconnue par l'API. De plus elles ne s'étendent pas à plus de 7 jours.
