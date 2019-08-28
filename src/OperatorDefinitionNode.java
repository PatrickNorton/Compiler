public class OperatorDefinitionNode implements DefinitionNode, ClassStatementNode {
    private String op_code;
    private TypeNode[] ret_type;
    private TypedArgumentListNode args;
    private StatementBodyNode body;
    private DescriptorNode[] descriptors;

    public OperatorDefinitionNode(String op_code, TypeNode[] ret_type, TypedArgumentListNode args, StatementBodyNode body) {
        this.op_code = op_code;
        this.ret_type = ret_type;
        if (args != null) {
            this.args = args;
        } else {
            this.args = new TypedArgumentListNode();
        }
        this.body = body;
    }

    public OperatorDefinitionNode(String op_code, StatementBodyNode body) {
        this.op_code = op_code;
        this.args = new TypedArgumentListNode();
        this.body = body;
    }

    @Override
    public void addDescriptor(DescriptorNode[] nodes) {
        this.descriptors = nodes;
    }

    public String getOp_code() {
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

    public DescriptorNode[] getDescriptors() {
        return descriptors;
    }

    @Override
    public VariableNode getName() {
        return new VariableNode(op_code);
    }

    static OperatorDefinitionNode parse(TokenList tokens) {
        String op_code = tokens.getFirst().sequence.replaceFirst("operator *", "");
        tokens.nextToken();
        if (tokens.tokenIs(TokenType.ASSIGN)) {
            tokens.nextToken();
            if (tokens.tokenIs(TokenType.OPERATOR_SP)) {
                OperatorTypeNode op = new OperatorTypeNode(tokens.getFirst().sequence);
                tokens.nextToken();
                tokens.Newline();
                return new OperatorDefinitionNode(op_code, new StatementBodyNode(op));
            } else if (tokens.tokenIs(TokenType.NAME)) {
                NameNode var = NameNode.parse(tokens);
                tokens.Newline();
                return new OperatorDefinitionNode(op_code, new StatementBodyNode(var));
            } else {
                throw new ParserException("Operator equivalence must be done to another var or op");
            }
        }
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
