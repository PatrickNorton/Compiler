public class VariableNode implements NameNode {
    private String name;

    public VariableNode(String names) {
        this.name = names;
    }

    public VariableNode() {
        this.name = "";
    }

    public String getName() {
        return name;
    }

    public boolean isEmpty() {
        return this.name.isEmpty();
    }
}
