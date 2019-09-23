package Parser;

/**
 * The exception thrown when a user input fails
 * @author Patrick Norton
 */
public class ParserException extends RuntimeException {
    /**
     * Create a new Parser.ParserException
     * @param s The string of the exception
     */
    public ParserException(String s) {
        super(s);
    }
}