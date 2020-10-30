package main.java.parser;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * The class representing a declaration and assignment rolled into one.
 *
 * @author Patrick Norton
 * @see DeclarationNode
 * @see AssignmentNode
 */
public class DeclaredAssignmentNode implements AssignStatementNode, ClassStatementNode, DeclaredStatementNode, AnnotatableNode {
    private LineInfo lineInfo;
    private boolean isColon;
    private TypedVariableNode[] assigned;
    private TestListNode value;
    private EnumSet<DescriptorNode> descriptors = DescriptorNode.emptySet();
    private NameNode[] annotations = new NameNode[0];

    /**
     * Create new instance of DeclaredAssignmentNode.
     * @param isColon Whether the assignment is dynamic (true) or static (false)
     * @param assigned The variables being assigned to
     * @param value The values being assigned
     */

    public DeclaredAssignmentNode(LineInfo lineInfo, boolean isColon, TypedVariableNode[] assigned, TestListNode value) {
        this.lineInfo = lineInfo;
        this.isColon = isColon;
        this.assigned = assigned;
        this.value = value;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    @Override
    public boolean isColon() {
        return isColon;
    }

    @Override
    public NameNode[] getNames() {
        NameNode[] name = new NameNode[assigned.length];
        for (int i = 0; i < assigned.length; i++) {
            name[i] = assigned[i].getVariable();
        }
        return name;
    }

    @Override
    public TypedVariableNode[] getTypes() {
        return assigned;
    }

    @Override
    public TestListNode getValues() {
        return value;
    }

    public EnumSet<DescriptorNode> getDescriptors() {
        return descriptors;
    }

    @Override
    public void addDescriptor(EnumSet<DescriptorNode> nodes) {
        this.descriptors = nodes;
    }

    @Override
    public NameNode[] getAnnotations() {
        return annotations;
    }

    @Override
    public void addAnnotations(NameNode... annotations) {
        this.annotations = annotations;
    }

    @Override
    public Set<DescriptorNode> validDescriptors() {
        return DescriptorNode.DECLARATION_VALID;
    }

    public Optional<DescriptorNode> getMutability() {
        for (var node : descriptors) {
            if (DescriptorNode.MUT_NODES.contains(node)) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    /**
     * Parse a DeclaredAssignmentNode from a list of tokens.
     * <p>
     *     The syntax for a DeclaredAssignmentNode is: <code>{@link TypeNode}
     *     {@link VariableNode} *["," [{@link TypeNode}] {@link VariableNode}]
     *     [","] ("=" | ":=") {@link TestNode} *["," {@link TestNode}] [","]
     *     </code>.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The newly parsed DeclaredAssignmentNode
     */

    static DeclaredAssignmentNode parse(TokenList tokens) {
        LineInfo info = tokens.lineInfo();
        TypedVariableNode[] assigned = TypedVariableNode.parseList(tokens);
        boolean isColon = tokens.tokenIs(":=");
        tokens.expect(TokenType.ASSIGN, "assignment operator", false);
        TestListNode value = TestListNode.parse(tokens, false);
        return new DeclaredAssignmentNode(info, isColon, assigned, value);
    }

    @Override
    public String toString() {
        return DescriptorNode.join(descriptors) +
                TestNode.toString(assigned) +
                (isColon ? " := " : " = ") + value;
    }
}
