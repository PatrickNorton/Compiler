import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

/**
 * The class representing an interface definition.
 * @author Patrick Norton
 * @see ClassDefinitionNode
 */
public class InterfaceDefinitionNode implements ComplexStatementNode, ClassStatementNode {
    private TypeNode name;
    private TypeNode[] superclasses;
    private InterfaceBodyNode body;
    private DescriptorNode[] descriptors;

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
        this.descriptors = null;
    }

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

    public DescriptorNode[] getDescriptors() {
        return descriptors;
    }

    @Override
    public void addDescriptor(DescriptorNode[] nodes) {
        this.descriptors = nodes;
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
        assert tokens.tokenIs("interface");
        tokens.nextToken();
        TypeNode name = TypeNode.parse(tokens);
        LinkedList<TypeNode> superclasses = new LinkedList<>();
        while (tokens.tokenIs("from")) {  // FIXME: Doesn't parse "from" correctly
            tokens.nextToken();
            superclasses.add(TypeNode.parse(tokens));
        }
        return new InterfaceDefinitionNode(name, superclasses.toArray(new TypeNode[0]), InterfaceBodyNode.parse(tokens));
    }
}
