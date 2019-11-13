package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * The class representing an interface definition.
 * @author Patrick Norton
 * @see ClassDefinitionNode
 */
public class InterfaceDefinitionNode implements DefinitionNode, ClassStatementNode {
    private LineInfo lineInfo;
    private TypeNode name;
    private TypeLikeNode[] superclasses;
    private InterfaceBodyNode body;
    private EnumSet<DescriptorNode> descriptors = DescriptorNode.emptySet();
    private NameNode[] decorators = new NameNode[0];
    private NameNode[] annotations = new NameNode[0];

    /**
     * Construct a new instance of InterfaceDefinitionNode.
     * @param name The name of the interface
     * @param superclasses The superclasses of the interface
     * @param body The body of the interface
     */
    @Contract(pure = true)
    public InterfaceDefinitionNode(LineInfo lineInfo, TypeNode name, TypeLikeNode[] superclasses, InterfaceBodyNode body) {
        this.lineInfo = lineInfo;
        this.name = name;
        this.superclasses = superclasses;
        this.body = body;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    @Override
    public TypeNode getName() {
        return name;
    }

    public TypeLikeNode[] getSuperclasses() {
        return superclasses;
    }

    @Override
    public InterfaceBodyNode getBody() {
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
     * Parse a new interface definition from a list of tokens.
     * <p>
     *     The syntax for an interface definition is: <code>*({@link
     *     DescriptorNode} "interface" {@link TypeNode} ["from" {@link
     *     TypeNode} *("," {@link TypeNode}) [","] {@link
     *     InterfaceBodyNode}</code>. Descriptors are parsed separately, but
     *     the list of tokens must begin with "interface".
     * </p>
     * @param tokens The list of tokens to be parsed destructively
     * @return The freshly parsed InterfaceDefinitionNode
     */
    @NotNull
    @Contract("_ -> new")
    static InterfaceDefinitionNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.INTERFACE);
        LineInfo info = tokens.lineInfo();
        tokens.nextToken();
        TypeNode name = TypeNode.parse(tokens);
        TypeLikeNode[] superclasses = TypeLikeNode.parseListOnToken(tokens, Keyword.FROM);
        return new InterfaceDefinitionNode(info, name, superclasses, InterfaceBodyNode.parse(tokens));
    }

    @Override
    public String toString() {
        return DescriptorNode.join(descriptors) + "interface " + name + " " + body;
    }
}
