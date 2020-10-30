package main.java.converter;

import main.java.parser.LineInfo;
import main.java.parser.Lined;

/**
 * The class representing an error caused by an operation not yet implemented
 * in the compiler.
 * <p>
 *     This is meant to replace {@link UnsupportedOperationException} in the
 *     compiler, as this exception comes with line information built in, unlike
 *     the builtin exception.
 * </p>
 * @author Patrick Norton
 * @see CompilerException
 * @see CompilerInternalError
 */
public class CompilerTodoError extends RuntimeException {
    private CompilerTodoError(String s) {
        super(s);
    }

    /**
     * Constructs a {@link CompilerTodoError} with a formatted error message.
     *
     * @param message The message to tell the user
     * @param lineInfo The line information for the error
     * @return The exception
     */

    public static CompilerTodoError of(String message,LineInfo lineInfo) {
        return new CompilerTodoError(
                String.format(
                        "%s%nError: Operation not yet implemented%nFile %s Line %d%n%s",
                        message, lineInfo.getPath(),
                        lineInfo.getLineNumber(), lineInfo.infoString()
                )
        );
    }

    /**
     * Constructs a {@link CompilerTodoError} with a formatted error message.
     * <p>
     *     This is equivalent to {@code CompilerTodoError.of(message,
     *     lineInfo.getLineInfo())}.
     * </p>
     *
     * @param message The message to tell the user
     * @param lineInfo The line information for the error
     * @return The exception
     */

    public static CompilerTodoError of(String message,Lined lineInfo) {
        return of(message, lineInfo.getLineInfo());
    }

    /**
     * Creates a CompilerTodoError with a formatted error message.
     * <p>
     *     Equivalent to {@code CompilerTodoError.of(String.format(message,
     *     args), lineInfo)}.
     * </p>
     *
     * @param message The message to be formatted
     * @param lineInfo The {@link Lined} object to get a {@link LineInfo} from
     * @param args The formatting args
     * @return The exception
     * @see #of(String, Lined)
     * @see String#format(String, Object...)
     */

    public static CompilerTodoError format(String message, LineInfo lineInfo, Object... args) {
        return of(String.format(message, args), lineInfo);
    }

    /**
     * Creates a CompilerTodoError with a formatted error message.
     * <p>
     *     Equivalent to {@code CompilerTodoError.of(String.format(message,
     *     args), lineInfo)}.
     * </p>
     *
     * @param message The message to be formatted
     * @param lineInfo The {@link Lined} object to get a {@link LineInfo} from
     * @param args The formatting args
     * @return The exception
     * @see #of(String, Lined)
     * @see String#format(String, Object...)
     */

    public static CompilerTodoError format(String message, Lined lineInfo, Object... args) {
        return of(String.format(message, args), lineInfo);
    }
}
