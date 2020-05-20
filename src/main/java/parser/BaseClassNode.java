package main.java.parser;

public interface BaseClassNode extends DefinitionNode, ClassStatementNode {
    TypeNode getName();
    TypeLikeNode[] getSuperclasses();
}
