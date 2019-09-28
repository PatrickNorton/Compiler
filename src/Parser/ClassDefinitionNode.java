package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.StringJoiner;

/**
 * The node representing a class definition
 *
 * @author
 */
public class ClassDefinitionNode implements DefinitionNode, ClassStatementNode {
    private TypeNode name;
    private TypeNode[] superclasses;
    private ClassBodyNode body;
    private EnumSet<DescriptorNode> descriptors = DescriptorNode.emptySet();
    private NameNode[] decorators = new NameNode[0];
    private NameNode[] annotations = new NameNode[0];

    /**
     * Create new instance of Parser.ClassDefinitionNode
     * @param name The name of the class being instantiated
     * @param superclasses The superclasses of the class
     * @param body The main body of the class
     */
    @Contract(pure = true)
    public ClassDefinitionNode(TypeNode name, TypeNode[] superclasses, ClassBodyNode body) {
        this.name = name;
        this.superclasses = superclasses;
        this.body = body;
    }

    @Override
    public void addDescriptor(EnumSet<DescriptorNode> descriptors) {
        this.descriptors = descriptors;
    }

    @Override
    public TypeNode getName() {
        return name;
    }

    public TypeNode[] getSuperclasses() {
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

    /**
     * Parse Parser.ClassDefinitionNode from list of tokens.
     * <p>
     *     The syntax for a class definition as as follows: <code>
     *     [*{@link DescriptorNode}] "class" {@link TypeNode} ["from"
     *     *{@link TypeNode}] {@link ClassBodyNode}</code>.
     *     The Parser.ClassDefinitionNode must start with "class", descriptors are
     *     parsed in a different section of the code.
     * </p>
     * @param tokens The tokens to be parsed. Parse operates destructively on
     *               this.
     * @return The new Parser.ClassDefinitionNode which was parsed
     */
    @NotNull
    @Contract("_ -> new")
    static ClassDefinitionNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.CLASS);
        tokens.nextToken();
        if (!tokens.tokenIs(TokenType.NAME)) {
            throw new ParserException("class keyword must be followed by class name");
        }
        TypeNode name = TypeNode.parse(tokens);
        LinkedList<TypeNode> superclasses = new LinkedList<>();
        while (tokens.tokenIs(Keyword.FROM)) {
            tokens.nextToken();
            superclasses.add(TypeNode.parse(tokens));
        }
        return new ClassDefinitionNode(name, superclasses.toArray(new TypeNode[0]), ClassBodyNode.parse(tokens));
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();
        for (DescriptorNode d : descriptors) {
            string.append(d);
            string.append(' ');
        }
        string.append("class ");
        string.append(name);
        string.append(" ");
        if (superclasses.length > 0) {
            StringJoiner sj = new StringJoiner(", ", "from ", " ");
            for (TypeNode t : superclasses) {
                sj.add(t.toString());
            }
            string.append(sj);
        }
        return string.append(body).toString();
    }
}
