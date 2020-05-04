package main.java.converter;

import main.java.parser.BaseNode;
import main.java.parser.LineInfo;
import main.java.parser.Lined;
import main.java.parser.OpSpTypeNode;
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

    @NotNull
    public static CompilerException doubleDef(String name, @NotNull Lined info1, @NotNull Lined info2) {
        return doubleDef(name, info1.getLineInfo(), info2.getLineInfo());
    }

    @NotNull
    public static CompilerException doubleDef(OpSpTypeNode op, @NotNull Lined info1, @NotNull Lined info2) {
        return doubleDef(op, info1.getLineInfo(), info2.getLineInfo());
    }
}
