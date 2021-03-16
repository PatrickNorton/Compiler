package main.java.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    PROTECTED("protected"),
    PUBGET("pubget"),
    STATIC("static"),
    MUT("mut"),
    MREF("mref"),
    READONLY("readonly"),
    FINAL("final"),
    NONFINAL("nonfinal"),
    NATIVE("native"),
    GENERATOR("generator"),
    SYNCED("synced"),
    CONST("const"),
    ;

    public static final Pattern PATTERN = Pattern.compile("^(" +
            Arrays.stream(values())
                    .map(Object::toString)
                    .collect(Collectors.joining("|"))
            + ")\\b"
    );

    public final String name;

    public static final Set<DescriptorNode> MUT_NODES = Collections.unmodifiableSet(EnumSet.of(MUT, MREF, READONLY, FINAL));

    private static final Map<String, DescriptorNode> values;

    private static final Set<DescriptorNode> ACCESS = Collections.unmodifiableSet(EnumSet.of(PUBLIC, PRIVATE, PUBGET, PROTECTED));
    private static final Set<DescriptorNode> STATIC_SET = Collections.unmodifiableSet(EnumSet.of(STATIC));
    private static final Set<DescriptorNode> MOD_SET = Collections.unmodifiableSet(EnumSet.of(MUT, MREF, READONLY));
    private static final Set<DescriptorNode> FINAL_SET = Collections.unmodifiableSet(EnumSet.of(FINAL, NONFINAL));
    private static final Set<DescriptorNode> NATIVE_SET = Collections.unmodifiableSet(EnumSet.of(NATIVE));
    private static final Set<DescriptorNode> GENERATOR_SET = Collections.unmodifiableSet(EnumSet.of(GENERATOR));
    private static final Set<DescriptorNode> SYNCED_SET = Collections.unmodifiableSet(EnumSet.of(SYNCED));
    private static final Set<DescriptorNode> CONST_SET = Collections.unmodifiableSet(EnumSet.of(CONST));

    private static final List<Set<DescriptorNode>> SETS = List.of(
            ACCESS, STATIC_SET, MOD_SET, FINAL_SET, NATIVE_SET, GENERATOR_SET, SYNCED_SET, CONST_SET
    );

    static final Set<DescriptorNode> DEFINITION_VALID = Collections.unmodifiableSet(
            EnumSet.of(PUBLIC, PRIVATE, PROTECTED, MUT, FINAL, NONFINAL, STATIC, NATIVE, CONST));
    static final Set<DescriptorNode> FUNCTION_VALID = Collections.unmodifiableSet(
            EnumSet.of(GENERATOR, SYNCED, NATIVE));
    static final Set<DescriptorNode> DECLARATION_VALID = Collections.unmodifiableSet(
            EnumSet.of(PUBLIC, PRIVATE, PUBGET, PROTECTED, MUT, MREF, READONLY, FINAL, STATIC, NATIVE));
    static final Set<DescriptorNode> CONTEXT_VALID = Collections.unmodifiableSet(
            EnumSet.of(PUBLIC, PRIVATE, PROTECTED, MUT, GENERATOR, STATIC, FINAL, SYNCED, NATIVE));
    static final Set<DescriptorNode> METHOD_VALID = Collections.unmodifiableSet(
            EnumSet.of(PUBLIC, PRIVATE, PROTECTED, STATIC, MUT, FINAL, NONFINAL, GENERATOR, SYNCED, NATIVE));
    static final Set<DescriptorNode> STATIC_BLOCK_VALID = Collections.unmodifiableSet(
            EnumSet.noneOf(DescriptorNode.class));

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

    @NotNull
    @Contract(pure = true)
    @Override
    public LineInfo getLineInfo() {
        return LineInfo.empty();
    }

    /**
     * Get a DescriptorNode based on the string given.
     * @param type The sequence corresponding to the descriptor
     * @return The descriptor
     */
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
        DescriptorNode descriptor = find(tokens.tokenSequence());
        if (descriptor == null) {
            throw tokens.internalError("Unknown descriptor " + tokens.getFirst());
        }
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
            LineInfo info = tokens.lineInfo();
            DescriptorNode d = parse(tokens);
            do {
                setsNum++;
                if (setsNum > SETS.size()) {
                    throw ParserException.of("Illegal descriptor combination", info);
                }
            } while (!SETS.get(setsNum - 1).contains(d));
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

    @Contract(pure = true)
    static Optional<Integer> pattern(String input) {
        for (var node : values()) {
            if (input.startsWith(node.name)) {
                if (Character.isUnicodeIdentifierPart(input.charAt(node.name.length()))) {
                    return Optional.empty();
                }
                return Optional.of(node.name.length());
            }
        }
        return Optional.empty();
    }

    @Contract(pure = true)
    @Override
    public String toString() {
        return name;
    }

    static String join(@NotNull EnumSet<DescriptorNode> values) {
        if (values.isEmpty()) return "";
        StringJoiner sj = new StringJoiner(" ", "", " ");
        for (DescriptorNode d : values) {
            sj.add(d.toString());
        }
        return sj.toString();
    }
}
