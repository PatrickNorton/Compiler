import org.jetbrains.annotations.Contract;

// TODO: Make me an enum

/**
 * The node representing all descriptors.
 * <p>
 *     This node is used for the descriptor types in such things as {@link
 *     ClassStatementNode} and {@link InterfaceStatementNode}, which are the
 *     only statements which may be preceded by descriptors.
 * </p>
 *
 * @author Patrick Norton
 * @see InterfaceStatementNode
 * @see ClassStatementNode
 */
public class DescriptorNode implements AtomicNode {
    public String name;

    @Contract(pure = true)
    public DescriptorNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
