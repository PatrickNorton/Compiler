package Parser;

/**
 * The interface representing all definition-style complex statements.
 * @author Patrick Norton
 */
public interface DefinitionNode extends ComplexStatementNode {
    /**
     * All things which are defined must have a name, thus this mandates it.
     * @return The name of the defined thing
     */
    AtomicNode getName();
}
