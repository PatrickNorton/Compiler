package main.java.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class UnionDefinitionNode implements BaseClassNode {
    private final LineInfo lineInfo;
    private final TypeNode name;
    private final TypeLikeNode[] superclasses;
    private final ClassBodyNode body;
    private EnumSet<DescriptorNode> descriptors = DescriptorNode.emptySet();
    private NameNode[] decorators = new NameNode[0];
    private NameNode[] annotations = new NameNode[0];

    /**
     * Create a new instance of a UnionDefinitionNode.
     * @param name The name of the class being instantiated
     * @param superclasses The superclasses of the class
     * @param body The main body of the class
     */
    @Contract(pure = true)
    public UnionDefinitionNode(LineInfo lineInfo, TypeNode name, TypeLikeNode[] superclasses, ClassBodyNode body) {
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
    public void addDescriptor(EnumSet<DescriptorNode> descriptors) {
        this.descriptors = descriptors;
    }

    @Override
    public TypeNode getName() {
        return name;
    }

    public TypeLikeNode[] getSuperclasses() {
        return superclasses;
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

    public String strName() {
        return name.strName();
    }

    /**
     * Parse a UnionDefinitionNode from list of tokens.
     * <p>
     *     The syntax for a class definition as as follows: <code>
     *     [*{@link DescriptorNode}] "class" {@link TypeNode} ["from"
     *     *{@link TypeNode}] {@link ClassBodyNode}</code>.
     *     The ClassDefinitionNode must start with "class", descriptors are
     *     parsed in a different section of the code.
     * </p>
     * @param tokens The tokens to be parsed. Parse operates destructively on
     *               this.
     * @return The new ClassDefinitionNode which was parsed
     */
    @NotNull
    @Contract("_ -> new")
    static UnionDefinitionNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.UNION);
        LineInfo info = tokens.lineInfo();
        tokens.nextToken();
        if (!tokens.tokenIs(TokenType.NAME)) {
            throw tokens.error("union keyword must be followed by class name");
        }
        TypeNode name = TypeNode.parse(tokens);
        TypeLikeNode[] superclasses = TypeLikeNode.parseListOnToken(tokens, Keyword.FROM);
        return new UnionDefinitionNode(info, name, superclasses, ClassBodyNode.parse(tokens));
    }

    @Override
    public String toString() {
        return String.format("%sunion %s %s %s", DescriptorNode.join(descriptors), name,
                superclasses.length > 0 ? "from " + TestNode.toString(superclasses) : "", body);
    }
}
