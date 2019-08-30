import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

/**
 * The node representing a class definition
 *
 * @author
 */
public class ClassDefinitionNode implements DefinitionNode, ClassStatementNode {
    private TypeNode name;
    private TypeNode[] superclasses;
    private ClassBodyNode body;
    private DescriptorNode[] descriptors;

    /**
     * Create new instance of ClassDefinitionNode
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
    public void addDescriptor(DescriptorNode[] descriptors) {
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

    public DescriptorNode[] getDescriptors() {
        return descriptors;
    }

    /**
     * Parse ClassDefinitionNode from list of tokens.
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
    static ClassDefinitionNode parse(@NotNull TokenList tokens) {
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
