import java.util.ArrayList;

public class VariableNode implements NameNode {
    private String[] names;
    private ArrayList<TestNode[]> indices;

    public VariableNode(String... names) {
        this.names = names;
        this.indices = new ArrayList<>();
    }

    public VariableNode(String[] names, ArrayList<TestNode[]> indices) {
        this.names = names;
        this.indices = indices;
    }

    public String[] getNames() {
        return names;
    }

    public ArrayList<TestNode[]> getIndices() {
        return indices;
    }

    public boolean isIndexed() {
        return indices.size() > 0;
    }

    public boolean isDotted() {
        return names.length > 1;
    }

    public boolean isEmpty() {
        return names.length > 0;
    }
}
