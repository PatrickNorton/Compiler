public class SliceNode implements SubTestNode {
    private TestNode start;
    private TestNode end;
    private TestNode step;

    public SliceNode(TestNode start, TestNode end, TestNode step) {
        this.start = start;
        this.end = end;
        this.step = step;
    }

    public TestNode getStart() {
        return start;
    }

    public TestNode getEnd() {
        return end;
    }

    public TestNode getStep() {
        return step;
    }

    static SliceNode parse(TokenList tokens) {
        assert tokens.tokenIs("[");
        tokens.nextToken(true);
        TestNode start;
        if (tokens.tokenIs(":")) {
            start = null;
        } else {
            start = TestNode.parse(tokens, true);
        }
        if (tokens.tokenIs("]")) {
            tokens.nextToken();
            return new SliceNode(start, null, null);
        }
        TestNode end = sliceTest(tokens);
        if (tokens.tokenIs("]")) {
            tokens.nextToken();
            return new SliceNode(start, end, null);
        }
        TestNode step = sliceTest(tokens);
        if (!tokens.tokenIs("]")) {
            throw new ParserException("Expected ], got "+tokens.getFirst());
        }
        tokens.nextToken();
        return new SliceNode(start, end, step);
    }

    private static TestNode sliceTest(TokenList tokens) {
        if (!tokens.tokenIs(":")) {
            throw new ParserException("Expected :, got "+tokens.getFirst());
        }
        tokens.nextToken(true);
        if (tokens.tokenIs(":", "]")) {
            return null;
        } else {
            return TestNode.parse(tokens, true);
        }
    }
}
