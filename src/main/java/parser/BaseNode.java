package main.java.parser;

/**
 * The base for all nodes.
 *
 * @author Patrick Norton
 * @see IndependentNode
 */
public interface BaseNode extends Lined {
    LineInfo getLineInfo();
}
