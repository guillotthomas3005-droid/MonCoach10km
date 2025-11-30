/* Cette classe agit comme un client pour l'API Open-Meteo. Utilisation de l'ia pour nous aider à coder la classe */
/* Elle gère la géolocalisation et la récupération des prévisions météo pour les différentes villes. */

package api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ServiceMeteo {
    
    // Exception signalant que la ville spécifiée n'a pas été trouvée par le service de géolocalisation. 
    public static class VilleNonTrouveeException extends Exception {
        public VilleNonTrouveeException(String ville) {
            super("La ville '" + ville + "' n'a pas été trouvée.");
        }
    }

    public Map<LocalDate, String> getPrevisionsMeteo(String ville) throws VilleNonTrouveeException {
        Map<LocalDate, String> meteoMap = new HashMap<>();
        try {
            double[] coords = getCoordonnees(ville); 
            
            // Si la géolocalisation a échoué (hors VilleNonTrouvee), on arrête là.
            if (coords == null) return meteoMap;

            // En France, les décimales s'écrivent avec une virgule (48,5). L'api étrangère veut un point (48.5). 
            // Locale.US force Java à utiliser le point, sinon l'URL serait invalide et l'appel échouerait. 
            String urlString = String.format(Locale.US, "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&daily=weather_code&timezone=auto", coords[0], coords[1]);
            
            String jsonResponse = callApi(urlString, "Prévisions météo");
            
            String[] dates = extractJsonArray(jsonResponse, "time");
            String[] codes = extractJsonArray(jsonResponse, "weather_code"); 
         
            if (dates != null && codes != null && dates.length == codes.length) {
                for (int i = 0; i < dates.length; i++) {
                    // Nettoyage manuel.
                    // Le JSON renvoie  par exemple"2025-11-27" (avec les guillemets). LocalDate.parse ne veut pas de guillemets. 
                    // On utilise .replace("\"", "") pour les supprimer avant de convertir le texte en objet Date. 
                    meteoMap.put(LocalDate.parse(dates[i].replace("\"", "")), getCodeDescription(Integer.parseInt(codes[i])));
                }
            }
        } catch (VilleNonTrouveeException e) {
            throw e; 
        } catch (Exception e) { 
            System.err.println("Erreur lors de la récupération des prévisions météo : " + e.getMessage());
        }
        return meteoMap;
    }

    private String getCodeDescription(int code) {
        switch (code) {
            case 0: return "Ciel dégagé"; 
            case 1: return "Principalement dégagé"; 
            case 2: return "Partiellement nuageux"; 
            case 3: return "Couvert";
            case 45: return "Brouillard"; 
            case 48: return "Brouillard givrant"; 
            case 51: return "Bruine légère"; 
            case 53: return "Bruine modérée";
            case 55: return "Bruine dense"; 
            case 61: return "Pluie faible"; 
            case 63: return "Pluie modérée"; 
            case 65: return "Pluie forte";
            case 80: return "Averses faibles"; 
            case 81: return "Averses modérées"; 
            case 82: return "Averses violentes"; 
            case 95: return "Orage";
            default: return "Pluie/Neige";
        }
    }

    public double[] getCoordonnees(String ville) throws VilleNonTrouveeException {
        try {
            // Encodage d'URL simpliste.
            // Une URL ne peut pas contenir d'espace. Si l'utilisateur tape "New York", l'URL casse. 
            // On remplace les espaces par "+" pour obtenir "New+York", format standard pour les paramètres Web. 
            String encodedVille = ville.trim().replace(" ", "+");
            
            String urlStr = "https://geocoding-api.open-meteo.com/v1/search?name=" + encodedVille + "&count=1&language=fr&format=json";
            
            String json = callApi(urlStr, "Géolocalisation de la ville");
            
            if (!json.contains("\"latitude\"") || json.contains("\"results\":[]")) {
                throw new VilleNonTrouveeException(ville);
            }
            
            return new double[]{
                Double.parseDouble(extractValue(json, "latitude")), 
                Double.parseDouble(extractValue(json, "longitude"))
            };
        } catch (VilleNonTrouveeException e) { 
            throw e; 
        } catch (Exception e) { 
            System.err.println("Erreur technique lors de la géolocalisation : " + e.getMessage());
            return null; 
        }
    }

    private String callApi(String urlStr, String type) throws Exception {
        System.out.println("--- API CALL (" + type + ") ---");
        System.out.println("URL: " + urlStr);
        
        // On préparer l'adresse du destinataire (Open-Météo)
        URL url = URI.create(urlStr).toURL();
        // On ouvre la connexion
        HttpURLConnection conn = (HttpURLConnection) url.openConnection(); 
        conn.setRequestMethod("GET"); 
        conn.setConnectTimeout(5000);  // To si c'est trop long
        
        // On ouvre un flux de données avec InputStream qui nous permet de recevoir la météo depuis le serveur
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream())); 
        
        StringBuilder content = new StringBuilder(); 
        String inputLine;
        
        // On lit tant qu'il y a des données qui arrivent.
        while ((inputLine = in.readLine()) != null) content.append(inputLine); 
        in.close(); // On referme toujours. 
        
        System.out.println("Status: " + conn.getResponseCode());
        System.out.println("---------------------------------");
        return content.toString();
    }
    
    /* Parsing manuel d'un tableau JSON. */
    /* Extrait la sous-chaîne entre les crochets [...] correspondant à la clé donnée. */
    private String[] extractJsonArray(String json, String key) {
        try { 
            // Calcul d'index précis.
            // On cherche la position du mot clé. 
            // On ajoute la longueur du mot (+ key.length()).
            // On ajoute 4 pour sauter les caractères de syntaxe JSON.
            int s = json.indexOf("\"" + key + "\":[") + key.length() + 4; 
            
            // On coupe (substring) du début du tableau '[' jusqu'au crochet fermant ']'.
            // Puis on découpe (split) à chaque virgule pour avoir les éléments séparés.
            return json.substring(s, json.indexOf("]", s)).split(","); 
        } catch (Exception e) { 
            return null; 
        }
    }

    /* Parsing manuel d'une valeur unique dans un JSON. */
    private String extractValue(String json, String key) {
        try { 
            // Même logique : on saute le mot clé + 3 caractères de syntaxe (" + : + ")
            int s = json.indexOf("\"" + key + "\":") + key.length() + 3; 
            
            // On cherche la virgule suivante (fin de la valeur).
            int e = json.indexOf(",", s); 
            
            // Si pas de virgule, c'est peut-être la fin de l'objet (accolade fermante).
            if(e==-1) e=json.indexOf("}", s); 
            
            return json.substring(s, e); 
        } catch (Exception e) { 
            return "0"; 
        }
    }
}
