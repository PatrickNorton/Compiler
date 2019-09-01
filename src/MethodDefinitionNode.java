import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a method definition.
 * @author Patrick Norton
 * @see FunctionDefinitionNode
 */
public class MethodDefinitionNode implements DefinitionNode, ClassStatementNode {
    private VariableNode name;
    private TypedArgumentListNode args;
    private TypeNode[] retval;
    private StatementBodyNode body;
    private DescriptorNode[] descriptors;

    /**
     * Create a new instance of MethodDefinitionNode.
     * @param name The name of the method defined
     * @param args The arguments the method takes
     * @param retval The return values of the method
     * @param body The body of the method
     */
    @Contract(pure = true)
    public MethodDefinitionNode(VariableNode name, TypedArgumentListNode args, TypeNode[] retval, StatementBodyNode body) {
        this.name = name;
        this.args = args;
        this.retval = retval;
        this.body = body;
    }

    @Override
    public VariableNode getName() {
        return name;
    }

    public TypedArgumentListNode getArgs() {
        return args;
    }

    public TypeNode[] getRetval() {
        return retval;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
    }

    public DescriptorNode[] getDescriptors() {
        return descriptors;
    }

    @Override
    public void addDescriptor(DescriptorNode[] nodes) {
        this.descriptors = nodes;
    }

    /**
     * Parse a method definition from a list of tokens.
     * <p>
     *     The grammar for a method definition is as follows: <code>*{@link
     *     DescriptorNode} "method" {@link VariableNode} {@link
     *     TypedArgumentListNode} ["->" {@link TypeNode} *("," {@link
     *     TypeNode}) [","]</code>. Descriptors are parsed separately, so the
     *     token list must begin with "method" when passed.
     * </p>
     * @param tokens The list of tokens to be parsed
     * @return The freshly-parsed method definition
     */
    @NotNull
    @Contract("_ -> new")
    static MethodDefinitionNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs("method");
        tokens.nextToken();
        VariableNode name = VariableNode.parse(tokens);
        TypedArgumentListNode args = TypedArgumentListNode.parse(tokens);
        TypeNode[] retval = TypeNode.parseRetVal(tokens);
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        tokens.Newline();
        return new MethodDefinitionNode(name, args, retval, body);
    }
}
