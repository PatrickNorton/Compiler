package main.java.parser;

public interface DeclaredStatementNode extends SimpleStatementNode {
    AssignableNode[] getNames();
    TypedVariableNode[] getTypes();
}
