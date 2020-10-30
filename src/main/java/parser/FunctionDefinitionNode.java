package main.java.parser;

import java.util.EnumSet;
import java.util.Set;

/**
 * The class representing a function definition.
 * @author Patrick Norton
 */
public class FunctionDefinitionNode implements DefinitionNode, GeneralizableNode {
    private LineInfo lineInfo;
    private VariableNode name;
    private TypedArgumentListNode args;
    private TypeLikeNode[] retval;
    private StatementBodyNode body;
    private EnumSet<DescriptorNode> descriptors = DescriptorNode.emptySet();
    private NameNode[] decorators = new NameNode[0];
    private NameNode[] annotations = new NameNode[0];
    private TypeLikeNode[] generics = new TypeLikeNode[0];

    public FunctionDefinitionNode(LineInfo lineInfo, VariableNode name, TypedArgumentListNode args,
                                  TypeLikeNode[] retval, StatementBodyNode body) {
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
        return DescriptorNode.FUNCTION_VALID;
    }

    /**
     * Parse a FunctionDefinitionNode from a list of tokens.
     * <p>
     *     The syntax for a function definition is: <code>"func" {@link
     *     VariableNode} {@link TypedArgumentListNode} ["->" {@link TypeNode}
     *     *("," {@link TypeNode}) [","]] {@link StatementBodyNode}</code>.
     * </p>
     * @param tokens The list of tokens to be parsed destructively
     * @return The newly parsed FunctionDefinitionNode
     */

    static FunctionDefinitionNode parse(TokenList tokens) {
        assert tokens.tokenIs(Keyword.FUNC);
        LineInfo info = tokens.lineInfo();
        tokens.nextToken();
        VariableNode name = VariableNode.parse(tokens);
        TypedArgumentListNode args = TypedArgumentListNode.parse(tokens);
        TypeLikeNode[] retval = TypeLikeNode.parseRetVal(tokens);
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        return new FunctionDefinitionNode(info, name, args, retval, body);
    }

    @Override
    public String toString() {
        return String.format("%s%sfunc %s%s%s %s",
                GeneralizableNode.toString(generics),
                DescriptorNode.join(descriptors), name, args,
                TypeLikeNode.returnString(retval), body);
    }
}
