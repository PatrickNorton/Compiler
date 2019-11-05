package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The class representing a normal, standard operator.
 * @author Patrick Norton
 * @see OperatorTypeNode
 */
public class OperatorNode implements SubTestNode {
    private LineInfo lineInfo;
    private OperatorTypeNode operator;
    private List<ArgumentNode> operands;

    /**
     * Construct a new instance of OperatorNode.
     * @param operator The operator itself
     * @param operands The operands of the operator
     */
    @Contract(pure = true)
    public OperatorNode(LineInfo lineInfo, OperatorTypeNode operator, @NotNull ArgumentNode... operands) {
        this.lineInfo = lineInfo;
        this.operator = operator;
        this.operands = new ArrayList<>(Arrays.asList(operands));
    }

    /**
     * Construct a new instance of OperatorNode.
     * @param operator The operator itself
     * @param operands The operands of the operator
     */
    @Contract(pure = true)
    public OperatorNode(LineInfo lineInfo, OperatorTypeNode operator, TestNode... operands) {
        this(lineInfo, operator, ArgumentNode.fromTestNodes(operands));
    }

    @Contract(pure = true)
    public OperatorNode(LineInfo lineInfo, OperatorTypeNode operator) {
        this(lineInfo, operator, new ArgumentNode[0]);
    };

    /**
     * Construct a new OperatorNode with no operands
     * @param operator The operator type
     */
    @Contract(pure = true)
    private OperatorNode(OperatorTypeNode operator) {
        this(operator.getLineInfo(), operator, new ArgumentNode[0]);
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public int precedence() {
        return operator.precedence;
    }

    public boolean isUnary() {
        return operator.isUnary();
    }

    public boolean isPostfix() {
        return operator.isPostfix();
    }

    /**
     * Construct a new OperatorNode from an empty one and a list of operands.
     * @param empty The empty OperatorNode
     * @param operands The operands to add to it
     * @return
     */
    @NotNull
    @Contract("_, _, _ -> new")
    public static OperatorNode fromEmpty(LineInfo lineInfo, @NotNull OperatorNode empty, TestNode... operands) {
        return new OperatorNode(lineInfo, empty.getOperator(), operands);
    }

    /**
     * Parse an empty OperatorNode from a list of tokens
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed empty OperatorNode
     */
    @NotNull
    @Contract("_ -> new")
    public static OperatorNode empty(@NotNull TokenList tokens) {
        assert tokens.tokenIs(TokenType.OPERATOR, TokenType.KEYWORD);
        return new OperatorNode(OperatorTypeNode.fromToken(tokens.getFirst()));
    }

    public OperatorTypeNode getOperator() {
        return operator;
    }

    public ArgumentNode[] getOperands() {
        return operands.toArray(new ArgumentNode[0]);
    }

    public OperatorNode addArgument(ArgumentNode e) {
        this.operands.add(e);
        return this;
    }

    public OperatorNode addArgument(TestNode e) {
        return addArgument(new ArgumentNode(e));
    }

    public OperatorNode addArgument(int index, ArgumentNode e) {
        this.operands.add(index, e);
        return this;
    }

    public OperatorNode addArgument(int index, TestNode e) {
        return addArgument(index, new ArgumentNode(e));
    }

    public OperatorNode setArgument(int index, ArgumentNode e) {
        this.operands.set(index, e);
        return this;
    }

    public OperatorNode setArgument(int index, TestNode e) {
        return setArgument(index, new ArgumentNode(e));
    }

    @Override
    public boolean isEmpty() {
        return operands.isEmpty();
    }

    @Override
    public String toString() {
        switch (operands.size()) {
            case 0:
                return "\\" + operator;
            case 1:
                return unaryString();
            case 2:
                return operandString(0) + " " + operator + " " + operandString(1);
            default:
                return "\\" + operator + "(" + operands.get(0) + ", ...)";
        }
    }

    @NotNull
    private String unaryString() {
        assert operands.size() == 1;
        if (operator.isUnary()) {
            if (operator.isPostfix()) {
                return operandString(0) + operator;
            } else if (operator == OperatorTypeNode.BOOL_NOT) {
                return operator.toString() + " " + operandString(0);
            } else {
                return operator.toString() + operandString(0);
            }
        } else {
            return "\\" + operator + "(" + operands.get(0) + ")";
        }
    }

    private String operandString(int index) {
        ArgumentNode operand = operands.get(index);
        TestNode argument = operand.getArgument();
        if (!(argument instanceof OperatorNode)) {
            return argument.toString();
        }
        OperatorNode argumentOp = (OperatorNode) argument;
        if (argumentOp.precedence() > this.precedence()) {
            return "(" + argumentOp + ")";
        } else {
            return argumentOp.toString();
        }
    }
}
