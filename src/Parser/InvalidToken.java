package Parser;

import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public enum InvalidToken {
    EXCLAMATION("^!", "! is invalid"),
    SEMICOLON("^;", "; is not allowed, go use Java or something"),
    OPERATOR("^operator\\b *(\\w+|[\\[(=$@])?", "Invalid operator definition"),
    ;

    public final Pattern regex;
    public final String errorMessage;

    InvalidToken(@NotNull @Language("regexp") String regex, String errorMessage) {
        assert regex.startsWith("^");
        this.regex = Pattern.compile(regex);
        this.errorMessage = errorMessage;
    }
}
