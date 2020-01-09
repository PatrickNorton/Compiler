package main.java.converter;

import main.java.parser.BaseNode;
import main.java.parser.LineInfo;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

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

    @NotNull
    public static CompilerInternalError of(String message, @NotNull LineInfo info) {
        return withHeader(String.format(DEFAULT_MESSAGE, message, info.getPath(), info.getLineNumber(), info.infoString()));
    }

    @NotNull
    public static CompilerInternalError of(String message, @NotNull BaseNode node) {
        return of(message, node.getLineInfo());
    }
}
