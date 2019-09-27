package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a property definition.
 * @author Patrick Norton
 */
public class PropertyDefinitionNode implements DefinitionNode, ClassStatementNode {
    private VariableNode name;
    private StatementBodyNode get;
    private TypedArgumentListNode set_args;
    private StatementBodyNode set;
    private DescriptorNode[] descriptors = new DescriptorNode[0];
    private NameNode[] annotations = new NameNode[0];
    private NameNode[] decorators = new NameNode[0];

    /**
     * Create a new instance of Parser.PropertyDefinitionNode.
     * @param name The name of the property
     * @param get The getter attribute for the property
     * @param set_args The arguments for setting the property
     * @param set The setter attribute for the property
     */
    @Contract(pure = true)
    public PropertyDefinitionNode(VariableNode name, StatementBodyNode get, TypedArgumentListNode set_args, StatementBodyNode set) {
        this.name = name;
        this.get = get;
        this.set_args = set_args;
        this.set = set;
    }

    @Override
    public void addDescriptor(DescriptorNode[] nodes) {
        this.descriptors = nodes;
    }

    @Override
    public VariableNode getName() {
        return name;
    }

    public StatementBodyNode getGet() {
        return get;
    }

    public StatementBodyNode getSet() {
        return set;
    }

    public TypedArgumentListNode getSet_args() {
        return set_args;
    }

    public DescriptorNode[] getDescriptors() {
        return descriptors;
    }

    @Override
    public StatementBodyNode getBody() {
        return get;
    }

    @Override
    public void addAnnotations(NameNode... annotations) {
        this.annotations = annotations;
    }

    @Override
    public NameNode[] getAnnotations() {
        return annotations;
    }

    @Override
    public void addDecorators(NameNode... decorators) {
        this.decorators = decorators;
    }

    @Override
    public NameNode[] getDecorators() {
        return decorators;
    }

    /**
     * Parse a new Parser.PropertyDefinitionNode from a list of tokens.
     * <p>
     *     The syntax of a property definition is: <code>*({@link
     *     DescriptorNode}) "property" [{@link NameNode}] "{" ["get" {@link
     *     StatementBodyNode}] ["set" {@link TypedArgumentListNode} {@link
     *     StatementBodyNode}] "}"</code>.
     * </p>
     * @param tokens The list of tokens to be parsed
     * @return The freshly parsed Parser.PropertyDefinitionNode
     */
    @NotNull
    @Contract("_ -> new")
    static PropertyDefinitionNode parse(@NotNull TokenList tokens) {
        VariableNode name = VariableNode.empty();
        if (!tokens.tokenIs("{")) {
            name = VariableNode.parse(tokens);
        }
        tokens.nextToken(true);
        StatementBodyNode get = new StatementBodyNode();
        StatementBodyNode set = new StatementBodyNode();
        TypedArgumentListNode set_args = new TypedArgumentListNode();
        if (tokens.tokenIs(Keyword.GET)) {
            get = StatementBodyNode.parse(tokens);
        }
        if (tokens.tokenIs(Keyword.SET)) {
            set_args = TypedArgumentListNode.parse(tokens);
            set = StatementBodyNode.parse(tokens);
        }
        tokens.passNewlines();
        if (!tokens.tokenIs("}")) {
            throw new ParserException("Only set and get are allowed in context statements");
        }
        tokens.nextToken();
        return new PropertyDefinitionNode(name, get, set_args, set);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (DescriptorNode d : descriptors) {
            sb.append(d);
            sb.append(" ");
        }
        return sb + "property " + name + get;
    }
}
