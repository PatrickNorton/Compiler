import java.util.LinkedList;

public class ArgumentNode implements BaseNode {
    private VariableNode variable;
    private String vararg;
    private TestNode argument;

    public ArgumentNode(VariableNode variable, String vararg, TestNode argument) {
        this.variable = variable;
        this.vararg = vararg;
        this.argument = argument;
    }

    public VariableNode getVariable() {
        return variable;
    }

    public String getVararg() {
        return vararg;
    }

    public TestNode getArgument() {
        return argument;
    }

    public boolean isVararg() {
        return !vararg.isEmpty();
    }

    static ArgumentNode[] parseList(TokenList tokens) {
        if (!tokens.tokenIs("(")) {
            throw new ParserException("Function call must start with open-paren");
        }
        tokens.nextToken(true);
        if (tokens.tokenIs(")")) {
            tokens.nextToken();
            return new ArgumentNode[0];
        }
        LinkedList<ArgumentNode> args = new LinkedList<>();
        while (true) {
            VariableNode var = new VariableNode();
            int offset = tokens.tokenIs("*", "**") ? 1 : 0;
            if (tokens.getToken(offset).is(TokenType.NAME)
                    && tokens.getToken(tokens.sizeOfVariable(offset)).is("=")) {
                var = VariableNode.parse(tokens);
                tokens.nextToken(true);
            }
            String vararg;
            if (tokens.tokenIs("*", "**")) {
                vararg = tokens.getFirst().sequence;
                tokens.nextToken(true);
            } else {
                vararg = "";
            }
            TestNode argument = TestNode.parse(tokens, true);
            args.add(new ArgumentNode(var, vararg, argument));
            if (tokens.tokenIs(")")) {
                break;
            }
            if (!tokens.tokenIs(",")) {
                throw new ParserException("Expected comma, got "+tokens.getFirst());
            }
            tokens.nextToken(true);
        }
        tokens.nextToken();
        return args.toArray(new ArgumentNode[0]);
    }
}
