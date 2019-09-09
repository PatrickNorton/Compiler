import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.StringJoiner;

/**
 * The class representing a declaration and assignment rolled into one.
 *
 * @author Patrick Norton
 * @see DeclarationNode
 * @see AssignmentNode
 */
public class DeclaredAssignmentNode implements AssignStatementNode, ClassStatementNode {
    private Boolean is_colon;
    private TypedVariableNode[] assigned;
    private TestNode[] value;
    private DescriptorNode[] descriptors;

    /**
     * Create new instance of DeclaredAssignmentNode.
     * @param is_colon Whether the assignment is dynamic (true) or static (false)
     * @param assigned The variables being assigned to
     * @param value The values being assigned
     */
    // TODO: Refactor such that types and names are in a TypedVariableNode instead
    @Contract(pure = true)
    public DeclaredAssignmentNode(Boolean is_colon, TypedVariableNode[] assigned, TestNode[] value) {
        this.is_colon = is_colon;
        this.assigned = assigned;
        this.value = value;
    }

    public Boolean getIs_colon() {
        return is_colon;
    }

    @Override
    public NameNode[] getName() {
        ArrayList<NameNode> name = new ArrayList<>();
        for (TypedVariableNode t : assigned) {
            name.add(t.getVar());
        }
        return name.toArray(new NameNode[0]);
    }

    public TypedVariableNode[] getAssigned() {
        return assigned;
    }

    public DescriptorNode[] getDescriptors() {
        return descriptors;
    }

    @Override
    public void addDescriptor(DescriptorNode[] nodes) {
        this.descriptors = nodes;
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
    @NotNull
    @Contract("_ -> new")
    static DeclaredAssignmentNode parse(TokenList tokens) {
        TypedVariableNode[] assigned = TypedVariableNode.parseList(tokens);
        if (!tokens.tokenIs(TokenType.ASSIGN)) {
            throw new ParserException("Unexpected "+tokens.getFirst());
        }
        boolean is_colon = tokens.tokenIs(":=");
        tokens.nextToken();
        TestNode[] value = TestNode.parseList(tokens, false);
        return new DeclaredAssignmentNode(is_colon, assigned, value);
    }

    @Override
    public String toString() {
        String string;
        StringJoiner sj = new StringJoiner(" ");
        for (DescriptorNode d : descriptors) {
            sj.add(d.toString());
        }
        string = sj + " ";
        sj = new StringJoiner(", ");
        for (TypedVariableNode t : assigned) {
            sj.add(t.toString());
        }
        string += sj + (is_colon ? " := " : " = ");
        sj = new StringJoiner(", ");
        for (TestNode v : value) {
            sj.add(v.toString());
        }
        return string + sj;
    }
}
