package main.java.parser;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Optional;

/**
 * The class for an assignment, such as {@code a = f(b)}.
 *
 * @author Patrick Norton
 * @see AssignStatementNode
 * @see AugmentedAssignmentNode
 */
public class AssignmentNode implements AssignStatementNode, DescribableNode {
    /**
     * The three parts of the assignment: Whether or not the assignment has a
     * colon (= vs :=), the name(s) being assigned, and the value(s) to which
     * they are assigned
     */
    private boolean isColon;
    private AssignableNode[] name;
    private TestListNode value;
    private LineInfo lineInfo;
    private DescriptorNode mutability;

    /**
     * Construct new instance of an AssignmentNode.
     * @param isColon whether or not the assignment is a dynamic or static
     *                 assignment (:= vs =)
     * @param name The name(s) being assigned to
     * @param value The values to which they are assigned
     */

    public AssignmentNode(boolean isColon, AssignableNode[] name, TestListNode value) {
        this(name[0].getLineInfo(), isColon, name, value);
    }

    public AssignmentNode(LineInfo lineInfo, boolean isColon, AssignableNode[] name, TestListNode value) {
        this.isColon = isColon;
        this.name = name;
        this.value = value;
        this.lineInfo = lineInfo;
        this.mutability = null;
    }

    @Override
    public boolean isColon() {
        return isColon;
    }

    @Override
    public AssignableNode[] getNames() {
        return name;
    }

    public TestListNode getValues() {
        return value;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    @Override
    public void addDescriptor(EnumSet<DescriptorNode> nodes) {
        assert nodes.size() == 1 && mutability == null;
        for (var node : nodes) {
            mutability = node;
            return;
        }
    }

    @Override
    public EnumSet<DescriptorNode> getDescriptors() {
        return mutability == null ? EnumSet.noneOf(DescriptorNode.class) : EnumSet.of(mutability);
    }

    public Optional<DescriptorNode> getMutability() {
        return Optional.ofNullable(mutability);
    }

    /**
     * Parse an AssignmentNode from a list of tokens.
     * <p>
     *     The syntax for an AssignmentNode is: <code>{@link AssignableNode}
     *     *("," {@link AssignableNode}) [","] (=|:=) {@link TestNode} *(","
     *     {@link TestNode}) ","</code>.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed AssignmentNode
     */

    public static AssignmentNode parse(TokenList tokens) {
        LinkedList<AssignableNode> name = new LinkedList<>();
        while (!tokens.tokenIs(TokenType.ASSIGN)) {
            name.add(AssignableNode.parse(tokens));
            if (!tokens.tokenIs(TokenType.COMMA)) {
                break;
            }
            tokens.nextToken();
        }
        boolean isColon = tokens.tokenIs(":=");
        tokens.nextToken();
        TestListNode value = TestListNode.parse(tokens, false);
        return new AssignmentNode(isColon, name.toArray(new AssignableNode[0]), value);
    }

    @Override
    public String toString() {
        String names = TestNode.toString(name);
        return String.join(" ", names, (isColon ? ":=" : "="), value.toString());
    }
}
