package main.java.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The class representing a keyword.
 *
 * @author Patrick Norton
 */
public enum Keyword {
    CLASS("class", ClassDefinitionNode::parse),
    FUNC("func", FunctionDefinitionNode::parse),
    IF("if", IfStatementNode::parse),
    FOR("for", ForStatementNode::parse),
    ELIF("elif", " must have a preceding if", TokenPlace.LEFT),
    ELSE("else", " must have a preceding if", TokenPlace.LEFT),
    DO("do", DoStatementNode::parse),
    DOTIMES("dotimes", DotimesStatementNode::parse),
    METHOD("method", MethodDefinitionNode::parse),
    WHILE("while", WhileStatementNode::parse),
    IN("in", " does not begin any statements", TokenPlace.LEFT),
    FROM("from", ImportExportNode::parse),
    IMPORT("import", ImportExportNode::parse),
    EXPORT("export", ImportExportNode::parse),
    TYPEGET("typeget", ImportExportNode::parse),
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
    CONTEXT("context", ContextDefinitionNode::parse),
    LAMBDA("lambda", TestNode::parse),
    RAISE("raise", RaiseStatementNode::parse),
    TYPEDEF("typedef", TypedefStatementNode::parse),
    SOME("some", TestNode::parse),
    INTERFACE("interface", InterfaceDefinitionNode::parse),
    SWITCH("switch", SwitchStatementNode::parse),
    CASE("case", "Case statements are illegal outside switch", TokenPlace.NONE),
    ENUM("enum", EnumDefinitionNode::parse),
    DEFAULT("default", "Default statements are illegal outside switch", TokenPlace.NONE),
    GOTO("goto", "This language does not support goto, go use C++", TokenPlace.NONE),
    DEFER("defer", DeferStatementNode::parse),
    VAR("var", IndependentNode::parseVar),
    SYNC("sync", SynchronizedStatementNode::parse),
    GENERIC("generic", GeneralizableNode::parse),
    ;

    private static final Map<String, Keyword> values;
    private static final List<String> SORTED = Arrays.stream(values())
            .map(x -> x.name)
            .sorted(Comparator.comparingInt(String::length).reversed())
            .collect(Collectors.toList());

    public final String name;
    private final Function<TokenList, IndependentNode> parseLeft;

    @Contract(pure = true)
    Keyword(String name, Function<TokenList, IndependentNode> fn) {
        this.name = name;
        this.parseLeft = fn;
    }

    @Contract(pure = true)
    Keyword(String name, String errorMessage, @NotNull TokenPlace place) {
        this.name = name;
        switch (place) {
            case LEFT:
                this.parseLeft = (TokenList tokens) -> {
                    throw tokens.error(tokens.getFirst() + errorMessage);
                };
                break;
            case RIGHT:
                this.parseLeft = (TokenList tokens) -> {
                    throw tokens.error(errorMessage + tokens.getFirst());
                };
                break;
            case NONE:
                this.parseLeft = (TokenList tokens) -> {
                    throw tokens.error(errorMessage);
                };
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

    /**
     * Find a keyword given its string value.
     * @param value The string value of the keyword
     * @return The keyword itself
     */
    @NotNull
    static Keyword find(@NotNull Token value) {
        assert value.is(TokenType.KEYWORD);
        Keyword val = values.get(value.sequence);
        if (val == null) {
            throw ParserInternalError.of("Unknown keyword " + value, value);
        } else {
            return val;
        }
    }

    IndependentNode parseLeft(TokenList tokens) {
        return this.parseLeft.apply(tokens);
    }

    @Contract(pure = true)
    static Optional<Integer> pattern(String input) {
        for (var name : SORTED) {
            if (input.startsWith(name)) {
                if (Character.isUnicodeIdentifierPart(input.charAt(name.length()))) {
                    return Optional.empty();
                } else {
                    return Optional.of(name.length());
                }
            }
        }
        return Optional.empty();
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public String toString() {
        return name.toLowerCase();
    }
}
