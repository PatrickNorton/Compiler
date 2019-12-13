package main.java.parser;

/**
 * The interface representing a node that can be empty or not.
 *
 * @author Patrick Norton
 */
public interface EmptiableNode extends BaseNode {
    boolean isEmpty();
}
