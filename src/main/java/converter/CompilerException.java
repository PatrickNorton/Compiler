package main.java.converter;

import main.java.parser.BaseNode;
import main.java.parser.LineInfo;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class CompilerException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "%s%nError: File %s Line %d%n%s";

    private CompilerException(String s) {
        super(s);
    }

    @NotNull
    public static CompilerException of(String message, @NotNull BaseNode info) {
        return of(message, info.getLineInfo());
    }

    @NotNull
    @Contract("_, _ -> new")
    public static CompilerException of(String message, @NotNull LineInfo lineInfo) {
        return new CompilerException(
                String.format(DEFAULT_MESSAGE, message, lineInfo.getPath(),
                        lineInfo.getLineNumber(), lineInfo.infoString())
        );
    }

    @NotNull
    public static CompilerException format(String message, @NotNull BaseNode lineInfo, Object... args) {
        return format(message, lineInfo.getLineInfo(), args);
    }

    @NotNull
    public static CompilerException format(String message, LineInfo lineInfo, Object... args) {
        return of(String.format(message, args), lineInfo);
    }
}
