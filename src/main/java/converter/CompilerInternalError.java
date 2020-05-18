package main.java.converter;

import main.java.parser.LineInfo;
import main.java.parser.Lined;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing an internal compiler error.
 * <p>
 *     This is similar to {@link main.java.parser.ParserInternalError}, but is
 *     for internal errors in the compiler, not the parser. This class should
 *     be used for errors in the compiler itself, not the code being compiled by
 *     it.
 * </p>
 * <p>
 *     It is not possible to construct this exception publicly, but instead one
 *     of the associated static methods must be used. These will create
 *     consistent error messages with information related to the lines on which
 *     the error occurred.
 * </p>
 */
public class CompilerInternalError extends RuntimeException {
    private static final String DEFAULT_HEADER = "INTERNAL ERROR:" + System.lineSeparator();
    private static final String DEFAULT_MESSAGE = "%s%nError: source file %s source line %d%n%s";

    /**
     * Constructs a new runtime exception with the specified detail message.
     * @param message   the detail message.
     */
    private CompilerInternalError(String message) {
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
    public static CompilerInternalError withHeader(String message) {
        return new CompilerInternalError(DEFAULT_HEADER + message);
    }

    /**
     * Construct a CompilerInternalError with a custom message and a {@link
     * LineInfo}.
     *
     * @param message The message to attach to the error
     * @param info The {@link LineInfo} to get line information from
     * @return The exception
     */
    @NotNull
    public static CompilerInternalError of(String message, @NotNull LineInfo info) {
        return withHeader(String.format(DEFAULT_MESSAGE, message, info.getPath(), info.getLineNumber(), info.infoString()));
    }

    /**
     * Construct a CompilerInternalError with a custom message and a {@link
     * Lined} object from which to construct a {@link LineInfo}.
     *
     * @param message The message to attach to the error
     * @param node The {@link Lined} object to get a {@link LineInfo} from
     * @return The exception
     * @see #of(String, LineInfo)
     */
    @NotNull
    public static CompilerInternalError of(String message, @NotNull Lined node) {
        return of(message, node.getLineInfo());
    }

    /**
     * Create a CompilerInternalError with a formatted error message.
     * <p>
     *     Equivalent to {@code CompilerInternalError.of(String.format(message,
     *     values), node)}.
     * </p>
     *
     * @param message The message to be formatted
     * @param node The {@link Lined} object to get a {@link LineInfo} from
     * @param values The formatting args
     * @return The exception
     * @see #of(String, Lined)
     * @see String#format(String, Object...)
     */
    @NotNull
    public static CompilerInternalError format(String message, Lined node, Object... values) {
        return of(String.format(message, values), node);
    }
}
