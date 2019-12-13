package Parser;

public interface DeclaredStatementNode extends SimpleStatementNode {
    AssignableNode[] getNames();
    TypedVariableNode[] getTypes();
}
