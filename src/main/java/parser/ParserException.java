package main.java.parser;

/**
 * The exception thrown when a user input fails
 * @author Patrick Norton
 */
public class ParserException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "%s%nError: File %s Line %d%n%s";

    private final String internalMessage;
    /**
     * Create a tokens.error
     * @param s The string of the exception
     */
    public ParserException(String s) {
        this(s, s);
    }

    public ParserException(String message, Throwable cause) {
        super(message, cause);
        internalMessage = message;
    }

    private ParserException(String newMessage, String internalMessage) {
        super(newMessage);
        this.internalMessage = internalMessage;
    }

    public String getInternalMessage() {
        return internalMessage;
    }

    /**
     * Create a new {@link ParserException} from the token being errored.
     * @param message The message to attach to the error
     * @param token The token to get the context from
     * @return The new exception
     */

    public static ParserException of(String message,Token token) {
        return  ParserException.of(message, token.lineInfo);
    }

    public static ParserException of(String message,BaseNode node) {
        return ParserException.of(message, node.getLineInfo());
    }

   /**
     * Create a new {@link ParserException} from the line info being errored.
     * @param message The message to attach to the error
     * @param lineInfo The info to get the context from
     * @return The new exception
     */

    public static ParserException of(String message,LineInfo lineInfo) {
        return new ParserException(
                String.format(DEFAULT_MESSAGE, message, lineInfo.getPath(),
                        lineInfo.getLineNumber(), lineInfo.infoString()),
                message
        );
    }
}
