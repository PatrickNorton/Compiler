package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * The class representing a property definition.
 * @author Patrick Norton
 */
public class PropertyDefinitionNode implements DefinitionNode, ClassStatementNode {
    private LineInfo lineInfo;
    private VariableNode name;
    private TypeNode type;
    private StatementBodyNode get;
    private TypedArgumentListNode set_args;
    private StatementBodyNode set;
    private EnumSet<DescriptorNode> descriptors = DescriptorNode.emptySet();
    private NameNode[] annotations = new NameNode[0];
    private NameNode[] decorators = new NameNode[0];

    /**
     * Create a new instance of PropertyDefinitionNode.
     * @param name The name of the property
     * @param get The getter attribute for the property
     * @param set_args The arguments for setting the property
     * @param set The setter attribute for the property
     */
    @Contract(pure = true)
    public PropertyDefinitionNode(LineInfo lineInfo, VariableNode name, TypeNode type,
                                  StatementBodyNode get, TypedArgumentListNode set_args, StatementBodyNode set) {
        this.lineInfo = lineInfo;
        this.name = name;
        this.type = type;
        this.get = get;
        this.set_args = set_args;
        this.set = set;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    @Override
    public void addDescriptor(EnumSet<DescriptorNode> nodes) {
        this.descriptors = nodes;
    }

    @Override
    public VariableNode getName() {
        return name;
    }

    public TypeNode getType() {
        return type;
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

    public EnumSet<DescriptorNode> getDescriptors() {
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

    @Override
    public EnumSet<DescriptorNode> validDescriptors() {
        return DescriptorNode.DECLARATION_VALID;
    }

    /**
     * Parse a new PropertyDefinitionNode from a list of tokens.
     * <p>
     *     The syntax of a property definition is: <code>*({@link
     *     DescriptorNode}) "property" {@link TypeNode} [{@link NameNode}] "{"
     *     ["get" {@link StatementBodyNode}] ["set" {@link
     *     TypedArgumentListNode} {@link StatementBodyNode}] "}"</code>.
     * </p>
     * @param tokens The list of tokens to be parsed
     * @return The freshly parsed PropertyDefinitionNode
     */
    @NotNull
    @Contract("_ -> new")
    static PropertyDefinitionNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.PROPERTY);
        LineInfo info = tokens.lineInfo();
        tokens.nextToken();
        TypeNode type = TypeNode.parse(tokens);
        VariableNode name = VariableNode.empty();
        if (!tokens.tokenIs("{")) {
            name = VariableNode.parse(tokens);
        }
        tokens.nextToken(true);
        StatementBodyNode get = new StatementBodyNode();
        StatementBodyNode set = new StatementBodyNode();
        TypedArgumentListNode set_args = new TypedArgumentListNode();
        if (tokens.tokenIs(Keyword.GET)) {
            tokens.nextToken();
            get = StatementBodyNode.parse(tokens);
        }
        if (tokens.tokenIs(Keyword.SET)) {
            tokens.nextToken();
            set_args = TypedArgumentListNode.parse(tokens);
            set = StatementBodyNode.parse(tokens);
        }
        tokens.passNewlines();
        if (!tokens.tokenIs("}")) {
            throw tokens.error("Only set and get are allowed in context statements");
        }
        tokens.nextToken();
        return new PropertyDefinitionNode(info, name, type, get, set_args, set);
    }

    @Override
    public String toString() {
        return DescriptorNode.join(descriptors) + "property " + name + get;
    }
}
