package main.java.parser;

import java.util.Set;

/**
 * The interface representing a definition of a generic function.
 *
 * @author Patrick Norton
 */
public interface GenericDefinitionNode extends InterfaceStatementNode {
    TypedArgumentListNode getArgs();
    TypeLikeNode[] getRetvals();

    @Override
    default Set<DescriptorNode> validDescriptors() {
        return DescriptorNode.DEFINITION_VALID;
    }
}
