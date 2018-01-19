package exceptions;

/**
 * @author dboyer
 * @version 1.0
 *
 * Exception class to handle cases where there are not enough valid links to complete the crawl.
 */
public class InsufficientLinksException extends RuntimeException {

    public InsufficientLinksException() {
        super("No valid links left!");
    }

    public InsufficientLinksException(int remainingHops) {
        super("Not enough valid links to complete " + String.valueOf(remainingHops) + " hops!");
    }
}
