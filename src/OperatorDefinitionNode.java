import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing an operator definition.
 * @author Patrick Norton
 */
public class OperatorDefinitionNode implements DefinitionNode, ClassStatementNode {
    private SpecialOpNameNode op_code;
    private TypeNode[] ret_type;
    private TypedArgumentListNode args;
    private StatementBodyNode body;
    private DescriptorNode[] descriptors;

    /**
     * Construct a new instance of OperatorDefinitionNode.
     * @param op_code The code of the operator definition
     * @param ret_type The return type of the operator
     * @param args The arguments the operator takes
     * @param body The body of the operator definition
     */
    // TODO: Change op_code to SpecialOpNameNode
    @Contract(pure = true)
    public OperatorDefinitionNode(SpecialOpNameNode op_code, TypeNode[] ret_type, TypedArgumentListNode args, StatementBodyNode body) {
        this.op_code = op_code;
        this.ret_type = ret_type;
        if (args != null) {
            this.args = args;
        } else {
            this.args = new TypedArgumentListNode();
        }
        this.body = body;
    }

    @Override
    public void addDescriptor(DescriptorNode[] nodes) {
        this.descriptors = nodes;
    }

    public SpecialOpNameNode getOp_code() {
        return op_code;
    }

    public TypeNode[] getRet_type() {
        return ret_type;
    }

    public TypedArgumentListNode getArgs() {
        return args;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
    }

    @Override
    public DescriptorNode[] getDescriptors() {
        return descriptors;
    }

    @Override
    public VariableNode getName() {
        return new VariableNode(op_code.getOperator().name);
    }

    /**
     * Parse a new operator definition from a list of tokens.
     * <p>
     *     The syntax for an operator definition is <code>*{@link
     *     DescriptorNode} SPECIAL_OP [{@link TypedArgumentListNode}] ["->"
     *     {@link TypeNode} *("," {@link TypeNode}) [","]] {@link
     *     StatementBodyNode}</code>. Descriptors are parsed separately, and
     *     therefore the list of tokens must begin with a special operator.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly-parsed OperatorDefinitionNode
     */
    @NotNull
    @Contract("_ -> new")
    static OperatorDefinitionNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(TokenType.OPERATOR_SP);
        SpecialOpNameNode op_code = SpecialOpNameNode.parse(tokens);
        TypedArgumentListNode args = TypedArgumentListNode.parseOnToken(tokens, "(");
        TypeNode[] retval;
        if (tokens.tokenIs(TokenType.ARROW)) {
            retval = TypeNode.parseRetVal(tokens);
        } else {
            retval = new TypeNode[0];
        }
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        return new OperatorDefinitionNode(op_code, retval, args, body);
    }
}
