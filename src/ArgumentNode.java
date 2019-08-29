import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

/**
 * ArgumentNode is the class for an untyped argument in code, for example in
 * function calls, e.g. {@code foo(a, b+c+d, *e)}, where there would be 3
 * ArgumentNodes, consisting of a, b+c*d, and *e.
 *
 * @author Patrick Norton
 * @see TypedArgumentNode
 * @see FunctionCallNode
 */
public class ArgumentNode implements BaseNode {
    /**
     * ArgumentNodes have three instance variables, representing parts of the
     * argument: {@code [variable]=[vararg][argument]}.
     */
    private VariableNode variable;
    private String vararg;
    private TestNode argument;

    /**
     * Create new instance of ArgumentNode.
     * @param variable The variable which is used as a keyword in the call
     * @param vararg Whether or not there is a vararg, and what the vararg is
     *               (e.g. * vs **)
     * @param argument What the actual argument is, e.g. what is actually
     *                 passed to the function in the end
     */
    @Contract(pure = true)
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

    /**
     * Parse a list of arguments from a list of tokens, and return the
     * corresponding nodes. This would be used, for example, in parsing the
     * arguments of a function, and it does include a check as for whether or
     * not the opening statement is a brace, in which case it will parse
     * through that.
     *
     * @param tokens List of the tokens which are to be parsed. These are
     *               mutated during the function call to remove the already-
     *               parsed tokens
     * @return List of ArgumentNodes representing the parsed tokens
     */
    @NotNull
    static ArgumentNode[] parseList(@NotNull TokenList tokens) {
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
            if (tokens.tokenIs(offset, TokenType.NAME)
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
