package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;

/**
 * The class representing a method definition.
 * @author Patrick Norton
 * @see FunctionDefinitionNode
 */
public class MethodDefinitionNode implements DefinitionNode, ClassStatementNode {
    private LineInfo lineInfo;
    private VariableNode name;
    private TypedArgumentListNode args;
    private TypeNode[] retval;
    private StatementBodyNode body;
    private EnumSet<DescriptorNode> descriptors = DescriptorNode.emptySet();
    private NameNode[] annotations = new NameNode[0];
    private NameNode[] decorators = new NameNode[0];

    /**
     * Create a new instance of MethodDefinitionNode.
     * @param name The name of the method defined
     * @param args The arguments the method takes
     * @param retval The return values of the method
     * @param body The body of the method
     */
    @Contract(pure = true)
    public MethodDefinitionNode(LineInfo lineInfo, VariableNode name, TypedArgumentListNode args, TypeNode[] retval, StatementBodyNode body) {
        this.lineInfo = lineInfo;
        this.name = name;
        this.args = args;
        this.retval = retval;
        this.body = body;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
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

    public EnumSet<DescriptorNode> getDescriptors() {
        return descriptors;
    }

    @Override
    public void addDescriptor(EnumSet<DescriptorNode> nodes) {
        this.descriptors = nodes;
    }

    @Override
    public NameNode[] getAnnotations() {
        return annotations;
    }

    @Override
    public void addAnnotations(NameNode... annotations) {
        this.annotations = annotations;
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
    public Set<DescriptorNode> validDescriptors() {
        return DescriptorNode.METHOD_VALID;
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
        assert tokens.tokenIs(Keyword.METHOD);
        LineInfo info = tokens.lineInfo();
        tokens.nextToken();
        VariableNode name = VariableNode.parse(tokens);
        TypedArgumentListNode args = TypedArgumentListNode.parse(tokens);
        TypeNode[] retval = TypeNode.parseRetVal(tokens);
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        return new MethodDefinitionNode(info, name, args, retval, body);
    }

    static MethodDefinitionNode fromGeneric(TokenList tokens, GenericFunctionNode op) {
        assert tokens.tokenIs("{");
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        return new MethodDefinitionNode(op.getLineInfo(), op.getName(), op.getArgs(), op.getRetvals(), body);
    }

    @Override
    public String toString() {
        return String.format("%smethod %s%s%s %s",
                DescriptorNode.join(descriptors), name, args, TypeNode.returnString(retval), body);
    }
}
