/**
 * Represents a complex statement.
 * <p>
 *     A complex statement is a statement which contains other statements, e.g.
 *     one that has a body, such as an if-statement, do-while, statement, or
 *     class definition.
 * </p>
 * @author Patrick Norton
 */
public interface ComplexStatementNode extends StatementNode {
    /**
     * Since all complex statements have a body, they all must have a getBody
     * method.
     * @return The body of the statement
     */
    BodyNode getBody();
}
