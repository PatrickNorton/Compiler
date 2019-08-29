import org.jetbrains.annotations.Contract;

/**
 * The class for an assignment, such as {@code a = f(b)}.
 * <p>
 * Also note: This has no {@code AssignmentNode.parse} static method, because it is built up instead by
 * the parse in AssignStatementNode. This is because there is no easy way to
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
    private Boolean is_colon;
    private NameNode[] name;
    private TestNode[] value;

    /**
     * Construct new instance of an AssignmentNode.
     * @param is_colon whether or not the assignment is a dynamic or static
     *                 assignment (:= vs =)
     * @param name The name(s) being assigned to
     * @param value The values to which they are assigned
     */
    @Contract(pure = true)
    public AssignmentNode(Boolean is_colon, NameNode[] name, TestNode[] value) {
        this.is_colon = is_colon;
        this.name = name;
        this.value = value;
    }

    public Boolean getIs_colon() {
        return is_colon;
    }

    @Override
    public NameNode[] getName() {
        return name;
    }

    public TestNode[] getValue() {
        return value;
    }
}
