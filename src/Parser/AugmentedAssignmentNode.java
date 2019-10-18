package Parser;

import org.jetbrains.annotations.Contract;

/**
 * The class representing an augmented assignment, such as +=, **=, &=, etc.
 * <p>
 *     This class also has no parse static method, as it is instead parsed by
 *     {@link BaseNode} in a private method, as that is where it is easiest to
 *     handle.
 * </p>
 */
public class AugmentedAssignmentNode implements AssignStatementNode {
    private LineInfo lineInfo;
    private AugAssignTypeNode operator;
    private NameNode name;
    private TestNode value;

    /**
     * Create new AugmentedAssignmentNode.
     * @param operator the operator being augmented
     * @param name the name which is augmented
     * @param value the value to which it is being augmented and then assigned
     */
    @Contract(pure = true)
    public AugmentedAssignmentNode(AugAssignTypeNode operator, NameNode name, TestNode value) {
        this(name.getLineInfo(), operator, name, value);
    }

    @Contract(pure = true)
    public AugmentedAssignmentNode(LineInfo lineInfo, AugAssignTypeNode operator, NameNode name, TestNode value) {
        this.lineInfo = lineInfo;
        this.operator = operator;
        this.name = name;
        this.value = value;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public AugAssignTypeNode getOperator() {
        return operator;
    }

    @Override
    public NameNode[] getName() {
        return new NameNode[] {name};
    }

    public TestNode getValue() {
        return value;
    }

    @Override
    public String toString() {
        return name + " " + operator.toString() + "= " + value;
    }
}
