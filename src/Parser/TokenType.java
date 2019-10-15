package Parser;

import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

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
    DESCRIPTOR("^\\b(public|private|const|final|pubget|static|generator)\\b"),
    /**
     * Language-reserved keywords, like if, try, or enum.
     */
    KEYWORD("^\\b(if|for|else|elif|do|func|class|method|while|in|from|(im|ex)port"
            +"|typeget|dotimes|break|continue|return|context|get|set|lambda"
            +"|property|enter|exit|try|except|finally|with|as|assert|del|yield"
            +"|raise|typedef|some|interface|casted|switch|case|enum|default|goto|defer"
            +"|var|inline)\\b"),
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
    AUG_ASSIGN("^([+\\-%]|([*/])\\2?|<<|>>|[&|^~])="),
    /**
     * The magical arrow unicorn, for function return types.
     */
    ARROW("^->"),
    /**
     * The even more magical double arrow bi-corn.
     */
    DOUBLE_ARROW("^=>"),
    /**
     * Bog-standard operators, like + or <<
     */
    OPERATOR("^(==|!=|<<|>>|[><]=?|([+\\-*/])\\2?|[&|^~%]|\\bis( +not)?\\b|\\bnot +in\\b)"),
    /**
     * Assignment, both static and dynamic (:=).
     */
    ASSIGN("^:?="),
    /**
     * String literals of all sorts.
     */
    STRING("^([refb]*([\"'])(.|\\R)*?(?<!\\\\)(\\\\{2})*\\2)"),
    // TODO? Merge with #OPERATOR
    /**
     * Boolean operators.
     */
    BOOL_OP("^\\b(and|or|not|xor)\\b"),
    /**
     * Numbers in all bases and decimals.
     */
    NUMBER("^(0x[0-9a-f]+(\\.[0-9a-f]+)?|(0[ob])?[0-9]+(\\.[0-9]+)?)\\b"),
    /**
     * Special operator names, for operator overload definitions.
     */
    OPERATOR_SP("^\\b(operator\\b *(r?(==|!=|([+\\-*/])\\4?|[><]=?|<<|>>|[&|^%])"
            + "|\\[]=?|\\(\\)|~|u-|iter|new|in|missing|str|repr|reversed|bool|del(\\[])?))"),
    /**
     * Variable names.
     */
    NAME("^\\b(?!operator\\b)[_a-zA-Z][_a-zA-Z0-9]*\\b"),
    /**
     * Backslash-preceded operator functions, such as \+ or \<<.
     */
    OP_FUNC("^\\\\(==|!=|[><]=?|r?([+\\-*/])\\2?|u-|<<|>>|[&|^~%])"),
    /**
     * Colons, for slices.
     */
    COLON("^:"),
    /**
     * The ellipsis unicorn.
     */
    ELLIPSIS("^\\.{3}"),
    /**
     * Dots that aren't an ellipsis.
     */
    DOT("^\\."),
    /**
     * The at symbol. for decorators.
     */
    AT("^@"),
    /**
     * The dollar sign, for annotations.
     */
    DOLLAR("^\\$"),
    ;

    final Pattern regex;

    TokenType(@NotNull @Language("RegExp") String regex) {
        assert regex.startsWith("^");  // Make sure regex will only match the beginning of strings
        this.regex = Pattern.compile(regex);
    }
}
