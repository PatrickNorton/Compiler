import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a property definition.
 * @author Patrick Norton
 */
public class PropertyDefinitionNode implements DefinitionNode, ClassStatementNode {
    public VariableNode name;
    public StatementBodyNode get;
    public TypedArgumentListNode set_args;
    public StatementBodyNode set;
    public DescriptorNode[] descriptors;

    /**
     * Create a new instance of PropertyDefinitionNode.
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
        this.descriptors = new DescriptorNode[0];
    }

    @Contract(pure = true)
    public PropertyDefinitionNode(StatementBodyNode get) {
        this.get = get;
    }

    @Contract(pure = true)
    public PropertyDefinitionNode(TypedArgumentListNode set_args, StatementBodyNode set) {
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

    /**
     * Parse a new PropertyDefinitionNode from a list of tokens.
     * <p>
     *     The syntax of a property definition is: <code>*({@link
     *     DescriptorNode}) "property" [{@link NameNode}] "{" ["get" {@link
     *     StatementBodyNode}] ["set" {@link TypedArgumentListNode} {@link
     *     StatementBodyNode}] "}"</code>.
     * </p>
     * @param tokens The list of tokens to be parsed
     * @return The freshly parsed PropertyDefinitionNode
     */
    @NotNull
    @Contract("_ -> new")
    static PropertyDefinitionNode parse(@NotNull TokenList tokens) {
        VariableNode name = new VariableNode();
        if (!tokens.tokenIs("{")) {
            name = VariableNode.parse(tokens);
        }
        tokens.nextToken(true);
        StatementBodyNode get = new StatementBodyNode();
        StatementBodyNode set = new StatementBodyNode();
        TypedArgumentListNode set_args = new TypedArgumentListNode();
        if (tokens.tokenIs("get")) {
            get = StatementBodyNode.parse(tokens);
        }
        if (tokens.tokenIs("set")) {
            set_args = TypedArgumentListNode.parse(tokens);
            set = StatementBodyNode.parse(tokens);
        }
        tokens.passNewlines();
        if (!tokens.tokenIs("}")) {
            throw new ParserException("Only set and get are allowed in context statements");
        }
        tokens.nextToken();
        tokens.Newline();
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
