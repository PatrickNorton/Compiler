import org.jetbrains.annotations.Contract;

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
}
