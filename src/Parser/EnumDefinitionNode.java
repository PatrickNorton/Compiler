package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * The class representing an enum definition.
 *
 * @author Patrick Norton
 */
public class EnumDefinitionNode implements ClassStatementNode, DefinitionNode {
    private LineInfo lineInfo;
    private TypeNode name;
    private TypeNode[] superclasses;
    private EnumKeywordNode[] names;
    private ClassBodyNode body;
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
    public EnumDefinitionNode(LineInfo lineInfo, TypeNode name, TypeNode[] superclasses, EnumKeywordNode[] names, ClassBodyNode body) {
        this.lineInfo = lineInfo;
        this.name = name;
        this.superclasses = superclasses;
        this.names = names;
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

    public TypeNode[] getSuperclasses() {
        return superclasses;
    }

    public EnumKeywordNode[] getNames() {
        return names;
    }

    @Override
    public ClassBodyNode getBody() {
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
        LineInfo info = tokens.lineInfo();
        tokens.nextToken();
        TypeNode name = TypeNode.parse(tokens);
        TypeNode[] superclasses = TypeNode.parseListOnToken(tokens, Keyword.FROM);
        if (!tokens.tokenIs("{")) {
            throw tokens.error("Expected {, got " + tokens.getFirst());
        }
        tokens.nextToken(true);
        List<EnumKeywordNode> names = new ArrayList<>();
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
        return new EnumDefinitionNode(info, name, superclasses, names.toArray(new EnumKeywordNode[0]), body);
    }

    @Override
    public String toString() {
        return DescriptorNode.join(descriptors) + "enum " + name + " " + body;
    }
}
