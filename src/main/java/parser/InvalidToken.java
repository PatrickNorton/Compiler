package main.java.parser;

import java.util.regex.Pattern;

public enum InvalidToken {
    EXCLAMATION("^!", "! is invalid"),
    SEMICOLON("^;", "; is not allowed, go use Java or something"),
    OPERATOR("^operator\\b *(\\w+|[\\[(=$@])?", "Invalid operator definition"),
    BACKSLASH("^\\\\", "Invalid backslash escape")
    ;

    public final Pattern regex;
    public final String errorMessage;

    InvalidToken(String regex, String errorMessage) {
        assert regex.startsWith("^");
        this.regex = Pattern.compile(regex);
        this.errorMessage = errorMessage;
    }
}
