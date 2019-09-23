package Parser;

import org.jetbrains.annotations.Contract;

import java.util.StringJoiner;

/**
 * The node for a variable index.
 * @author Patrick Norton
 */
public class IndexNode implements NameNode {
    private NameNode var;
    private TestNode[] indices;

    /**
     * Construct a new instance of Parser.IndexNode.
     * @param var The node being indexed
     * @param indices The list of indices in the square brackets
     */
    @Contract(pure = true)
    public IndexNode(NameNode var, TestNode... indices) {
        this.var = var;
        this.indices = indices;
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