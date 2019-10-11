package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.LinkedList;

/**
 * The class representing an enum definition.
 *
 * @author Patrick Norton
 */
public class EnumDefinitionNode implements ClassStatementNode, DefinitionNode, InlineableNode {
    private TypeNode name;
    private EnumKeywordNode[] names;
    private ClassBodyNode body;
    private boolean inline;
    private EnumSet<DescriptorNode> descriptors = DescriptorNode.emptySet();
    private NameNode[] decorators = new NameNode[0];
    private NameNode[] annotations = new NameNode[0];

    /**
     * Construct a new instance of EnumDefinitionNode.
     * @param name The name of the enum
     * @param names The names of the instances
     * @param body The rest of the enum body
     */
    @Contract(pure = true)
    public EnumDefinitionNode(TypeNode name, EnumKeywordNode[] names, ClassBodyNode body) {
        this.name = name;
        this.names = names;
        this.body = body;
    }

    @Override
    public TypeNode getName() {
        return name;
    }

    public EnumKeywordNode[] getNames() {
        return names;
    }

    @Override
    public ClassBodyNode getBody() {
        return body;
    }

    @Override
    public boolean isInline() {
        return inline;
    }

    @Override
    public void setInline(boolean inline) {
        this.inline = inline;
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
     * Parse an EnumDefinitionNode from a list of tokens.
     * <p>
     *     The syntax for an enum definition is: <code>"enum" {@link NameNode}
     *     "{" *({@link EnumKeywordNode} "," *NEWLINE) {@link EnumKeywordNode}
     *     *({@link IndependentNode} NEWLINE) "}"</code>.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed EnumDefinitionNode
     */
    @NotNull
    @Contract("_ -> new")
    public static EnumDefinitionNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.ENUM);
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
        return sb + "enum " + name + " " + body;
    }
}
