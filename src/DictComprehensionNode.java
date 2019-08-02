public class DictComprehensionNode implements SubTestNode {
    private TestNode[] keys;
    private TestNode[] values;

    public DictComprehensionNode(TestNode[] keys, TestNode[] values) {
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
