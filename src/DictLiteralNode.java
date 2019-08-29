import java.util.LinkedList;

public class DictLiteralNode implements SubTestNode {
    private TestNode[] keys;
    private TestNode[] values;

    public DictLiteralNode(TestNode[] keys, TestNode[] values) {
        this.keys = keys;
        this.values = values;
    }

    public TestNode[] getKeys() {
        return keys;
    }

    public TestNode[] getValues() {
        return values;
    }

    static DictLiteralNode parse(TokenList tokens) {
        assert tokens.tokenIs("{");
        tokens.nextToken(true);
        LinkedList<TestNode> keys = new LinkedList<>();
        LinkedList<TestNode> values = new LinkedList<>();
        while (true) {
            keys.add(TestNode.parse(tokens));
            if (!tokens.tokenIs(":")) {
                throw new ParserException("Dict comprehension must have colon");
            }
            tokens.nextToken(true);
            values.add(TestNode.parse(tokens));
            if (!tokens.tokenIs(",")) {
                break;
            }
            tokens.nextToken(true);
            if (tokens.tokenIs("}")) {
                break;
            }
        }
        if (!tokens.tokenIs("}")) {
            throw new ParserException("Unmatched brace");
        }
        tokens.nextToken();
        return new DictLiteralNode(keys.toArray(new TestNode[0]), values.toArray(new TestNode[0]));
    }
}
