package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a normal, standard operator.
 * @author Patrick Norton
 * @see OperatorTypeNode
 */
public class OperatorNode implements SubTestNode {
    private OperatorTypeNode operator;
    private ArgumentNode[] operands;

    /**
     * Construct a new instance of OperatorNode.
     * @param operator The operator itself
     * @param operands The operands of the operator
     */
    @Contract(pure = true)
    public OperatorNode(OperatorTypeNode operator, @NotNull ArgumentNode... operands) {
        this.operator = operator;
        this.operands = operands;
    }

    /**
     * Construct a new instance of OperatorNode.
     * @param operator The operator itself
     * @param operands The operands of the operator
     */
    @Contract(pure = true)
    public OperatorNode(OperatorTypeNode operator, TestNode... operands) {
        this(operator, ArgumentNode.fromTestNodes(operands));
    }

    /**
     * Construct a new OperatorNode with no operands
     * @param operator The operator type
     */
    @Contract(pure = true)
    public OperatorNode(OperatorTypeNode operator) {
        this(operator, new ArgumentNode[0]);
    }

    /**
     * Construct a new OperatorNode from an empty one and a list of operands.
     * @param empty The empty OperatorNode
     * @param operands The operands to add to it
     * @return
     */
    @NotNull
    @Contract("_, _ -> new")
    public static OperatorNode fromEmpty(@NotNull OperatorNode empty, TestNode... operands) {
        return new OperatorNode(empty.getOperator(), operands);
    }

    /**
     * Parse an empty OperatorNode from a list of tokens
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed empty OperatorNode
     */
    @NotNull
    @Contract("_ -> new")
    public static OperatorNode empty(@NotNull TokenList tokens) {
        assert tokens.tokenIs(TokenType.OPERATOR, TokenType.BOOL_OP, TokenType.KEYWORD);
        return new OperatorNode(OperatorTypeNode.fromToken(tokens.getFirst()));
    }

    public OperatorTypeNode getOperator() {
        return operator;
    }

    public ArgumentNode[] getOperands() {
        return operands;
    }

    @Override
    public boolean isEmpty() {
        return operands.length == 0;
    }

    @Override
    public String toString() {
        switch (operands.length) {
            case 0:
                return "\\" + operator;
            case 1:
                return operator + " " + operands[0];
            case 2:
                return operands[0] + " " + operator + " " + operands[1];
            default:
                return "\\" + operator + "(" + operands[0] + ", ...)";
        }
    }
}
