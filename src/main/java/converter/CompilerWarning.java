package main.java.converter;

import main.java.parser.LineInfo;
import org.jetbrains.annotations.NotNull;

public final class CompilerWarning {
    private CompilerWarning() {}

    public static void warn(String message, @NotNull LineInfo info) {
        System.out.printf("Warning - file %s, line %d: %s%n%s%n",
                info.getPath(), info.getLineNumber(), message, info.infoString());
    }
}
