package main.java.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * The class representing the body of a class.
 *
 * @author Patrick Norton
 * @see StatementBodyNode
 */
public class ClassBodyNode implements BodyNode, Iterable<ClassStatementNode> {
    private LineInfo lineInfo;
    private ClassStatementNode[] statements;

    @Contract(pure = true)
    public ClassBodyNode() {
        this(LineInfo.empty());
    }

    @Contract(pure = true)
    public ClassBodyNode(LineInfo lineInfo, ClassStatementNode... statements) {
        this.lineInfo = lineInfo;
        this.statements = statements;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    @Override
    public ClassStatementNode[] getStatements() {
        return statements;
    }

    @Override
    public boolean isEmpty() {
        return statements.length == 0;
    }

    @Override
    public ClassStatementNode get(int i) {
        return statements[i];
    }

    @NotNull
    @Override
    public Iterator<ClassStatementNode> iterator() {
        return Arrays.asList(statements).iterator();
    }

    /**
     * Parse ClassBodyNode from list of tokens.
     * <p>
     *     A class body is simply a list of class statements, e.g. statements
     *     which can have a declaration in front of them. The statement must
     *     also be surrounded by curly braces, not doing so will result in a
     *     ParserException being raised. The syntax of a ClassStatementNode is
     *     <code> "{" *{@link ClassStatementNode} "}" NEWLINE</code>.
     * </p>
     * @param tokens The list of tokens passed
     * @return The parsed ClassBodyNode
     */
    @NotNull
    @Contract("_ -> new")
    static ClassBodyNode parse(@NotNull TokenList tokens) {
        // FIXME: Allow lack of newlines around braces
        if (!tokens.tokenIs("{")) {
            throw tokens.error("The body of a class must be enclosed in curly brackets");
        }
        LineInfo info = tokens.lineInfo();
        tokens.nextToken(true);
        ClassBodyNode cb = parseUntilToken(info, tokens, "}");
        assert tokens.tokenIs("}");
        tokens.nextToken();
        return cb;
    }

    /**
     * Parse the ClassBodyNode of an enum from a list of tokens.
     * <p>
     *     This is functionally identical to the normal parse method, except
     *     there is no check for the opening brace, as that will have already
     *     been parsed before the constants were enumerated.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed ClassBodyNode
     */
    @NotNull
    static ClassBodyNode parseEnum(@NotNull TokenList tokens) {
        ClassBodyNode cb = parseUntilToken(tokens, "}");
        assert tokens.tokenIs("}");
        tokens.nextToken();
        return cb;
    }

    @NotNull
    private static ClassBodyNode parseUntilToken(@NotNull TokenList tokens, String... sentinels) {
        return parseUntilToken(tokens.lineInfo(), tokens, sentinels);
    }

    @NotNull
    @Contract("_, _, _ -> new")
    private static ClassBodyNode parseUntilToken(LineInfo lineInfo, @NotNull TokenList tokens, String... sentinels) {
        List<ClassStatementNode> statements = new ArrayList<>();
        while (!tokens.tokenIs(sentinels)) {
            statements.add(ClassStatementNode.parse(tokens));
            if (!tokens.tokenIs(sentinels)) {
                tokens.Newline();
            }
        }
        return new ClassBodyNode(lineInfo, statements.toArray(new ClassStatementNode[0]));
    }

    @NotNull
    @Contract("_ -> new")
    static ClassBodyNode fromList(@NotNull List<ClassStatementNode> statements) {
        if (statements.isEmpty()) {
            return new ClassBodyNode();
        }
        return new ClassBodyNode(statements.get(0).getLineInfo(), statements.toArray(new ClassStatementNode[0]));
    }

    @Override
    public String toString() {
        return isEmpty() ? "{}" : "{...}";
    }
}
