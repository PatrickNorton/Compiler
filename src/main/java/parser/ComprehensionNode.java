package main.java.parser;

import main.java.util.Pair;

/**
 * The class representing a non-dictionary comprehension
 * @author Patrick Norton
 * @see DictComprehensionNode
 */
public class ComprehensionNode extends ComprehensionLikeNode {

    /**
     * Create a new instance of ComprehensionNode.
     * @param braceType The type of brace used in the comprehension
     * @param variables The variables being looped over in the loop
     * @param builder What is actually forming the values that go into the
     *                built object
     * @param looped The iterable being looped over
     */

    public ComprehensionNode(LineInfo lineInfo, String braceType, VarLikeNode[] variables,
                             ArgumentNode[] builder, TestListNode looped, TestNode condition, TestNode whileCond) {
        super(lineInfo, braceType, variables, builder, looped, condition, whileCond);
    }

    /**
     * Parse a new ComprehensionNode from a list of tokens.
     *
     * <p>
     *     The syntax for a comprehension is: <code>OPEN_BRACE {@link TestNode}
     *     *("," {@link TestNode}) [","] "for" *{@link VarLikeNode} "in"
     *     *{@link TestNode} ["if" {@link TestNode}] ["while" {@link TestNode}]
     *     CLOSE_BRACE</code>.
     * </p>
     * @param tokens The tokens which are operated destructively on to parse
     * @return The newly parsed comprehension
     */

    static ComprehensionNode parse(TokenList tokens) {
        assert tokens.tokenIs(TokenType.OPEN_BRACE);
        LineInfo info = tokens.lineInfo();
        String braceType = tokens.tokenSequence();
        String matchingBrace = tokens.matchingBrace();
        tokens.nextToken(true);
        ArgumentNode[] builder = ArgumentNode.parseBraceFreeList(tokens);
        if (!tokens.tokenIs(Keyword.FOR)) {
            throw tokens.error("Invalid start to comprehension");
        }
        tokens.nextToken(true);
        VarLikeNode[] variables = VarLikeNode.parseList(tokens, true);
        if (!tokens.tokenIs(Keyword.IN)) {
            throw tokens.error("Comprehension body must have in after variable list");
        }
        tokens.nextToken(true);
        Pair<TestListNode, TestNode> loopedAndCondition = TestListNode.parsePostIf(tokens, true);
        TestListNode looped = loopedAndCondition.getKey();
        TestNode condition = loopedAndCondition.getValue();
        TestNode whileCond = TestNode.parseOnToken(tokens, Keyword.WHILE, true);
        if (!braceType.isEmpty() && !tokens.tokenIs(matchingBrace)) {
            throw tokens.tokenIs(TokenType.CLOSE_BRACE)
                    ? tokens.errorf("Unmatched brace: %s does not match %s", matchingBrace, tokens.getFirst())
                    : tokens.errorExpected("close brace");
        }
        tokens.nextToken();
        return new ComprehensionNode(info, braceType, variables, builder, looped, condition, whileCond);
    }

}
