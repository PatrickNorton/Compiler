import java.util.LinkedList;

public class TopNode implements BaseNode {
    private LinkedList<BaseNode> nodes;

    public TopNode(LinkedList<BaseNode> nodes) {
        this.nodes = nodes;
    }

    public TopNode() {
        this.nodes = new LinkedList<>();
    }

    public void add(BaseNode operand) {
        nodes.add(operand);
    }

    public LinkedList<BaseNode> getNodes() {
        return nodes;
    }
}
