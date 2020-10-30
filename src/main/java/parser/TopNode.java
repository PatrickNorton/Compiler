package main.java.parser;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * The node representing the full program.
 * @author Patrick Norton
 * @see StatementBodyNode
 */
public class TopNode implements BaseNode, Iterable<IndependentNode> {
    private Path path;
    private LinkedList<IndependentNode> nodes;

    public TopNode(Path path) {
        this.path = path;
        this.nodes = new LinkedList<>();
    }

    public Path getPath() {
        return path;
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
    public Iterator<IndependentNode> iterator() {
        return nodes.iterator();
    }

    @Override
    public String toString() {
        return path.toString();
    }
}
