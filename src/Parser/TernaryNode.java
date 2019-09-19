package Parser;

import org.jetbrains.annotations.Contract;

/**
 * The class representing a ternary statement.
 * @author Patrick Norton
 */
public class TernaryNode implements TestNode {
    private TestNode if_true;
    private TestNode statement;
    private TestNode if_false;

    @Contract(pure = true)
    public TernaryNode(TestNode if_true, TestNode statement, TestNode if_false) {
        this.if_true = if_true;
        this.statement = statement;
        this.if_false = if_false;
    }

    public TestNode getIf_true() {
        return if_true;
    }

    public TestNode getStatement() {
        return statement;
    }

    public TestNode getIf_false() {
        return if_false;
    }

    @Override
    public String toString() {
        return if_true + " if " + statement + " else " + if_false;
    }
}
