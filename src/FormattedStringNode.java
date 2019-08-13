public class FormattedStringNode implements AtomicNode {
    private String[] strs;
    private TestNode[] tests;

    public FormattedStringNode(String[] strs, TestNode[] tests) {
        this.strs = strs;
        this.tests = tests;
    }

    public String[] getStrs() {
        return strs;
    }

    public TestNode[] getTests() {
        return tests;
    }
}
