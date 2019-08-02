public class TypedArgumentListNode implements BaseNode {
    private TypedArgumentNode[] args;

    public TypedArgumentListNode(TypedArgumentNode... args) {
        this.args = args;
    }

    public TypedArgumentNode[] getArgs() {
        return args;
    }

    public TypedArgumentNode get(int index) {
        return args[index];
    }
}
