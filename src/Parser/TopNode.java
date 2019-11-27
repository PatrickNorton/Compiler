package Parser;

import org.jetbrains.annotations.Contract;

import java.nio.file.Path;
import java.util.LinkedList;

/**
 * The node representing the full program.
 * @author Patrick Norton
 * @see StatementBodyNode
 */
public class TopNode implements BaseNode {
    private Path path;
    private LinkedList<IndependentNode> nodes;

    @Contract(pure = true)
    public TopNode(Path path, LinkedList<IndependentNode> nodes) {
        this.path = path;
        this.nodes = nodes;
    }

    @Contract(pure = true)
    public TopNode(Path path) {
        this.path = path;
        this.nodes = new LinkedList<>();
    }

    @Override
    public LineInfo getLineInfo() {
        return LineInfo.empty();
    }

    public void add(IndependentNode operand) {
        nodes.add(operand);
    }

    public LinkedList<IndependentNode> getNodes() {
        return nodes;
    }

    @Override
    public String toString() {
        return path.toString();
    }
}
