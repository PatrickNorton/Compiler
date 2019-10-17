package Parser;

import org.jetbrains.annotations.Contract;

import java.util.StringJoiner;

/**
 * The node for a variable index.
 * @author Patrick Norton
 */
public class IndexNode implements NameNode {
    private LineInfo lineInfo;
    private NameNode var;
    private TestNode[] indices;

    public IndexNode(NameNode var, TestNode... indices) {
        this(var.getLineInfo(), var, indices);
    }

    /**
     * Construct a new instance of IndexNode.
     * @param var The node being indexed
     * @param indices The list of indices in the square brackets
     */
    @Contract(pure = true)
    public IndexNode(LineInfo lineInfo, NameNode var, TestNode... indices) {
        this.lineInfo = lineInfo;
        this.var = var;
        this.indices = indices;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public NameNode getVar() {
        return var;
    }

    public TestNode[] getIndices() {
        return indices;
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", ");
        for (TestNode t : indices) {
            sj.add(t.toString());
        }
        return var + "[" + sj + "]";
    }
}
