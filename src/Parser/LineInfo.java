package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class LineInfo {
    public final int lineNumber;
    public final String line;
    public final int startingPoint;

    @Contract(pure = true)
    public LineInfo(int lineNumber, String line, int startingPoint) {
        this.lineNumber = lineNumber;
        this.line = line;
        this.startingPoint = startingPoint;
    }

    public String infoString() {
        String header = lineNumber + ":";
        return String.format("%s %s%n%s^", header, line, " ".repeat(startingPoint + header.length()));
    }

    @NotNull
    @Contract(value = " -> new", pure = true)
    public static LineInfo empty() {
        return new LineInfo(0, "", 0) {
            @Override
            public String infoString() {
                return "Line info not available";
            }
        };
    }
}
