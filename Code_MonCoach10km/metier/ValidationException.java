/*On regroupe les exceptions de même nature pour gérer les erreurs de manière centralisée. */

package metier;

public class ValidationException extends RuntimeException { 
    public ValidationException(String message) { 
        super(message); 
        } 
    }
