package Parser;

import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.Callable;
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
    WHITESPACE("^(#\\|(.|\\R)*?\\|#|#.*|[\t ]+|\\\\\\R)"),
    /**
     * End of the file.
     */
    EPSILON("^\\z"),
    /**
     * Newlines.
     */
    NEWLINE("^\\R"),
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
    OPEN_BRACE("^[\\[({]"),
    /**
     * Closing braces. Each token this matches corresponds with an {@link
     * #OPEN_BRACE open brace}.
     */
    CLOSE_BRACE("^[])}]"),
    /**
     * The comma, such as in between items in a list.
     */
    COMMA("^,"),
    /**
     * Augmented assignment operators, such as += or -=.
     */
    AUG_ASSIGN(AugAssignTypeNode::pattern),
    /**
     * The magical arrow unicorn, for function return types.
     */
    ARROW("^->"),
    /**
     * The even more magical double arrow bi-corn.
     */
    DOUBLE_ARROW("^=>"),
    /**
     * The ellipsis unicorn.
     */
    ELLIPSIS("^\\.{3}"),
    /**
     * Dots that aren't an ellipsis.
     */
    DOT("^\\??\\."),
    /**
     * For increment and decrement operations.
     */
    INCREMENT("^([-+]){2}"),
    /**
     * Bog-standard operators, like + or <<
     */
    OPERATOR(OperatorTypeNode::pattern),
    /**
     * Assignment, both static and dynamic (:=).
     */
    ASSIGN("^:?="),
    /**
     * String literals of all sorts.
     */
    STRING("^([refb]*([\"'])(.|\\R)*?(?<!\\\\)(\\\\{2})*\\2)"),
    /**
     * Numbers in all bases and decimals.
     */
    NUMBER("^(0x[0-9a-f][0-9a-f_]*(\\.[0-9a-f_]+)?|(0[ob])?[0-9][0-9_]*(\\.[0-9_]+)?)\\b"),
    /**
     * Special operator names, for operator overload definitions.
     */
    OPERATOR_SP(OpSpTypeNode::pattern),
    /**
     * Variable names.
     */
    NAME("^\\b(?!operator\\b)(?!0-9)\\w*\\b"),
    /**
     * Backslash-preceded operator functions, such as \+ or \<<.
     */
    OP_FUNC(OpFuncTypeNode::pattern),
    /**
     * Colons, for slices.
     */
    COLON("^:"),
    /**
     * The at symbol. for decorators.
     */
    AT("^@"),
    /**
     * The dollar sign, for annotations.
     */
    DOLLAR("^\\$"),
    ;

    public static final Set<TokenType> BRACE_IS_LITERAL = Collections.unmodifiableSet(
            EnumSet.of(OPEN_BRACE, NEWLINE, KEYWORD, COMMA, OPERATOR, COLON, AT, DOLLAR, ASSIGN)
    );

    private Pattern regex;
    private final Callable<Pattern> regexMaker;

    TokenType(@NotNull @Language("RegExp") String regex) {
        assert regex.startsWith("^");  // Make sure regex will only match the beginning of strings
        this.regex = Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS);
        this.regexMaker = null;
    }

    @Contract(pure = true)
    TokenType(Callable<Pattern> regexMaker) {
        this.regexMaker = regexMaker;
        this.regex = null;
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
}
