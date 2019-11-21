package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The exception thrown when a user input fails
 * @author Patrick Norton
 */
public class ParserException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "%s%nError: Line %d%n%s";

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
    @NotNull
    @Contract("_, _ -> new")
    public static ParserException of(String message, @NotNull Token token) {
        return  ParserException.of(message, token.lineInfo);
    }

   /**
     * Create a new {@link ParserException} from the line info being errored.
     * @param message The message to attach to the error
     * @param lineInfo The info to get the context from
     * @return The new exception
     */
    @NotNull
    @Contract("_, _ -> new")
    public static ParserException of(String message, @NotNull LineInfo lineInfo) {
        return new ParserException(
                String.format(DEFAULT_MESSAGE, message, lineInfo.getLineNumber(), lineInfo.infoString()),
                message
        );
    }
}
