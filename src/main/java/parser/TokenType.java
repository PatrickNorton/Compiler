package main.java.parser;

import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The possible types for a token to be.
 */
public enum TokenType {
    /**
     * Whitespace. Matches comments, spaces, and escaped newlines.
     * Should not make it past the tokenizer.
     */
    WHITESPACE(TokenType::whitespace),
    /**
     * End of the file.
     */
    EPSILON(TokenType::epsilon),
    /**
     * Newlines.
     */
    NEWLINE(TokenType::newline),
    /**
     * Descriptor words, such as public or static.
     */
    DESCRIPTOR(DescriptorNode::pattern),
    /**
     * Language-reserved keywords, like if, try, or enum.
     */
    KEYWORD(Keyword::pattern),
    /**
     * Open braces. Each token this matches corresponds with an {@link
     * #CLOSE_BRACE closing brace}.
     */
    OPEN_BRACE(TokenType::openBrace),
    /**
     * Closing braces. Each token this matches corresponds with an {@link
     * #OPEN_BRACE open brace}.
     */
    CLOSE_BRACE(TokenType::closeBrace),
    /**
     * The comma, such as in between items in a list.
     */
    COMMA(TokenType::comma),
    /**
     * Augmented assignment operators, such as += or -=.
     */
    AUG_ASSIGN(AugAssignTypeNode::pattern),
    /**
     * The magical arrow unicorn, for function return types.
     */
    ARROW(TokenType::arrow),
    /**
     * The even more magical double arrow bi-corn.
     */
    DOUBLE_ARROW(TokenType::doubleArrow),
    /**
     * The ellipsis unicorn.
     */
    ELLIPSIS(TokenType::ellipsis),
    /**
     * Dots that aren't an ellipsis.
     */
    DOT(TokenType::dot),
    /**
     * For increment and decrement operations.
     */
    INCREMENT(TokenType::increment),
    /**
     * Bog-standard operators, like + or <<
     */
    OPERATOR(OperatorTypeNode::pattern),
    /**
     * Assignment, both static and dynamic (:=).
     */
    ASSIGN(TokenType::assign),
    /**
     * String literals of all sorts.
     */
    STRING(TokenType::string),
    /**
     * Numbers in all bases and decimals.
     */
    NUMBER(TokenType::number),
    /**
     * Special operator names, for operator overload definitions.
     */
    OPERATOR_SP(OpSpTypeNode::pattern),
    /**
     * Variable names.
     */
    NAME(TokenType::name),
    /**
     * Backslash-preceded operator functions, such as \+ or \<<.
     */
    OP_FUNC(OpFuncTypeNode::pattern),
    /**
     * Colons, for slices.
     */
    COLON(TokenType::colon),
    /**
     * The at symbol. for decorators.
     */
    AT(TokenType::at),
    /**
     * The dollar sign, for annotations.
     */
    DOLLAR(TokenType::dollar),
    ;

    public static final Set<TokenType> BRACE_IS_LITERAL = Collections.unmodifiableSet(
            EnumSet.of(OPEN_BRACE, NEWLINE, KEYWORD, COMMA, OPERATOR, COLON, AT, DOLLAR, ASSIGN)
    );

    private Pattern regex;
    private final Callable<Pattern> regexMaker;
    private final Function<String, Optional<Integer>> matcher;

