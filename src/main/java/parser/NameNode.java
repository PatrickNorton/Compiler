package main.java.parser;

/**
 * The interface for any sort of name, such as a function call, index, or
 * variable.
 * @author Patrick Norton
 */
public interface NameNode extends AtomicNode, PostDottableNode, AssignableNode {
    /**
     * Parse a name from a list of tokens.
     * <p>
     *     The syntax for a NameNode is made up of its constituent subclasses.
     *     The list of tokens must begin with a NAME token.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed NameNode
     */

    static NameNode parse(TokenList tokens) {
        return parse(tokens, false);
    }

    /**
     * Parse a name from a list of tokens.
     * <p>
     *     The syntax for a NameNode is made up of its constituent subclasses.
     *     The list of tokens must begin with a NAME token.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @param ignoreNewlines Whether or not to ignore newlines
     * @return The freshly parsed NameNode
     */

    static NameNode parse(TokenList tokens, boolean ignoreNewlines) {
        assert tokens.tokenIs(TokenType.NAME, "(");
        NameNode name;
        if (tokens.tokenIs(TokenType.NAME)) {
            name = VariableNode.parse(tokens);
        } else {
            assert tokens.tokenIs("(");
            tokens.nextToken(true);
            name = parse(tokens, true);
            tokens.expect(")", ignoreNewlines);
        }
        return parsePost(tokens, name, ignoreNewlines);
    }

    static NameNode parsePost(TokenList tokens,NameNode name, boolean ignoreNewlines) {
        NameNode value = parsePostBraces(tokens, name, ignoreNewlines);
        return DottedVariableNode.parsePostDots(tokens, value, ignoreNewlines);
    }

    static NameNode parsePostBraces(TokenList tokens,NameNode name) {
        return parsePostBraces(tokens, name, false);
    }

    static NameNode parsePostBraces(TokenList tokens,NameNode name, boolean ignoreNewlines) {
        TestNode newName = TestNode.parsePostBraces(tokens, name, ignoreNewlines);
        if (newName instanceof NameNode) {
            return (NameNode) newName;
        } else {
            throw tokens.internalError("Error in post-brace parsing");
        }
    }

    static String parenthesize(TestNode stmt) {
        return stmt instanceof NameNode ? stmt.toString() : "(" + stmt + ")";
    }
}
