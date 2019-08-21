import org.intellij.lang.annotations.Language;

import java.util.regex.Pattern;

public enum TokenType {
    // TODO? Turn dot into its own token
    // Whitespace. Matches comments, spaces, and escaped newlines
    WHITESPACE("^(#\\|((?!\\|#).|\n)*\\|#|#.*| +|\\\\\n)"),
    // Matches when input is empty
    EPSILON("^\\z"),
    // Matches newlines of all types
    NEWLINE("^\\R"),
    // Matches list of descriptor words (const, private, etc.)
    DESCRIPTOR("^\\b(private|const|final|pubget|static|generator)\\b"),
    // Matches keywords (if, else, class, do, etc.)
    KEYWORD("^\\b(if|for|else|do|func|class|method|while|in|from|(im|ex)port"
            +"|typeget|dotimes|break|continue|return|context|get|set|lambda"
            +"|property|enter|exit|try|except|finally|with|as|assert|del|yield"
            +"|raise|typedef|some|interface|cast(ed)?|to)\\b"),
    // Matches self.foo() or cls.foo()
    // TODO? Remove this and just have as standard keywords
    SELF_CLS("^\\b(self|cls)(\\.[_a-zA-Z][_a-zA-Z0-9.]*)?\\b"),
    // Matches open braces
    OPEN_BRACE("^[\\[({]"),
    // Matches close braces
    CLOSE_BRACE("^[])}]"),
    // The comma in between list items
    COMMA("^,"),
    // Augmented assignment, e.g. +=
    // Parsed separately from standard operators because of their different use
    AUG_ASSIGN("^([+\\-%]|([*/])\\2?|<<|>>|[&|^~])="),
    // Standard, boring operators, + - **
    OPERATOR("^(->|==|!=|[><]=?|([+\\-*/])\\2?|<<|>>|[&|^~%])"),
    // Assignment, and dynamic assignment (:=)
    ASSIGN("^:?="),
    // String literals, including f-strings
    STRING("^[rfb]*\"([^\"]|\\\\\"|\n)+(?<!\\\\)\""),
    // Boolean operators
    BOOL_OP("^\\b(and|or|not|xor)\\b"),
    // Numbers, from 123 to 0xab4f6.245
    NUMBER("^(0x[0-9a-f]+(\\.[0-9a-f]+)?|(0[ob])?[0-9]+(\\.[0-9]+)?)\\b"),
    // That special operator definition syntax
    OPERATOR_SP("^\\b(operator *(r?(==|!=|([+\\-*/])\\4?|[><]=?)|\\[]=?|\\(\\)"
            + "|u-|iter|new|in|missing|del|str|repr|bool|del(\\[])?|<<|>>|[&|^~%]))"),
    // Dotted variables (e.g. [abc()].foo())
    DOTTED_VAR("^(?!operator\\b)\\.[_a-zA-Z][_a-zA-Z0-9]*(\\.[_a-zA-Z][_a-zA-Z0-9]*)*\\b"),
    // Regular variables
    VARIABLE("^(?!operator\\b)[_a-zA-Z][_a-zA-Z0-9]*(\\.[_a-zA-Z][_a-zA-Z0-9]*)*\\b"),
    // operator functions, like \+
    OP_FUNC("^\\\\(==|!=|[><]=?|r?([+\\-*/])\\2?|u-|<<|>>|[&|^~%])"),
    // Colons, for slicing syntax etc.
    COLON("^:"),
    // The ellipsis unicorn
    ELLIPSIS("^\\.{3}");

    final Pattern regex;

    TokenType(@Language("RegExp") String regex) {
        this.regex = Pattern.compile(regex);
    }
}
