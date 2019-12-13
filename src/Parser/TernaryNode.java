package Parser;

import org.jetbrains.annotations.Contract;

/**
 * The class representing a ternary statement.
 * @author Patrick Norton
 */
public class TernaryNode implements TestNode {
    private LineInfo lineInfo;
    private TestNode ifTrue;
    private TestNode statement;
    private TestNode ifFalse;

    public TernaryNode(TestNode ifTrue, TestNode statement, TestNode ifFalse) {
        this(ifTrue.getLineInfo(), ifTrue, statement, ifFalse);
    }

    @Contract(pure = true)
    public TernaryNode(LineInfo lineInfo, TestNode ifTrue, TestNode statement, TestNode ifFalse) {
        this.lineInfo = lineInfo;
        this.ifTrue = ifTrue;
        this.statement = statement;
        this.ifFalse = ifFalse;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public TestNode getIfTrue() {
        return ifTrue;
    }

    public TestNode getStatement() {
        return statement;
    }

    public TestNode getIfFalse() {
        return ifFalse;
    }

    @Override
    public String toString() {
        return ifTrue + " if " + statement + " else " + ifFalse;
    }
}
