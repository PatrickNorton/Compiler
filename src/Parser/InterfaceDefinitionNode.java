package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.LinkedList;

/**
 * The class representing an interface definition.
 * @author Patrick Norton
 * @see ClassDefinitionNode
 */
public class InterfaceDefinitionNode implements DefinitionNode, ClassStatementNode {
    private TypeNode name;
    private TypeNode[] superclasses;
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
    public InterfaceDefinitionNode(TypeNode name, TypeNode[] superclasses, InterfaceBodyNode body) {
        this.name = name;
        this.superclasses = superclasses;
        this.body = body;
    }

    @Override
    public TypeNode getName() {
        return name;
    }

    public TypeNode[] getSuperclasses() {
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
        tokens.nextToken();
        TypeNode name = TypeNode.parse(tokens);
        LinkedList<TypeNode> superclasses = new LinkedList<>();
        if (tokens.tokenIs(Keyword.FROM)) {
            tokens.nextToken();
            while (!tokens.tokenIs("{")) {
                superclasses.add(TypeNode.parse(tokens));
                if (!tokens.tokenIs(TokenType.COMMA)) {
                    break;
                }
                tokens.nextToken();
            }
        }
        return new InterfaceDefinitionNode(name, superclasses.toArray(new TypeNode[0]), InterfaceBodyNode.parse(tokens));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (DescriptorNode d : descriptors) {
            sb.append(d);
            sb.append(' ');
        }
        return sb + "interface " + name + " " + body;
    }
}
