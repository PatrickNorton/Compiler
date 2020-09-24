package main.java.converter;

import main.java.parser.LineInfo;
import main.java.parser.Lined;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

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

    @Contract("_, _ -> new")
    @NotNull
    public static CompilerTodoError of(String message, @NotNull LineInfo lineInfo) {
        return new CompilerTodoError(
                String.format(
                        "%s%nError: Operation not yet implemented%nFile %s Line%d%n%s",
                        message, lineInfo.getPath(),
                        lineInfo.getLineNumber(), lineInfo.infoString()
                )
        );
    }

    @NotNull
    public static CompilerTodoError of(String message, @NotNull Lined lineInfo) {
        return of(message, lineInfo.getLineInfo());
    }
}