    TokenType(@NotNull @Language("RegExp") String regex) {
        assert regex.startsWith("^");  // Make sure regex will only match the beginning of strings
        this.regex = Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS);
        this.regexMaker = null;
        this.matcher = null;
    }

    TokenType(Function<String, Optional<Integer>> matcher) {
        this.regexMaker = null;
        this.regex = null;
        this.matcher = matcher;
    }

    @NotNull Matcher matcher(CharSequence input) {
        if (this.regex == null) {
            try {
                this.regex = regexMaker.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            assert regex.toString().startsWith("^");
        }
        return regex.matcher(input);
    }

    @NotNull Optional<Integer> matches(String input) {
        return this.matcher.apply(input);
    }

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("^(#\\|(.|\\R)*?\\|#|#.*|[\t ]+|\\\\\\R)");

    private static Optional<Integer> whitespace(String input) {
        var matcher = WHITESPACE_PATTERN.matcher(input);
        if (matcher.find()) {
            assert matcher.start() == 0;
            return Optional.of(matcher.end());
        } else {
            return Optional.empty();
        }
    }

    private static Optional<Integer> epsilon(String input) {
        if (input.isEmpty()) {
            return Optional.of(0);
        } else {
            return Optional.empty();
        }
    }

    private static Optional<Integer> newline(String input) {
        // Newlines according to the Java Regex spec
        if (input.startsWith("\r\n")) {
            return Optional.of(2);
        } else {
            switch (input.charAt(0)) {
                case '\n':
                case '\u000b':
                case '\f':
                case '\r':
                case '\u0085':
                case '\u2028':
                case '\u2029':
                    return Optional.of(1);
                default:
                    return Optional.empty();
            }
        }
    }

    private static Optional<Integer> openBrace(String input) {
        switch (input.charAt(0)) {
            case '(':
            case '[':
            case '{':
                return Optional.of(1);
            default:
                return Optional.empty();
        }
    }

    private static Optional<Integer> closeBrace(String input) {
        switch (input.charAt(0)) {
            case ')':
            case ']':
            case '}':
                return Optional.of(1);
            default:
                return Optional.empty();
        }
    }

    private static Optional<Integer> comma(String input) {
        if (input.charAt(0) == ',') {
            return Optional.of(1);
        } else {
            return Optional.empty();
        }
    }

    private static Optional<Integer> arrow(String input) {
        if (input.startsWith("->")) {
            return Optional.of(2);
        } else {
            return Optional.empty();
        }
    }

    private static Optional<Integer> doubleArrow(String input) {
        if (input.startsWith("=>")) {
            return Optional.of(2);
        } else {
            return Optional.empty();
        }
    }

    private static Optional<Integer> ellipsis(String input) {
        if (input.startsWith("...")) {
            return Optional.of(3);
        } else {
            return Optional.empty();
        }
    }

    private static Optional<Integer> dot(String input) {
        if (input.startsWith(".")) {
            return Optional.of(1);
        } else if (input.startsWith("?.")) {
            return Optional.of(2);
        } else if (input.startsWith("!!.")) {
            return Optional.of(3);
        } else {
            return Optional.empty();
        }
    }

    private static Optional<Integer> increment(String input) {
        if (input.startsWith("++")) {
            return Optional.of(2);
        } else if (input.startsWith("--")) {
            return Optional.of(2);
        } else {
            return Optional.empty();
        }
    }

    private static Optional<Integer> assign(String input) {
        if (input.startsWith("=")) {
            return Optional.of(1);
        } else if (input.startsWith("?=")) {
            return Optional.of(2);
        } else {
            return Optional.empty();
        }
    }

    private static final Set<Character> STRING_PREFIXES = Set.of('r', 'e', 'f', 'b', 'c', 'y');

    private static Optional<Integer> string(String input) {
        int cursor = 0;
        while (STRING_PREFIXES.contains(input.charAt(cursor))) {
            cursor++;
            if (cursor >= input.length()) {
                return Optional.empty();
            }
        }
        if (input.charAt(cursor) != '"' && input.charAt(cursor) != '\'') {
            return Optional.empty();
        }
        char terminator = input.charAt(cursor);
        cursor++;
        int backslashCount = 0;
        while (cursor < input.length()) {
            switch (input.charAt(cursor)) {
                case '"':
                    if (terminator == '"' && backslashCount % 2 == 0) {
                        return Optional.of(cursor + 1);
                    } else {
                        cursor++;
                    }
                    backslashCount = 0;
                    break;
                case '\'':
                    if (terminator == '\'' && backslashCount % 2 == 0) {
                        return Optional.of(cursor + 1);
                    } else {
                        cursor++;
                    }
                    backslashCount = 0;
                    break;
                case '\\':
                    backslashCount++;
                    cursor++;
                    break;
                default:
                    cursor++;
                    backslashCount = 0;
                    break;
            }
        }
        return Optional.empty();
    }

    private static final Set<Character> HEX_DIGITS = Set.of(
            '_', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F', 'a', 'b', 'c', 'd', 'e', 'f'
    );

    private static final Set<Character> DEC_DIGITS = Set.of(
            '_', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
    );

    private static Optional<Integer> number(String input) {
        if (input.startsWith("0x")) {
            return numberOf(input, "0x", HEX_DIGITS);
        } else if (input.startsWith("0o")) {
            return numberOf(input, "0o", DEC_DIGITS);
        } else if (input.startsWith("0b")) {
            return numberOf(input, "0b", DEC_DIGITS);
        } else {
            return numberOf(input, "", DEC_DIGITS);
        }
    }

    private static Optional<Integer> numberOf(String input, String prefix, Set<Character> digits) {
        assert input.startsWith(prefix);
        int cursor = prefix.length();
        if (input.length() == prefix.length()) {
            return Optional.empty();
        }
        if (input.charAt(cursor) == '_') {
            return Optional.empty();
        }
        while (digits.contains(input.charAt(cursor))) {
            cursor++;
            if (cursor >= input.length()) {
                return Optional.of(cursor);
            }
        }
        if (input.charAt(cursor) == '.') {
            cursor++;
            if (!digits.contains(input.charAt(cursor))) {
                return Optional.empty();
            }
            cursor++;
            while (digits.contains(input.charAt(cursor))) {
                cursor++;
                if (cursor >= input.length()) {
                    return Optional.of(cursor);
                }
            }
        }
        return cursor == prefix.length() ? Optional.empty() : Optional.of(cursor);
    }

    private static Optional<Integer> name(String input) {
        if (!Character.isUnicodeIdentifierStart(input.codePointAt(0)) && input.codePointAt(0) != '_') {
            return Optional.empty();
        } else {
            var count = input.codePoints().takeWhile(Character::isUnicodeIdentifierPart)
                    .map(Character::charCount).sum();
            if (count == "operator".length() && input.startsWith("operator")) {
                return Optional.empty();
            } else if (count > 0) {
                return Optional.of(count);
            } else {
                return Optional.empty();
            }
        }
    }

    private static Optional<Integer> colon(String input) {
        if (input.charAt(0) == ':') {
            return Optional.of(1);
        } else {
            return Optional.empty();
        }
    }

    private static Optional<Integer> at(String input) {
        if (input.charAt(0) == '@') {
            return Optional.of(1);
        } else {
            return Optional.empty();
        }
    }

    private static Optional<Integer> dollar(String input) {
        if (input.charAt(0) == '$') {
            return Optional.of(1);
        } else {
            return Optional.empty();
        }
    }
}
