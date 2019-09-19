package Parser;

import org.jetbrains.annotations.Contract;

import java.util.LinkedList;

/**
 * The node representing the full program.
 * @author Patrick Norton
 * @see StatementBodyNode
 */
public class TopNode implements BaseNode {
    private LinkedList<IndependentNode> nodes;

    @Contract(pure = true)
    public TopNode(LinkedList<IndependentNode> nodes) {
        this.nodes = nodes;
    }

    @Contract(pure = true)
    public TopNode() {
        this.nodes = new LinkedList<>();
    }

    public void add(IndependentNode operand) {
        nodes.add(operand);
    }

    public LinkedList<IndependentNode> getNodes() {
        return nodes;
    }
}
