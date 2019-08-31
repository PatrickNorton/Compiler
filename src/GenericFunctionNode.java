import org.jetbrains.annotations.Contract;

/**
 * The class representing a generic function.
 * <p>
 *     This class is only valid within an interface definition, and thus has no
 *     parse method, as it is not meant to be constructed separately.
 * </p>
 * @author Patrick Norton
 * @see InterfaceStatementNode
 * @see GenericOperatorNode
 */
public class GenericFunctionNode implements InterfaceStatementNode {
    private VariableNode name;
    private TypedArgumentListNode args;
    private TypeNode[] retvals;
    private DescriptorNode[] descriptors;

    @Contract(pure = true)
    public GenericFunctionNode(VariableNode name, TypedArgumentListNode args, TypeNode... retvals) {
        this.name = name;
        this.args = args;
        this.retvals = retvals;
    }

    public VariableNode getName() {
        return name;
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
