package main.java.parser;

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
    private static final String DEFAULT_HEADER = "INTERNAL ERROR:" + System.lineSeparator();
    private static final String DEFAULT_MESSAGE = "%s%nError: source file %s source line %d%n%s";

    /**
     * Constructs a new runtime exception with the specified detail message.
     * @param message   the detail message.
     */
    private ParserInternalError(String message) {
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

    @NotNull
    public static ParserInternalError of(String message, @NotNull LineInfo info) {
        return withHeader(String.format(DEFAULT_MESSAGE, message, info.getPath(), info.getLineNumber(), info.infoString()));
    }

    @NotNull
    public static ParserInternalError of(String message, @NotNull Token info) {
        return of(message, info.lineInfo);
    }

    @NotNull
    public static ParserInternalError of(String message, @NotNull BaseNode node) {
        return of(message, node.getLineInfo());
    }
}
