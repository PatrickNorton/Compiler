package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

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
    private LineInfo lineInfo;
    private VariableNode variable;
    private String vararg;
    private TestNode argument;

    /**
     * Create new instance of ArgumentNode.
     * @param lineInfo The info regarding which line it is on
     * @param variable The variable which is used as a keyword in the call
     * @param vararg Whether or not there is a vararg, and what the vararg is
     *               (e.g. * vs **)
     * @param argument What the actual argument is, e.g. what is actually
     *                 passed to the function in the end
     */
    @Contract(pure = true)
    public ArgumentNode(LineInfo lineInfo, VariableNode variable, String vararg, TestNode argument) {
        this.lineInfo = lineInfo;
        this.variable = variable;
        this.vararg = vararg;
        this.argument = argument;
    }

    /**
     * Create a new instance of ArgumentNode.
     * @param variable The variable which is used as a keyword in the call
     * @param vararg Whether or not there is a vararg, and what the vararg is
     *               (e.g. * vs **)
     * @param argument What the actual argument is, e.g. what is actually
     *                 passed to the function in the end
     */
    public ArgumentNode(VariableNode variable, String vararg, TestNode argument) {
        this(variable.getLineInfo(), variable, vararg, argument);
    }

    @Contract(pure = true)
    public ArgumentNode(LineInfo lineInfo, TestNode argument) {
        this(lineInfo, VariableNode.empty(), "", argument);
    }

    public ArgumentNode(TestNode argument) {
        this(argument.getLineInfo(), argument);
    }

    @NotNull
    public static ArgumentNode[] fromTestNodes(@NotNull TestNode... testNodes) {
        ArgumentNode[] args = new ArgumentNode[testNodes.length];
        for (int i = 0; i < testNodes.length; i++) {
            args[i] = new ArgumentNode(testNodes[i]);
        }
        return args;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
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
            throw tokens.error("Function call must start with open-paren");
        }
        if (tokens.braceContains(Keyword.FOR)) {
            return new ArgumentNode[] {new ArgumentNode(ComprehensionNode.parse(tokens))};
        }
        tokens.nextToken(true);
        if (tokens.tokenIs(")")) {
            tokens.nextToken();
            return new ArgumentNode[0];
        }
        ArgumentNode[] args = parseBraceFreeList(tokens);
        if (!tokens.tokenIs(")")) {
            throw tokens.error("Unexpected " + tokens.getFirst());
        }
        tokens.nextToken();
        return args;
    }

    @NotNull
    static ArgumentNode[] parseBraceFreeList(@NotNull TokenList tokens) {
        List<ArgumentNode> args = new ArrayList<>();
        while (tokens.tokenIs("*", "**") || TestNode.nextIsTest(tokens)) {
            VariableNode var = VariableNode.empty();
            int offset = tokens.tokenIs("*", "**") ? 1 : 0;
            if (tokens.tokenIs(offset, TokenType.NAME)
                    && tokens.tokenIs(tokens.sizeOfVariable(offset), "=")) {
                var = VariableNode.parse(tokens);
                tokens.nextToken(true);
            }
            String vararg;
            if (tokens.tokenIs("*", "**")) {
                vararg = tokens.tokenSequence();
                tokens.nextToken(true);
            } else {
                vararg = "";
            }
            TestNode argument = TestNode.parse(tokens, true);
            args.add(new ArgumentNode(var, vararg, argument));
            if (!tokens.tokenIs(",")) {
                break;
            }
            tokens.nextToken(true);
        }
        return args.toArray(new ArgumentNode[0]);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(vararg);
        if (!variable.isEmpty()) {
            sb.append(variable).append("=");
        }
        sb.append(argument);
        return sb.toString();
    }

    public static String toString(@NotNull ArgumentNode... values) {
        StringJoiner sj = new StringJoiner(", ");
        for (ArgumentNode v : values) {
            sj.add(v.toString());
        }
        return sj.toString();
    }
}
