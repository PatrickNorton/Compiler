public class StringNode implements AtomicNode {
    private String contents;
    private char[] prefixes;

    public StringNode(String contents, char... prefixes) {
        this.contents = contents;
        this.prefixes = prefixes;
    }

    public String getContents() {
        return contents;
    }

    public char[] getPrefixes() {
        return prefixes;
    }
}
