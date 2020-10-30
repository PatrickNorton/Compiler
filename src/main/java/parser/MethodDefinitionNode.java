package main.java.parser;

import java.util.EnumSet;
import java.util.Set;

/**
 * The class representing a method definition.
 * @author Patrick Norton
 * @see FunctionDefinitionNode
 */
public class MethodDefinitionNode implements DefinitionNode, ClassStatementNode, GeneralizableNode {
    private LineInfo lineInfo;
    private VariableNode name;
    private TypedArgumentListNode args;
    private TypeLikeNode[] retval;
    private StatementBodyNode body;
    private EnumSet<DescriptorNode> descriptors = DescriptorNode.emptySet();
    private NameNode[] annotations = new NameNode[0];
    private NameNode[] decorators = new NameNode[0];
    private TypeLikeNode[] generics = new TypeLikeNode[0];

    /**
     * Create a new instance of MethodDefinitionNode.
     * @param name The name of the method defined
     * @param args The arguments the method takes
     * @param retval The return values of the method
     * @param body The body of the method
     */

    public MethodDefinitionNode(LineInfo lineInfo, VariableNode name, TypedArgumentListNode args, TypeLikeNode[] retval, StatementBodyNode body) {
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

    public TypeLikeNode[] getRetval() {
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
    public TypeLikeNode[] getGenerics() {
        return generics;
    }

    @Override
    public void addGenerics(TypeLikeNode... types) {
        this.generics = types;
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

    static MethodDefinitionNode parse(TokenList tokens) {
        assert tokens.tokenIs(Keyword.METHOD);
        LineInfo info = tokens.lineInfo();
        tokens.nextToken();
        VariableNode name = VariableNode.parse(tokens);
        TypedArgumentListNode args = TypedArgumentListNode.parse(tokens);
        TypeLikeNode[] retval = TypeLikeNode.parseRetVal(tokens);
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
        return String.format("%s%smethod %s%s%s %s",
                GeneralizableNode.toString(generics),
                DescriptorNode.join(descriptors), name, args,
                TypeLikeNode.returnString(retval), body);
    }
}
