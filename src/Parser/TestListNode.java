package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import util.Pair;

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
     * @param ignoreNewlines Whether or not to ignore newlines
     * @return The list of TestNodes
     */@NotNull
    @Contract("_, _ -> new")
    static TestListNode parse(TokenList tokens, boolean ignoreNewlines) {
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
            tests.add(TestNode.parse(tokens, ignoreNewlines));
            if (!tokens.tokenIs(TokenType.COMMA)) {
                break;
            }
            tokens.nextToken(ignoreNewlines);
        }
        return new TestListNode(tests.toArray(new TestNode[0]), varargs.toArray(new String[0]));
    }

    @NotNull
    @Contract("_, _ -> new")
    static Pair<TestListNode, TestNode> parsePostIf(TokenList tokens, boolean ignoreNewlines) {
        if (!ignoreNewlines && tokens.tokenIs(TokenType.NEWLINE)) {
            return Pair.of(new TestListNode(), null);
        }
        List<TestNode> tests = new ArrayList<>();
        List<String> varargs = new ArrayList<>();
        TestNode postIf = TestNode.empty();
        while (TestNode.nextIsTest(tokens) || tokens.tokenIs(Keyword.IF)) {
            if (tokens.tokenIs(Keyword.IF)) {
                tokens.nextToken(ignoreNewlines);
                postIf = TestNode.parse(tokens, ignoreNewlines);
                break;
            }
            if (tokens.tokenIs("*", "**")) {
                varargs.add(tokens.tokenSequence());
                tokens.nextToken(ignoreNewlines);
            } else {
                varargs.add("");
            }
            Pair<TestNode, TestNode> next = TestNode.parseMaybePostIf(tokens, ignoreNewlines);
            tests.add(next.getKey());
            if (!next.getValue().isEmpty()) {
                postIf = next.getValue();
                break;
            }
            if (!tokens.tokenIs(TokenType.COMMA)) {
                break;
            }
            tokens.nextToken(ignoreNewlines);
        }
        TestListNode node = new TestListNode(tests.toArray(new TestNode[0]), varargs.toArray(new String[0]));
        return Pair.of(node, postIf);
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
