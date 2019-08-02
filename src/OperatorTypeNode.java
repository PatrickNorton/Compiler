public class OperatorTypeNode implements AtomicNode {
    private String name;

    public OperatorTypeNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
