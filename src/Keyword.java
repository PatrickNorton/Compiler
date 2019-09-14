import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public enum Keyword {
    CLASS("class", (TokenList tokens) -> {
        if (tokens.tokenIs(1, TokenType.DESCRIPTOR, TokenType.KEYWORD)) {
            return ClassStatementNode.parseDescriptor(tokens);
        }
        return ClassDefinitionNode.parse(tokens);
    }),
    FUNC("func", FunctionDefinitionNode::parse),
    IF("if", IfStatementNode::parse),
    FOR("for", ForStatementNode::parse),
    ELIF("elif", " must have a preceding if", TokenPlace.LEFT),
    ELSE("else", " must have a preceding if", TokenPlace.LEFT),
    DO("do", DoStatementNode::parse),
    DOTIMES("dotimes", DotimesStatementNode::parse),
    METHOD("method", MethodDefinitionNode::parse),
    WHILE("while", WhileStatementNode::parse),
    CASTED("casted", " must not begin a statement", TokenPlace.LEFT),
    IN("in", " does not begin any statements", TokenPlace.LEFT),
    FROM("from", ImportExportNode::parse),
    IMPORT("import", ImportStatementNode::parse),
    EXPORT("export", ExportStatementNode::parse),
    TYPEGET("typeget", TypegetStatementNode::parse),
    BREAK("break", BreakStatementNode::parse),
    CONTINUE("continue", ContinueStatementNode::parse),
    RETURN("return", ReturnStatementNode::parse),
    PROPERTY("property", PropertyDefinitionNode::parse),
    ENTER("enter", " must be in a property block", TokenPlace.LEFT),
    EXIT("exit", " must be in a property block", TokenPlace.LEFT),
    TRY("try", TryStatementNode::parse),
    EXCEPT("except", " must be in a try-statement", TokenPlace.LEFT),
    FINALLY("finally", " must be in a try statement", TokenPlace.LEFT),
    WITH("with", WithStatementNode::parse),
    AS("as", " must be with a with or import/typeget", TokenPlace.LEFT),
    ASSERT("assert", AssertStatementNode::parse),
    DEL("del", DeleteStatementNode::parse),
    YIELD("yield", YieldStatementNode::parse),
    GET("get", " must be in a property block", TokenPlace.LEFT),
    SET("set", " must be in a property block", TokenPlace.LEFT),
    LAMBDA("lambda", TestNode::parse),
    RAISE("raise", RaiseStatementNode::parse),
    TYPEDEF("typedef", TypedefStatementNode::parse),
    SOME("some", TestNode::parse),
    INTERFACE("interface", InterfaceDefinitionNode::parse),
    SWITCH("switch", SwitchStatementNode::parse),
    CASE("case", "Unexpected ", TokenPlace.RIGHT),
    ENUM("enum", EnumDefinitionNode::parse),
    ;
    public final String name;
    private final Function<TokenList, BaseNode> parseLeft;
    private static final Map<String, Keyword> values;

    @Contract(pure = true)
    Keyword(String name, Function<TokenList, BaseNode> fn) {
        this.name = name;
        this.parseLeft = fn;
    }

    @Contract(pure = true)
    Keyword(String name, String errorMessage, @NotNull TokenPlace place) {
        this.name = name;
        switch (place) {
            case LEFT:
                this.parseLeft = (TokenList tokens) -> {throw new RuntimeException(tokens.getFirst() + errorMessage);};
                break;
            case RIGHT:
                this.parseLeft = (TokenList tokens) -> {throw new RuntimeException(errorMessage + tokens.getFirst());};
                break;
            case NONE:
                this.parseLeft = (TokenList tokens) -> {throw new RuntimeException(errorMessage);};
                break;
            default:
                throw new RuntimeException("Unexpected TokenPlace");
        }
    }

    private enum TokenPlace {
        LEFT,
        RIGHT,
        NONE,
    }

    static {
        Map<String, Keyword> temp = new HashMap<>();
        for (Keyword kw : Keyword.values()) {
            temp.put(kw.name, kw);
        }
        values = Collections.unmodifiableMap(temp);
    }

    @NotNull
    static Keyword find(String value) {
        Keyword val = values.get(value);
        if (val == null) {
            throw new RuntimeException("Unknown keyword");
        } else {
            return val;
        }
    }

    BaseNode parseLeft(TokenList tokens) {
        return this.parseLeft.apply(tokens);
    }
}
