package main.java.parser;

public interface BaseClassNode extends DefinitionNode, ClassStatementNode {
    TypeLikeNode[] getSuperclasses();
}
