package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * The class representing an operator assignment.
 */
public class SpecialOpAssignmentNode implements ClassStatementNode {
    private LineInfo lineInfo;
    private SpecialOpNameNode name;
    private TestNode assignment;
    private EnumSet<DescriptorNode> descriptors = DescriptorNode.emptySet();

    public SpecialOpAssignmentNode(SpecialOpNameNode name, TestNode assignment) {
        this(name.getLineInfo(), name, assignment);
    }

    @Contract(pure = true)
    public SpecialOpAssignmentNode(LineInfo lineInfo, SpecialOpNameNode name, TestNode assignment) {
        this.lineInfo = lineInfo;
        this.name = name;
        this.assignment = assignment;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public SpecialOpNameNode getName() {
        return name;
    }

    public TestNode getAssignment() {
        return assignment;
    }

    @Override
    public EnumSet<DescriptorNode> getDescriptors() {
        return descriptors;
    }

    @Override
    public void addDescriptor(EnumSet<DescriptorNode> nodes) {
        this.descriptors = nodes;
    }

    /**
     * Parse a special operator assignment from a list of tokens.
     * <p>
     *     The syntax for an operator assignment is: <code>*{@link
     *     DescriptorNode} SPECIAL_OP "=" {@link TestNode}</code>. The list of
     *     tokens passed must begin with a SPECIAL_OP, as descriptors are parsed
     *     separately.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The newly parsed SpecialOpAssignmentNode
     */
    @NotNull
    @Contract("_ -> new")
    public static SpecialOpAssignmentNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(1, "=");
        SpecialOpNameNode name = SpecialOpNameNode.parse(tokens);
        assert tokens.tokenIs("=");
        tokens.nextToken();
        TestNode assignment;
        if (tokens.tokenIs(TokenType.OPERATOR_SP)) {
            assignment = SpecialOpNameNode.parse(tokens);
        } else {
            assignment = TestNode.parse(tokens);
        }
        return new SpecialOpAssignmentNode(name, assignment);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (DescriptorNode d : descriptors) {
            sb.append(d);
            sb.append(" ");
        }
        return sb + "" + name + " = " + assignment;
    }
}
