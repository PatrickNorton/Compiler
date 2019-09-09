import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.StringJoiner;

/**
 * The class representing a non-dictionary comprehension
 * @author Patrick Norton
 * @see DictComprehensionNode
 */
public class ComprehensionNode implements SubTestNode {
    private String brace_type;
    private TypedVariableNode[] variables;
    private TestNode builder;
    private TestNode[] looped;

    /**
     * Create a new instance of ComprehensionNode.
     * @param brace_type The type of brace used in the comprehension
     * @param variables The variables being looped over in the loop
     * @param builder What is actually forming the values that go into the
     *                built object
     * @param looped The iterable being looped over
     */
    @Contract(pure = true)
    public ComprehensionNode(String brace_type, TypedVariableNode[] variables, TestNode builder, TestNode[] looped) {
        this.brace_type = brace_type;
        this.variables = variables;
        this.builder = builder;
        this.looped = looped;
    }

    public String getBrace_type() {
        return brace_type;
    }

    public TypedVariableNode[] getVariables() {
        return variables;
    }

    public TestNode getBuilder() {
        return builder;
    }

    public TestNode[] getLooped() {
        return looped;
    }

    public boolean hasBraces() {
        return !brace_type.isEmpty();
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
        String brace_type = tokens.getFirst().sequence;
        tokens.nextToken(true);
        TestNode builder = TestNode.parse(tokens, true);
        if (!tokens.tokenIs("for")) {
            throw new ParserException("Invalid start to comprehension");
        }
        tokens.nextToken(true);
        TypedVariableNode[] variables = TypedVariableNode.parseList(tokens);
        if (!tokens.tokenIs("in")) {
            throw new ParserException("Comprehension body must have in after variable list");
        }
        tokens.nextToken(true);
        LinkedList<TestNode> looped = new LinkedList<>();
        while (true) {
            if (tokens.tokenIs(brace_type)) {
                break;
            }
            looped.add(TestNode.parse(tokens, true));
            if (tokens.tokenIs(",")) {
                tokens.nextToken(true);
            } else {
                break;
            }
        }
        if (!brace_type.isEmpty() && !tokens.tokenIs(TokenList.matchingBrace(brace_type))) {
            throw new ParserException("Expected close brace");
        }
        tokens.nextToken();
        TestNode[] looped_array = looped.toArray(new TestNode[0]);
        return new ComprehensionNode(brace_type, variables, builder, looped_array);
    }

    public String toString() {
        String string = brace_type;
        StringJoiner sj = new StringJoiner(", ");
        for (TypedVariableNode t : variables) {
            sj.add(t.toString());
        }
        string += sj + " for " + builder + " in ";
        sj = new StringJoiner(", ");
        for (TestNode t : looped) {
            sj.add(t.toString());
        }
        return string + sj + TokenList.matchingBrace(brace_type);
    }
}
