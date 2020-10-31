package main.java.parser;

import main.java.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringJoiner;

public class TestListNode implements BaseNode, Iterable<Pair<TestNode, String>> {
    private TestNode[] tests;
    private String[] varargs;

    public TestListNode() {
        this(new TestNode[0], new String[0]);
    }

    public TestListNode(TestNode... tests) {
        this(tests, nEmpties(tests.length));
    }

    public TestListNode(TestNode[] tests,String[] varargs) {
        assert tests.length == varargs.length;
        this.tests = tests;
        this.varargs = varargs;
    }

    @Override
    public LineInfo getLineInfo() {
        return LineInfo.empty();
    }

    public int size() {
        return tests.length;
    }

    public boolean isEmpty() {
        return tests.length == 0;
    }

    public TestNode get(int index) {
        return tests[index];
    }

    public String getVararg(int index) {
        return varargs[index];
    }

    @Override
    public Iterator<Pair<TestNode, String>> iterator() {
        return new TestListIterator(tests, varargs);
    }

    /**
     * Parse a list of TestNodes from a list of tokens.
     * @param tokens The list of tokens to be destructively parsed
     * @param ignoreNewlines Whether or not to ignore newlines
     * @return The list of TestNodes
     */

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

    private static final class TestListIterator implements Iterator<Pair<TestNode, String>> {
        private int index;
        private final TestNode[] tests;
        private final String[] varargs;

        public TestListIterator(TestNode[] tests,String[] varargs) {
            assert tests.length == varargs.length;
            this.index = 0;
            this.tests = tests;
            this.varargs = varargs;
        }

        @Override
        public boolean hasNext() {
            return index < tests.length && index < varargs.length;
        }

        @Override

        public Pair<TestNode, String> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            var pair = Pair.of(tests[index], varargs[index]);
            index++;
            return pair;
        }
    }

    private static String[] nEmpties(int count) {
         var result = new String[count];
         Arrays.fill(result, "");
         return result;
    }
}
