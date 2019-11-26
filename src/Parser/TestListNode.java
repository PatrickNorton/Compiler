package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class TestListNode implements BaseNode {
    private TestNode[] tests;
    private String[] varargs;

    @Contract(pure = true)
    public TestListNode() {
        this(new TestNode[0], new String[0]);
    }

    @Contract(pure = true)
    public TestListNode(@NotNull TestNode[] tests, @NotNull String[] varargs) {
        assert tests.length == varargs.length;
        this.tests = tests;
        this.varargs = varargs;
    }

    @Override
    public LineInfo getLineInfo() {
        return LineInfo.empty();
    }

    /**
     * Parse a list of TestNodes from a list of tokens.
     * @param tokens The list of tokens to be destructively parsed
     * @param ignore_newlines Whether or not to ignore newlines
     * @return The list of TestNodes
     */
    @NotNull
    static TestListNode parse(@NotNull TokenList tokens, boolean ignore_newlines) {
        return parse(tokens, ignore_newlines, false);
    }

    @NotNull
    @Contract("_, _, _ -> new")
    static TestListNode parse(TokenList tokens, boolean ignoreNewlines, boolean noTernary) {
        if (!ignoreNewlines && tokens.tokenIs(TokenType.NEWLINE)) {
            return new TestListNode();
        }
        List<TestNode> tests = new ArrayList<>();
        List<String> varargs = new ArrayList<>();
        while (TestNode.nextIsTest(tokens)) {
            if (tokens.tokenIs("*", "**")) {
                varargs.add(tokens.tokenSequence());
                tokens.nextToken(ignoreNewlines);
            } else {
                varargs.add("");
            }
            tests.add(noTernary ? TestNode.parseNoTernary(tokens, ignoreNewlines) : TestNode.parse(tokens, ignoreNewlines));
            if (!tokens.tokenIs(TokenType.COMMA)) {
                break;
            }
            tokens.nextToken(ignoreNewlines);
        }
        return new TestListNode(tests.toArray(new TestNode[0]), varargs.toArray(new String[0]));
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", ");
        for (int i = 0; i < tests.length; i++) {
            sj.add(varargs[i] + tests[i]);
        }
        return sj.toString();
    }
}
