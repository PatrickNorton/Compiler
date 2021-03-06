package main.java.converter;

import main.java.parser.BaseNode;
import main.java.parser.LineInfo;
import org.jetbrains.annotations.NotNull;

public final class CompilerWarning {
    private CompilerWarning() {}

    public static void warn(String message, @NotNull LineInfo info) {
        System.err.printf("Warning - file %s, line %d: %s%n%s%n",
                info.getPath(), info.getLineNumber(), message, info.infoString());
    }

    public static void warn(String message, @NotNull BaseNode node) {
        warn(message, node.getLineInfo());
    }

    public static void warnf(String message, @NotNull LineInfo info, Object... args) {
        warn(String.format(message, args), info);
    }

    public static void warnf(String message, @NotNull BaseNode node, Object... args) {
        warn(String.format(message, args), node.getLineInfo());
    }
}
