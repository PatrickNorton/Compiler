package main.java.parser;

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
    private boolean isColon;
    private AssignableNode[] name;
    private TestListNode value;
    private LineInfo lineInfo;

    /**
     * Construct new instance of an AssignmentNode.
     * @param isColon whether or not the assignment is a dynamic or static
     *                 assignment (:= vs =)
     * @param name The name(s) being assigned to
     * @param value The values to which they are assigned
     */
    @Contract(pure = true)
    public AssignmentNode(boolean isColon, AssignableNode[] name, TestListNode value) {
        this(name[0].getLineInfo(), isColon, name, value);
    }

    @Contract(pure = true)
    public AssignmentNode(LineInfo lineInfo, boolean isColon, AssignableNode[] name, TestListNode value) {
        this.isColon = isColon;
        this.name = name;
        this.value = value;
        this.lineInfo = lineInfo;
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
