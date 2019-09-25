package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.StringJoiner;

/**
 * The class for an assignment, such as {@code a = f(b)}.
 * <p>
 * Also note: This has no {@code Parser.AssignmentNode.parse} static method, because it is built up instead by
 * the parse in Parser.AssignStatementNode. This is because there is no easy way to
 * tell whether or not the result is declared or not at an early enough point to
 * split the two off from each other.
 * </p>
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
    private TestNode[] value;

    /**
     * Construct new instance of an Parser.AssignmentNode.
     * @param is_colon whether or not the assignment is a dynamic or static
     *                 assignment (:= vs =)
     * @param name The name(s) being assigned to
     * @param value The values to which they are assigned
     */
    @Contract(pure = true)
    public AssignmentNode(boolean is_colon, AssignableNode[] name, TestNode[] value) {
        this.is_colon = is_colon;
        this.name = name;
        this.value = value;
    }

    public boolean getIs_colon() {
        return is_colon;
    }

    @Override
    public AssignableNode[] getName() {
        return name;
    }

    public TestNode[] getValue() {
        return value;
    }

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
        TestNode[] value = TestNode.parseList(tokens, false);
        return new AssignmentNode(is_colon, name.toArray(new AssignableNode[0]), value);
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ");
        for (AssignableNode i : name) {
            joiner.add(i.toString());
        }
        return joiner + " " + (is_colon ? ":=" : '=') + " ...";
    }
}
