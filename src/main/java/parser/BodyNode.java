package main.java.parser;

/**
 * The interface representing a body of statements, e.g. {@link
 * StatementBodyNode} or {@link ClassBodyNode}.
 *
 * @author Patrick Norton
 */
public interface BodyNode extends BaseNode, EmptiableNode {
    IndependentNode[] getStatements();
    IndependentNode get(int i);
}
