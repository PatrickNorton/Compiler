package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class ParserInternalError extends RuntimeException {
    public static final String DEFAULT_HEADER = "INTERNAL ERROR:" + System.lineSeparator();

    public ParserInternalError(String message) {
        super(message);
    }

    @NotNull
    @Contract("_ -> new")
    public static ParserInternalError withHeader(String message) {
        return new ParserInternalError(DEFAULT_HEADER + message);
    }
}
