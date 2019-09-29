package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
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

    private static final EnumSet<DescriptorNode> ACCESS = EnumSet.of(PUBLIC, PRIVATE, PUBGET);
    private static final EnumSet<DescriptorNode> STATIC_SET = EnumSet.of(STATIC);
    private static final EnumSet<DescriptorNode> CONST_SET = EnumSet.of(CONST);
    private static final EnumSet<DescriptorNode> FINAL_SET = EnumSet.of(FINAL);
    private static final EnumSet<DescriptorNode> GENERATOR_SET = EnumSet.of(GENERATOR);

    private static final List<EnumSet<DescriptorNode>> SETS;

    static final EnumSet<DescriptorNode> DEFINITION_VALID = EnumSet.of(PUBLIC, PRIVATE, CONST, FINAL, STATIC);
    static final EnumSet<DescriptorNode> FUNCTION_VALID = EnumSet.of(GENERATOR);
    static final EnumSet<DescriptorNode> DECLARATION_VALID = EnumSet.of(PUBLIC, PRIVATE, PUBGET, CONST, FINAL, STATIC);
    static final EnumSet<DescriptorNode> CONTEXT_VALID = EnumSet.of(PUBLIC, PRIVATE, GENERATOR, STATIC, FINAL);
    static final EnumSet<DescriptorNode> METHOD_VALID = EnumSet.of(PUBLIC, PRIVATE, STATIC, CONST, FINAL, GENERATOR);
    static final EnumSet<DescriptorNode> STATIC_BLOCK_VALID = EnumSet.noneOf(DescriptorNode.class);

    static {
        SETS = List.of(ACCESS, STATIC_SET, CONST_SET, FINAL_SET, GENERATOR_SET);
    }

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

    /**
     * Parse a DescriptorNode from a list of tokens.
     * @param tokens The list of tokens to be destructively parsed
     * @return The parsed DescriptorNode
     */
    @NotNull
    public static DescriptorNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(TokenType.DESCRIPTOR);
        DescriptorNode descriptor = find(tokens.getFirst().sequence);
        tokens.nextToken();
        return descriptor;
    }

    /**
     * Parse a list of DescriptorNodes from a list of tokens.
     * @param tokens The list of tokens to be parsed
     * @return The array of DescriptorNodes being parsed
     */
    @NotNull
    public static EnumSet<DescriptorNode> parseList(@NotNull TokenList tokens) {
        int setsNum = 0;
        EnumSet<DescriptorNode> descriptors = EnumSet.noneOf(DescriptorNode.class);
        while (tokens.tokenIs(TokenType.DESCRIPTOR)) {
            DescriptorNode d = parse(tokens);
            do {
                setsNum++;
                if (setsNum > SETS.size()) {
                    throw new ParserException("Illegal keyword placement");
                }
            } while (SETS.get(setsNum - 1).contains(d));
            descriptors.add(d);
        }
        return descriptors;
    }

    /**
     * Returns an empty EnumSet for descriptors
     * @return The empty set
     */
    @NotNull
    public static EnumSet<DescriptorNode> emptySet() {
        return EnumSet.noneOf(DescriptorNode.class);
    }

    /**
     * Get the amount of descriptors upcoming in a list of tokens.
     * @param tokens The list of tokens to be checked for descriptors
     * @return The number of descriptors upcoming
     */
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
