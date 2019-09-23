package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.StringJoiner;

public class EnumDefinitionNode implements ClassStatementNode, ComplexStatementNode, DecoratableNode, AnnotatableNode {
    private TypeNode types;
    private EnumKeywordNode[] names;
    private ClassBodyNode body;
    private DescriptorNode[] descriptors = new DescriptorNode[0];
    private NameNode[] decorators = new NameNode[0];
    private NameNode[] annotations = new NameNode[0];

    @Contract(pure = true)
    public EnumDefinitionNode(TypeNode types, EnumKeywordNode[] names, ClassBodyNode body) {
        this.types = types;
        this.names = names;
        this.body = body;
    }

    public TypeNode getTypes() {
        return types;
    }

    public EnumKeywordNode[] getNames() {
        return names;
    }

    @Override
    public ClassBodyNode getBody() {
        return body;
    }

    @Override
    public DescriptorNode[] getDescriptors() {
        return descriptors;
    }

    @Override
    public void addDescriptor(DescriptorNode[] nodes) {
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

    @NotNull
    @Contract("_ -> new")
    public static EnumDefinitionNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs("enum");
        tokens.nextToken();
        TypeNode name = TypeNode.parse(tokens);
        if (!tokens.tokenIs("{")) {
            throw new ParserException("Expected {, got " + tokens.getFirst());
        }
        tokens.nextToken(true);
        LinkedList<EnumKeywordNode> names = new LinkedList<>();
        while (true) {
            names.add(EnumKeywordNode.parse(tokens));
            if (!tokens.tokenIs(TokenType.COMMA)) {
                break;
            } else {
                tokens.nextToken(true);
            }
        }
        tokens.passNewlines();
        ClassBodyNode body = ClassBodyNode.parseEnum(tokens);
        return new EnumDefinitionNode(name, names.toArray(new EnumKeywordNode[0]), body);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (DescriptorNode d : descriptors) {
            sb.append(d);
            sb.append(' ');
        }
        return sb + "enum " + types + " " + body;
    }
}