package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a non-dictionary comprehension
 * @author Patrick Norton
 * @see DictComprehensionNode
 */
public class ComprehensionNode extends ComprehensionLikeNode {

    /**
     * Create a new instance of ComprehensionNode.
     * @param brace_type The type of brace used in the comprehension
     * @param variables The variables being looped over in the loop
     * @param builder What is actually forming the values that go into the
     *                built object
     * @param looped The iterable being looped over
     */
    @Contract(pure = true)
    public ComprehensionNode(LineInfo lineInfo, String brace_type, TypedVariableNode[] variables,
                             ArgumentNode[] builder, TestNode[] looped, TestNode condition, TestNode whileCond) {
        super(lineInfo, brace_type, variables, builder, looped, condition, whileCond);
    }

    /**
     * Parse a new ComprehensionNode from a list of tokens.
     * <p>
     *     The syntax for a comprehension is: <code>OPEN_BRACE {@link TestNode}
     *     "for" *{@link TypedVariableNode} "in" *{@link TestNode} CLOSE_BRACE
     *     </code>.
     * </p>
     * @param tokens The tokens which are operated destructively on to parse
     * @return The newly parsed comprehension
     */
    @NotNull
    @Contract("_ -> new")
    static ComprehensionNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(TokenType.OPEN_BRACE);
        LineInfo info = tokens.lineInfo();
        String brace_type = tokens.tokenSequence();
        String matchingBrace = tokens.matchingBrace();
        tokens.nextToken(true);
        ArgumentNode[] builder = ArgumentNode.parseBraceFreeList(tokens);
        if (!tokens.tokenIs(Keyword.FOR)) {
            throw tokens.error("Invalid start to comprehension");
        }
        tokens.nextToken(true);
        TypedVariableNode[] variables = TypedVariableNode.parseList(tokens, true);
        if (!tokens.tokenIs(Keyword.IN)) {
            throw tokens.error("Comprehension body must have in after variable list");
        }
        tokens.nextToken(true);
        TestNode[] looped = TestNode.parseListNoTernary(tokens, true);
        TestNode condition = TestNode.parseOnToken(tokens, Keyword.IF, true);
        TestNode whileCond = TestNode.parseOnToken(tokens, Keyword.WHILE, true);
        if (!brace_type.isEmpty() && !tokens.tokenIs(matchingBrace)) {
            throw tokens.error("Expected close brace");
        }
        tokens.nextToken();
        return new ComprehensionNode(info, brace_type, variables, builder, looped, condition, whileCond);
    }

    public String toString() {
        return super.toString();
    }
}
