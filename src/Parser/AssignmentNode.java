package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

/**
 * The class for an assignment, such as {@code a = f(b)}.
 *
 * @author Patrick Norton
 * @see AssignStatementNode
 * @see AugmentedAssignmentNode
 */
public class AssignmentNode implements AssignStatementNode {
    /**
     * The three parts of the assignment: Whether or not the assignment has a
     * colon (= vs :=), the name(s) being assigned, and the value(s) to which
     * they are assigned
     */
    private boolean is_colon;
    private AssignableNode[] name;
    private TestListNode value;
    private LineInfo lineInfo;

    /**
     * Construct new instance of an AssignmentNode.
     * @param is_colon whether or not the assignment is a dynamic or static
     *                 assignment (:= vs =)
     * @param name The name(s) being assigned to
     * @param value The values to which they are assigned
     */
    @Contract(pure = true)
    public AssignmentNode(boolean is_colon, AssignableNode[] name, TestListNode value) {
        this(name[0].getLineInfo(), is_colon, name, value);
    }

    @Contract(pure = true)
    public AssignmentNode(LineInfo lineInfo, boolean is_colon, AssignableNode[] name, TestListNode value) {
        this.is_colon = is_colon;
        this.name = name;
        this.value = value;
        this.lineInfo = lineInfo;
    }

    public boolean getIs_colon() {
        return is_colon;
    }

    @Override
    public AssignableNode[] getName() {
        return name;
    }

    public TestListNode getValue() {
        return value;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
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
    @NotNull
    @Contract("_ -> new")
    public static AssignmentNode parse(@NotNull TokenList tokens) {
        LinkedList<AssignableNode> name = new LinkedList<>();
        while (!tokens.tokenIs(TokenType.ASSIGN)) {
            name.add(AssignableNode.parse(tokens));
            if (!tokens.tokenIs(TokenType.COMMA)) {
                break;
            }
            tokens.nextToken();
        }
        boolean is_colon = tokens.tokenIs(":=");
        tokens.nextToken();
        TestListNode value = TestListNode.parse(tokens, false);
        return new AssignmentNode(is_colon, name.toArray(new AssignableNode[0]), value);
    }

    @Override
    public String toString() {
        String names = TestNode.toString(name);
        return String.join(" ", names, (is_colon ? ":=" : "="), value.toString());
    }
}
