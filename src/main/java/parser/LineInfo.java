package main.java.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class LineInfo {
    private final Path path;
    private final int lineNumber;
    private final String line;
    private final int startingPoint;

    @Contract(pure = true)
    public LineInfo(Path path, int lineNumber, String line, int startingPoint) {
        this.path = path;
        this.lineNumber = lineNumber;
        this.line = line;
        this.startingPoint = startingPoint;
    }

    public Path getPath() {
        return path;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String infoString() {
        int numSpaces = startingPoint + String.valueOf(lineNumber).length() + 2;
        return String.format("%d: %s%n%s^", lineNumber, line, " ".repeat(numSpaces));
    }

    @NotNull
    @Contract(value = " -> new", pure = true)
    public static LineInfo empty() {
        return new LineInfo(Path.of(""), -1, "", 0) {
            @Override
            public String infoString() {
                return "Line info not available";
            }
        };
    }
}
