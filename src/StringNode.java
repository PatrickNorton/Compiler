public class StringNode implements AtomicNode {
    private String contents;

    public StringNode(String contents) {
        if (contents.startsWith("\"") && contents.endsWith("\"")) {
            contents = contents.substring(1, contents.length()-1);
            this.contents = contents;
        }
    }

    public String getContents() {
        return contents;
    }
}
