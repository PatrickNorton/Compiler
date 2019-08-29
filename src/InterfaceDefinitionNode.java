import java.util.LinkedList;

public class InterfaceDefinitionNode implements ComplexStatementNode, ClassStatementNode {
    private TypeNode name;
    private TypeNode[] superclasses;
    private InterfaceBodyNode body;
    private DescriptorNode[] descriptors;

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

    static InterfaceDefinitionNode parse(TokenList tokens) {
        assert tokens.tokenIs("interface");
        tokens.nextToken();
        TypeNode name = TypeNode.parse(tokens);
        LinkedList<TypeNode> superclasses = new LinkedList<>();
        while (tokens.tokenIs("from")) {
            tokens.nextToken();
            superclasses.add(TypeNode.parse(tokens));
        }
        return new InterfaceDefinitionNode(name, superclasses.toArray(new TypeNode[0]), InterfaceBodyNode.parse(tokens));
    }
}
