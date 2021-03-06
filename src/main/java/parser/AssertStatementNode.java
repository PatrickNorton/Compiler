package main.java.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Node representing an assert statement, such as the ones in Java or any other
 * major programming language.
 *
 * @author Patrick Norton
 */
public class AssertStatementNode implements SimpleStatementNode {
    /**
     * The assertion to be tested
     */
    private TestNode assertion;
    private LineInfo lineInfo;

    @Contract(pure = true)
    public AssertStatementNode(LineInfo lineInfo, TestNode assertion) {
        this.assertion = assertion;
        this.lineInfo = lineInfo;
    }

    /**
     * Create new instance of AssertStatementNode.
     * @param assertion The assertion to be tested
     */
    @Contract(pure = true)
    public AssertStatementNode(TestNode assertion) {
        this(assertion.getLineInfo(), assertion);
    }

    public TestNode getAssertion() {
        return assertion;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    /**
     * Parse AssertStatementNode from a list of tokens. This will use the
     * definition of an assertion in order to create a new node, taking up the
     * stream as it goes
     * @param tokens The list of tokens to be parsed. This method does remove
     *               all parsed tokens as part of the parsing
     * @return The new AbstractStatementNode which is created
     */
    @NotNull
    @Contract("_ -> new")
    static AssertStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.ASSERT);
        tokens.nextToken();
        TestNode assertion = TestNode.parse(tokens);
        return new AssertStatementNode(assertion);
    }

    @Override
    public String toString() {
        return "assert " + assertion;
    }
}
