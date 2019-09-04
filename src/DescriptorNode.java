import org.jetbrains.annotations.Contract;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// TODO: Make me an enum

/**
 * The node representing all descriptors.
 * <p>
 *     This node is used for the descriptor types in such things as {@link
 *     ClassStatementNode} and {@link InterfaceStatementNode}, which are the
 *     only statements which may be preceded by descriptors.
 * </p>
 *
 * @author Patrick Norton
 * @see InterfaceStatementNode
 * @see ClassStatementNode
 */
public enum DescriptorNode implements AtomicNode {
    PUBLIC("public"),
    PRIVATE("private"),
    PUBGET("pubget"),
    STATIC("static"),
    CONST("const"),
    CLASS("class"),
    FINAL("final"),
    GENERATOR("generator"),
    ;
    public final String name;

    private static Map<String, DescriptorNode> values;

    static {
        Map<String, DescriptorNode> temp = new HashMap<>();
        for (DescriptorNode d : DescriptorNode.values()) {
            temp.put(d.name, d);
        }
        values = Collections.unmodifiableMap(temp);
    }

    @Contract(pure = true)
    DescriptorNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static DescriptorNode find(String type) {
        return values.get(type);
    }
}
