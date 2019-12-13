package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

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
    public NameNode[] getNames() {
        return new NameNode[] {name};
    }

    @Override
    public TestListNode getValues() {
        return new TestListNode(value);
    }

    @Override
    public boolean isColon() {
        return false;
    }

    @NotNull
    @Contract("_ -> new")
    static AugmentedAssignmentNode parse(TokenList tokens) {
        NameNode var = NameNode.parse(tokens);
        if (!tokens.tokenIs(TokenType.AUG_ASSIGN)) {
            throw tokens.errorExpected("augmented assignment");
        }
        AugAssignTypeNode op = AugAssignTypeNode.parse(tokens);
        TestNode assignment = TestNode.parse(tokens);
        return new AugmentedAssignmentNode(op, var, assignment);
    }

    @Override
    public String toString() {
        return String.join(" ", name.toString(), operator.toString(), value.toString());
    }
}
