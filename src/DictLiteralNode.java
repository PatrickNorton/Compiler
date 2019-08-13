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
}
