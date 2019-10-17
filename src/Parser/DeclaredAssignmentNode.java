package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;
import java.util.StringJoiner;

/**
 * The class representing a declaration and assignment rolled into one.
 *
 * @author Patrick Norton
 * @see DeclarationNode
 * @see AssignmentNode
 */
public class DeclaredAssignmentNode implements AssignStatementNode, ClassStatementNode {
    private LineInfo lineInfo;
    private boolean is_colon;
    private TypedVariableNode[] assigned;
    private TestNode[] value;
    private EnumSet<DescriptorNode> descriptors = DescriptorNode.emptySet();

    /**
     * Create new instance of DeclaredAssignmentNode.
     * @param is_colon Whether the assignment is dynamic (true) or static (false)
     * @param assigned The variables being assigned to
     * @param value The values being assigned
     */
    @Contract(pure = true)
    public DeclaredAssignmentNode(LineInfo lineInfo, boolean is_colon, TypedVariableNode[] assigned, TestNode[] value) {
        this.lineInfo = lineInfo;
        this.is_colon = is_colon;
        this.assigned = assigned;
        this.value = value;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public boolean getIs_colon() {
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

    public EnumSet<DescriptorNode> getDescriptors() {
        return descriptors;
    }

    @Override
    public void addDescriptor(EnumSet<DescriptorNode> nodes) {
        this.descriptors = nodes;
    }

    @Override
    public Set<DescriptorNode> validDescriptors() {
        return DescriptorNode.DECLARATION_VALID;
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
    static DeclaredAssignmentNode parse(@NotNull TokenList tokens) {
        LineInfo info = tokens.lineInfo();
        TypedVariableNode[] assigned = TypedVariableNode.parseList(tokens);
        if (!tokens.tokenIs(TokenType.ASSIGN)) {
            throw tokens.error("Unexpected "+tokens.getFirst());
        }
        boolean is_colon = tokens.tokenIs(":=");
        tokens.nextToken();
        TestNode[] value = TestNode.parseList(tokens, false);
        return new DeclaredAssignmentNode(info, is_colon, assigned, value);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (DescriptorNode d : descriptors) {
            sb.append(d.toString());
            sb.append(' ');
        }
        StringJoiner sj = new StringJoiner(", ");
        for (TypedVariableNode t : assigned) {
            sj.add(t.toString());
        }
        sb.append(sj);
        sj = new StringJoiner(", ");
        for (TestNode v : value) {
            sj.add(v.toString());
        }
        return sb + (is_colon ? " := " : " = ") + sj;
    }
}
