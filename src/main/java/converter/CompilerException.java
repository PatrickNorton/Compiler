package main.java.converter;

import main.java.parser.LineInfo;
import main.java.parser.Lined;
import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing an compilation error.
 * <p>
 *     This is similar to {@link main.java.parser.ParserException}, but is used
 *     for errors in the compiler as opposed to the parser. Any error that
 *     occurs due to errors in the code being compiled should use this class.
 *     Errors in the compiler itself should either use assert statements, for
 *     where there is no reasonable {@link LineInfo} associated with the error,
 *     or a {@link main.java.parser.ParserInternalError ParserInternalError}
 *     where there is a piece of code that caused the problem.
 * </p>
 * <p>
 *     It is not possible to construct this exception publicly, but instead one
 *     of the associated static methods must be used. These will create
 *     consistent error messages with information related to the lines on which
 *     the error occurred.
 * </p>
 *
 * @author Patrick Norton
 * @see main.java.parser.ParserException
 */
public class CompilerException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "%s%nError: File %s Line %d%n%s";

    private CompilerException(String s) {
        super(s);
    }

    /**
     * Creates a CompilerException with the specified custom message and
     * {@link Lined} value from which to extract a {@link LineInfo}.
     *
     * @param message The message to use
     * @param info The {@link Lined} object to use
     * @return The CompilerException
     * @see #of(String, LineInfo)
     */
    @NotNull
    public static CompilerException of(String message, @NotNull Lined info) {
        return of(message, info.getLineInfo());
    }

    /**
     * Creates a CompilerException with the specified custom message and
     * {@link LineInfo}.
     * <p>
     *     The default message contains info on the file name, line number, and
     *     content of the line.
     * </p>
     *
     * @param message The message to use
     * @param lineInfo The {@link LineInfo} representing the line on which the
     *                 error occurred
     * @return The exception
     */
    @NotNull
    @Contract("_, _ -> new")
    public static CompilerException of(String message, @NotNull LineInfo lineInfo) {
        return new CompilerException(
                String.format(DEFAULT_MESSAGE, message, lineInfo.getPath(),
                        lineInfo.getLineNumber(), lineInfo.infoString())
        );
    }

    /**
     * Create a CompilerException with a formatted error message.
     * <p>
     *     Equivalent to {@code CompilerException.of(String.format(message,
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
    @NotNull
    public static CompilerException format(String message, @NotNull Lined lineInfo, Object... args) {
        return format(message, lineInfo.getLineInfo(), args);
    }

    /**
     * Create a CompilerException with a formatted error message.
     * <p>
     *     Equivalent to {@code CompilerException.of(String.format(message,
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
    @NotNull
    public static CompilerException format(String message, LineInfo lineInfo, Object... args) {
        return of(String.format(message, args), lineInfo);
    }

    /**
     * Create a CompilerException with a error message specific to a
     * double-definition.
     *
     * @param name The name which was illegally double-defined
     * @param info1 The {@link LineInfo} for the first definition
     * @param info2 The {@link LineInfo} for the illegal definition
     * @return The exception
     */
    @NotNull
    public static CompilerException doubleDef(String name, @NotNull LineInfo info1, @NotNull LineInfo info2) {
        return new CompilerException(
                String.format(
                        "Error: name '%s' defined twice:%n" +
                                "Definition 1: File %s Line %d%n%s%n" +
                                "Definition 2: File %s Line %d%n%s%n",
                        name,
                        info1.getPath(), info1.getLineNumber(), info1.infoString(),
                        info2.getPath(), info2.getLineNumber(), info2.infoString()
                )
        );
    }

    /**
     * Create a CompilerException with a error message specific to a
     * double-definition of an operator.
     *
     * @param op The operator which was illegally double-defined
     * @param info1 The {@link LineInfo} for the first definition
     * @param info2 The {@link LineInfo} for the illegal definition
     * @return The exception
     */
    @NotNull
    public static CompilerException doubleDef(OpSpTypeNode op, @NotNull LineInfo info1, @NotNull LineInfo info2) {
        return new CompilerException(
                String.format(
                        "Error: '%s' defined twice:%n" +
                                "Definition 1: File %s Line %d%n%s%n" +
                                "Definition 2: File %s Line %d%n%s%n",
                        op,
                        info1.getPath(), info1.getLineNumber(), info1.infoString(),
                        info2.getPath(), info2.getLineNumber(), info2.infoString()
                )
        );
    }

    /**
     * Create a CompilerException with a error message specific to a
     * double-definition.
     *
     * @param name The name which was illegally double-defined
     * @param info1 The {@link Lined} object for the first definition
     * @param info2 The {@link Lined} object for the illegal definition
     * @return The exception
     */
    @NotNull
    public static CompilerException doubleDef(String name, @NotNull Lined info1, @NotNull Lined info2) {
        return doubleDef(name, info1.getLineInfo(), info2.getLineInfo());
    }

    /**
     * Create a CompilerException with a error message specific to a
     * double-definition of an operator.
     *
     * @param op The operator which was illegally double-defined
     * @param info1 The {@link Lined} object for the first definition
     * @param info2 The {@link Lined} object for the illegal definition
     * @return The exception
     */
    @NotNull
    public static CompilerException doubleDef(OpSpTypeNode op, @NotNull Lined info1, @NotNull Lined info2) {
        return doubleDef(op, info1.getLineInfo(), info2.getLineInfo());
    }
}
