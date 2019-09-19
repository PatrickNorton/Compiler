package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
    FINAL("final"),
    GENERATOR("generator"),
    ;
    public final String name;

    private static final Map<String, DescriptorNode> values;

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

    public static DescriptorNode find(String type) {
        return values.get(type);
    }

    @NotNull
    public static DescriptorNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(TokenType.DESCRIPTOR);
        DescriptorNode descriptor = find(tokens.getFirst().sequence);
        tokens.nextToken();
        return descriptor;
    }

    @NotNull
    public static DescriptorNode[] parseList(@NotNull TokenList tokens) {
        ArrayList<DescriptorNode> descriptors = new ArrayList<>();
        while (tokens.tokenIs(TokenType.DESCRIPTOR)) {
            descriptors.add(parse(tokens));
        }
        return descriptors.toArray(new DescriptorNode[0]);
    }

    public static int count(@NotNull TokenList tokens) {
        int number = 0;
        while (tokens.tokenIs(number, TokenType.DESCRIPTOR)) {
            number++;
        }
        return number;
    }

    @Contract(pure = true)
    @Override
    public String toString() {
        return name;
    }
}
