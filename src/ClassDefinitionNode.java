import java.util.LinkedList;

public class ClassDefinitionNode implements DefinitionNode, ClassStatementNode {
    private TypeNode name;
    private TypeNode[] superclasses;
    private ClassBodyNode body;
    private DescriptorNode[] descriptors;

    public ClassDefinitionNode(TypeNode name, TypeNode[] superclasses, ClassBodyNode body) {
        this.name = name;
        this.superclasses = superclasses;
        this.body = body;
    }

    public ClassDefinitionNode(TypeNode name, ClassBodyNode body) {
        this.name = name;
        this.superclasses = new TypeNode[0];
        this.body = body;
    }

    public void addDescriptor(DescriptorNode[] descriptors) {
        this.descriptors = descriptors;
    }

    public TypeNode getName() {
        return name;
    }

    public TypeNode[] getSuperclasses() {
        return superclasses;
    }

    public ClassBodyNode getBody() {
        return body;
    }

    public DescriptorNode[] getDescriptors() {
        return descriptors;
    }

    static ClassDefinitionNode parse(TokenList tokens) {
        assert tokens.tokenIs("class");
        tokens.nextToken();
        if (!tokens.tokenIs(TokenType.NAME) && !tokens.tokenIs("from")) {
            throw new ParserException("class keyword must be followed by class name");
        }
        TypeNode name = TypeNode.parse(tokens);
        LinkedList<TypeNode> superclasses = new LinkedList<>();
        while (tokens.tokenIs("from")) {
            tokens.nextToken();
            superclasses.add(TypeNode.parse(tokens));
        }
        return new ClassDefinitionNode(name, superclasses.toArray(new TypeNode[0]), ClassBodyNode.parse(tokens));
    }
}
