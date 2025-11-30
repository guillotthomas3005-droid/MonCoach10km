/* Point d'entrée de l'application (Main). */
/* Elle configure l'apparence visuelle et lance l'interface graphique. */

package main;

import ihm.Interface;

public class MainApp {
   
    public static void main(String[] args) {

        /* Utilisation de 'invokeLater' pour garantir que l'interface est construite dans le thread de gestion des événements (EDT). */
        /* C'est indispensable pour éviter les bugs d'affichage ou les blocages. */
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                // Tentative d'application du style visuel "Nimbus" pour une interface plus moderne que le style par défaut.
                for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        javax.swing.UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception e) {
                // Si Nimbus n'est pas disponible, l'application se lancera avec le design standard sans planter.
            }
            
            // Instanciation et affichage de la fenêtre principale.
            new Interface().setVisible(true);
        });
    }
}
