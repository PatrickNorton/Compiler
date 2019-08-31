import org.jetbrains.annotations.Contract;

/**
 * The node representing a generic operator definition.
 * <p>
 *     This node is only valid in an interface definition, and nowhere else,
 *     due to its non-initialized nature. There is no parse method for this
 *     reason, it is only parsable from inside an {@link
 *     InterfaceStatementNode}.
 * </p>
 * @author Patrick Norton
 * @see InterfaceStatementNode
 * @see GenericFunctionNode
 */
public class GenericOperatorNode implements InterfaceStatementNode {
    private String op_code;
    private TypedArgumentListNode args;
    private TypeNode[] retvals;
    private DescriptorNode[] descriptors;

    @Contract(pure = true)
    public GenericOperatorNode(String op_code, TypedArgumentListNode args, TypeNode... retvals) {
        this.op_code = op_code;
        this.args = args;
        this.retvals = retvals;
    }

    public String getOp_code() {
        return op_code;
    }

    public TypedArgumentListNode getArgs() {
        return args;
    }

    public TypeNode[] getRetvals() {
        return retvals;
    }

    public DescriptorNode[] getDescriptors() {
        return descriptors;
    }

    @Override
    public void addDescriptor(DescriptorNode[] nodes) {
        this.descriptors = nodes;
    }
}
