package Parser;

public class EmptyTestNode implements TestNode {
    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public String toString() {
        return "";
    }
}
