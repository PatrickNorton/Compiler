package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing an internal error of the parser.
 * <p>
 *     This exception should be raised in cases where there is a failure that
 *     was not caused by the fault of the inputted file, but instead by the
 *     parser itself's own failings in logic. These <i>should</i> never be
 *     thrown for any input at all, even invalid ones.
 * </p>
 *
 * @author Patrick Norton
 * @see ParserException
 */
public class ParserInternalError extends RuntimeException {
    public static final String DEFAULT_HEADER = "INTERNAL ERROR:" + System.lineSeparator();

    /**
     * Constructs a new runtime exception with the specified detail message.
     * @param message   the detail message.
     */
    public ParserInternalError(String message) {
        super(message);
    }

    /**
     * Create a new ParserInternalError with a header attached to the given
     * message.
     * @param message The message to be given
     * @return The error itself
     */
    @NotNull
    @Contract("_ -> new")
    public static ParserInternalError withHeader(String message) {
        return new ParserInternalError(DEFAULT_HEADER + message);
    }
}
