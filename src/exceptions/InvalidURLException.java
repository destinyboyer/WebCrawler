package exceptions;

/**
 * @author dboyer
 * @version 1.0
 *
 * Exception class to handle a malformed URL.
 */
public class InvalidURLException extends RuntimeException {

    public InvalidURLException() {
        super("Malformed URL. URL must follow a standard well-formed pattern.");
    }
}
