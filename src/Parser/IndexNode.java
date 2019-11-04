package Parser;

import org.jetbrains.annotations.Contract;

/**
 * The node for a variable index.
 * @author Patrick Norton
 */
public class IndexNode implements NameNode {
    private LineInfo lineInfo;
    private TestNode var;
    private TestNode[] indices;

    public IndexNode(TestNode var, TestNode... indices) {
        this(var.getLineInfo(), var, indices);
    }

    /**
     * Construct a new instance of IndexNode.
     * @param var The node being indexed
     * @param indices The list of indices in the square brackets
     */
    @Contract(pure = true)
    public IndexNode(LineInfo lineInfo, TestNode var, TestNode... indices) {
        this.lineInfo = lineInfo;
        this.var = var;
        this.indices = indices;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public TestNode getVar() {
        return var;
    }

    public TestNode[] getIndices() {
        return indices;
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", NameNode.parenthesize(var), TestNode.toString(indices));
    }
}
