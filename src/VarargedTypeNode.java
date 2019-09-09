import org.jetbrains.annotations.Contract;
// TODO: Delete me
/**
 * The class representing a vararged type.
 * <p>
 *     The parser for this is in {@link TypeNode#parse}, so see that instead.
 *     The syntax for a VarargedTypeNode is <code>["*"] {@link
 *     TypeNode}</code>.
 * </p>
 */
public class VarargedTypeNode implements SubTestNode {
    boolean vararg;
    TypeNode type;

    @Contract(pure = true)
    public VarargedTypeNode(boolean is_vararg, TypeNode type) {
        this.vararg = is_vararg;
        this.type = type;
    }

    public boolean isVararg() {
        return vararg;
    }

    public TypeNode getType() {
        return type;
    }
}
