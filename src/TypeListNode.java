/**
 * The (unused) TypeListNode
 * @author Patrick Norton
 */
// TODO: Remove me
public class TypeListNode implements BaseNode {
    private VarargedTypeNode[] types;

    public TypeListNode(VarargedTypeNode[] types) {
        this.types = types;
    }

    public VarargedTypeNode[] getTypes() {
        return types;
    }
}
