public class LiteralNode implements SubTestNode {
    private String brace_type;
    private TestNode[] builders;
    private Boolean[] is_splats;

    public LiteralNode(String brace_type, TestNode[] builders, Boolean[] is_splats) {
        this.brace_type = brace_type;
        this.builders = builders;
        this.is_splats = is_splats;
    }

    public String getBrace_type() {
        return brace_type;
    }

    public TestNode[] getBuilders() {
        return builders;
    }

    public Boolean[] getIs_splats() {
        return is_splats;
    }
}
