import org.jetbrains.annotations.Contract;

import java.util.LinkedList;

/**
 * The node representing the full program.
 * @author Patrick Norton
 * @see StatementBodyNode
 */
public class TopNode implements BaseNode {
    private LinkedList<BaseNode> nodes;

    @Contract(pure = true)
    public TopNode(LinkedList<BaseNode> nodes) {
        this.nodes = nodes;
    }

    @Contract(pure = true)
    public TopNode() {
        this.nodes = new LinkedList<>();
    }

    public void add(BaseNode operand) {
        nodes.add(operand);
    }

    public LinkedList<BaseNode> getNodes() {
        return nodes;
    }
}
