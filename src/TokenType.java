import org.intellij.lang.annotations.Language;

import java.util.regex.Pattern;

public enum TokenType {
    // Whitespace. Matches comments, spaces, and escaped newlines
    WHITESPACE("^(#\\|((?!\\|#).|\n)*\\|#|#.*|[\t ]+|\\\\\n)"),
    // Matches when input is empty
    EPSILON("^\\z"),
    // Matches newlines of all types
    NEWLINE("^\\R"),
    // Matches list of descriptor words (const, private, etc.)
    DESCRIPTOR("^\\b(private|const|final|pubget|static|generator)\\b"),
    // Matches keywords (if, else, class, do, etc.)
    KEYWORD("^\\b(if|for|else|elif|do|func|class|method|while|in|from|(im|ex)port"
            +"|typeget|dotimes|break|continue|return|context|get|set|lambda"
            +"|property|enter|exit|try|except|finally|with|as|assert|del|yield"
            +"|raise|typedef|some|interface|casted)\\b"),
    // Matches open braces
    OPEN_BRACE("^[\\[({]"),
    // Matches close braces
    CLOSE_BRACE("^[])}]"),
    // The comma in between list items
    COMMA("^,"),
    // Augmented assignment, e.g. +=
    // Parsed separately from standard operators because of their different use
    AUG_ASSIGN("^([+\\-%]|([*/])\\2?|<<|>>|[&|^~])="),
    // The magical arrow unicorn
    ARROW("^->"),
    // Standard, boring operators, + - **
    OPERATOR("^(==|!=|[><]=?|([+\\-*/])\\2?|<<|>>|[&|^~%])"),
    // Assignment, and dynamic assignment (:=)
    ASSIGN("^:?="),
    // String literals, including f-strings
    STRING("^([efb]*\"([^\"]|\\\\\"|\n)+(?<!\\\\)(\\\\{2})*\")"),
    // Boolean operators
    BOOL_OP("^\\b(and|or|not|xor)\\b"),
    // Numbers, from 123 to 0xab4f6.245
    NUMBER("^(0x[0-9a-f]+(\\.[0-9a-f]+)?|(0[ob])?[0-9]+(\\.[0-9]+)?)\\b"),
    // That special operator definition syntax
    OPERATOR_SP("^\\b(operator *(r?(==|!=|([+\\-*/])\\4?|[><]=?|<<|>>|[&|^%])"
            + "|\\[]=?|\\(\\)|~|u-|iter|new|in|missing|str|repr|bool|del(\\[])?))"),
    NAME("^\\b[_a-zA-Z][_a-zA-Z0-9]*\\b"),
    // operator functions, like \+
    OP_FUNC("^\\\\(==|!=|[><]=?|r?([+\\-*/])\\2?|u-|<<|>>|[&|^~%])"),
    // Colons, for slicing syntax etc.
    COLON("^:"),
    // The ellipsis unicorn
    ELLIPSIS("^\\.{3}"),
    // Dots, when they don't match ellipsis
    DOT("^\\.");

    final Pattern regex;

    TokenType(@Language("RegExp") String regex) {
        this.regex = Pattern.compile(regex);
    }
}
