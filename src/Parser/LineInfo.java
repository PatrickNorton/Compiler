package Parser;

import org.jetbrains.annotations.Contract;

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
}
