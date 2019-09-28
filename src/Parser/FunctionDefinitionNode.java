package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * The class representing a function definition.
 * @author Patrick Norton
 */
public class FunctionDefinitionNode implements DefinitionNode {
    private VariableNode name;
    private TypedArgumentListNode args;
    private TypeNode[] retval;
    private StatementBodyNode body;
    private EnumSet<DescriptorNode> descriptors = DescriptorNode.emptySet();
    private NameNode[] decorators = new NameNode[0];
    private NameNode[] annotations = new NameNode[0];

    @Contract(pure = true)
    public FunctionDefinitionNode(VariableNode name, TypedArgumentListNode args, TypeNode[] retval, StatementBodyNode body) {
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

    @Override
    public EnumSet<DescriptorNode> getDescriptors() {
        return descriptors;
    }

    @Override
    public void addDescriptor(EnumSet<DescriptorNode> nodes) {
        this.descriptors = nodes;
    }

    @Override
    public NameNode[] getDecorators() {
        return decorators;
    }

    @Override
    public void addDecorators(NameNode... decorators) {
        this.decorators = decorators;
    }

    @Override
    public NameNode[] getAnnotations() {
        return annotations;
    }

    @Override
    public void addAnnotations(NameNode... annotations) {
        this.annotations = annotations;
    }

    /**
     * Parse a Parser.FunctionDefinitionNode from a list of tokens.
     * <p>
     *     The syntax for a function definition is: <code>"func" {@link
     *     VariableNode} {@link TypedArgumentListNode} ["->" {@link TypeNode}
     *     *("," {@link TypeNode}) [","]] {@link StatementBodyNode}</code>.
     * </p>
     * @param tokens The list of tokens to be parsed destructively
     * @return The newly parsed Parser.FunctionDefinitionNode
     */
    @NotNull
    @Contract("_ -> new")
    static FunctionDefinitionNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.FUNC);
        tokens.nextToken();
        VariableNode name = VariableNode.parse(tokens);
        TypedArgumentListNode args = TypedArgumentListNode.parse(tokens);
        TypeNode[] retval = TypeNode.parseRetVal(tokens);
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        return new FunctionDefinitionNode(name, args, retval, body);
    }

    @Override
    public String toString() {
        return "func " + name + args + " " + body;
    }
}
