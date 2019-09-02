/**
 * The interface representing a simple flow statement.
 * <p>
 *     Simple flow statements are flow statements without any sort of a body
 *     attached. They all have a possible condition attached to them, and thus
 *     the interface has a getCond method.
 * </p>
 */
public interface SimpleFlowNode extends SimpleStatementNode {
    TestNode getCond();
}
