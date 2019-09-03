import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a normal, standard operator.
 * @author Patrick Norton
 * @see OperatorTypeNode
 */
public class OperatorNode implements SubTestNode {
    private OperatorTypeNode operator;
    private TestNode[] operands;

    /**
     * Construct a new instance of OperatorNode.
     * @param operator The operator itself
     * @param operands The operands of the operator
     */
    @Contract(pure = true)
    public OperatorNode(String operator, TestNode... operands) {
        this.operator = OperatorTypeNode.find_op(operator);
        this.operands = operands;
    }

    /**
     * Construct a new instance of OperatorNode.
     * @param operator The operator itself
     * @param operands The operands of the operator
     */
    @Contract(pure = true)
    public OperatorNode(String operator, @NotNull TypedArgumentListNode operands) {
        this.operator = OperatorTypeNode.find_op(operator);
        this.operands = operands.getArgs();
    }

    /**
     * Construct a new instance of OperatorNode.
     * @param operator The operator itself
     * @param operands The operands of the operator
     */
    @Contract(pure = true)
    public OperatorNode(OperatorTypeNode operator, TestNode... operands) {
        this.operator = operator;
        this.operands = operands;
    }

    public OperatorTypeNode getOperator() {
        return operator;
    }

    public TestNode[] getOperands() {
        return operands;
    }

    // FIXME: Order of operations for these vs normal operators
    /**
     * Parse a left-starting boolean operator from a list of tokens.
     * <p>
     *     The only valid left-boolean is a {@code not}, so others will throw
     *     a {@link ParserException}. The first token in the list must be a
     *     boolean operator.
     * </p>
     * @param tokens The list of tokens to be parsed destructively
     * @param ignore_newline Whether or not to ignore newlines
     * @return The freshly parsed operator
     */
    @NotNull
    @Contract("_, _ -> new")
    static OperatorNode parseBoolOp(@NotNull TokenList tokens, boolean ignore_newline) {
        assert tokens.tokenIs(TokenType.BOOL_OP);
        switch (tokens.getFirst().sequence) {
            case "not":
                tokens.nextToken(ignore_newline);
                return new OperatorNode("not", TestNode.parse(tokens));
            case "and":
            case "or":
            case "xor":
                throw new ParserException(tokens.getFirst()+" must be in between statements");
            default:
                throw new RuntimeException("Unknown boolean operator");
        }
    }

    /**
     * Parse a left-starting operator from a list of tokens.
     * <p>
     *     The only valid left-operator is {@code -}, so others will throw a
     *     {@link ParserException}. The first token in the list must be an
     *     operator.
     * </p>
     * @param tokens The list of tokens to be parsed
     * @param ignore_newline Whether or not to ignore newlines
     * @return The freshly parsed operator
     */
    @NotNull
    @Contract("_, _ -> new")
    static OperatorNode parse(@NotNull TokenList tokens, boolean ignore_newline) {
        assert tokens.tokenIs(TokenType.OPERATOR);
        if (!tokens.tokenIs("-")) {
            throw new ParserException("- is the only unary operator");
        }
        tokens.nextToken(ignore_newline);
        TestNode next = TestNode.parse(tokens, ignore_newline);
        return new OperatorNode(tokens.getFirst().sequence, next);
    }
}
