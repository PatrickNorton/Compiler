import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The node representing the name of a special operator.
 * @author Patrick Norton
 */
public class SpecialOpNameNode implements NameNode {
    private OperatorTypeNode operator;

    @Contract(pure = true)
    public SpecialOpNameNode(OperatorTypeNode operator) {
        this.operator = operator;
    }

    public OperatorTypeNode getOperator() {
        return operator;
    }

    @NotNull
    @Contract("_ -> new")
    public static SpecialOpNameNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(TokenType.OPERATOR_SP);
        String op_type = tokens.getFirst().sequence.replaceFirst("^operator *",  "");
        OperatorTypeNode op = OperatorTypeNode.findOp(op_type, OperatorTypeNode.Use.OPERATOR_SP);
        tokens.nextToken();
        return new SpecialOpNameNode(op);
    }

    @Override
    public String toString() {
        return "operator " + operator;
    }
}
