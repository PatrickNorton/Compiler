package main.java.parser;

public interface DeclaredStatementNode extends SimpleStatementNode, TopLevelNode {
    AssignableNode[] getNames();
    TypedVariableNode[] getTypes();
}
