package main.java.parser;

/**
 * The class representing an empty {@link TestNode}.
 *
 * @author Patrick Norton
 * @see TestNode
 */
public class EmptyTestNode implements TestNode {
    @Override
    public LineInfo getLineInfo() {
        return LineInfo.empty();
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public String toString() {
        return "";
    }
}
