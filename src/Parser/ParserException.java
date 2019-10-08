package Parser;

/**
 * The exception thrown when a user input fails
 * @author Patrick Norton
 */
public class ParserException extends RuntimeException {
    /**
     * Create a new ParserException
     * @param s The string of the exception
     */
    public ParserException(String s) {
        super(s);
    }

    public ParserException(String message, Throwable cause) {
        super(message, cause);
    }
}
