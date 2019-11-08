package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * The class representing a typed argument list.
 * @author Patrick Norton
 * @see TypedArgumentNode
 */
public class TypedArgumentListNode implements BaseNode, EmptiableNode {
    private LineInfo lineInfo;
    private TypedArgumentNode[] positionArgs;
    private TypedArgumentNode[] normalArgs;
    private TypedArgumentNode[] nameArgs;

    @Contract(pure = true)
    public TypedArgumentListNode(LineInfo lineInfo, TypedArgumentNode... args) {
        this(lineInfo, new TypedArgumentNode[0], args, new TypedArgumentNode[0]);
    }

    @Contract(pure = true)
    public TypedArgumentListNode(LineInfo lineInfo, TypedArgumentNode[] positionArgs, TypedArgumentNode[] normalArgs,
                                 TypedArgumentNode[] nameArgs) {
        this.lineInfo = lineInfo;
        this.normalArgs = normalArgs;
        this.positionArgs = positionArgs;
        this.nameArgs = nameArgs;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public TypedArgumentNode[] getPositionArgs() {
        return positionArgs;
    }

    public TypedArgumentNode[] getArgs() {
        return normalArgs;
    }

    public TypedArgumentNode[] getNameArgs() {
        return nameArgs;
    }

    public TypedArgumentNode get(int index) {
        return normalArgs[index];
    }

    @Override
    public boolean isEmpty() {
        return positionArgs.length + nameArgs.length + normalArgs.length == 0;
    }

    /**
     * Parse a typed argument list if and only if the next token is an open
     * brace.
     * <p>
     *     For grammar, see {@link #parse(TokenList)}
     * </p>
     *
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed TypedArgumentListNode
     */
    static TypedArgumentListNode parseOnOpenBrace(@NotNull TokenList tokens) {
        return tokens.tokenIs("(") ? parse(tokens, false) : empty();
    }

    @NotNull
    @Contract(" -> new")
    static TypedArgumentListNode empty() {
        return new TypedArgumentListNode(LineInfo.empty());
    }

    /**
     * Parse a TypedArgumentListNode from a list of tokens.
     * <p>
     *     The syntax for a list of typed arguments is: <code>"(" {@link
     *     TypedArgumentNode} *("," {@link TypedArgumentNode}) ["," "/"] *(","
     *     {@link TypedArgumentNode}) ["," "*"] *("," {@link
     *     TypedArgumentNode}) [","] ")"</code>.
     * </p>
     * @param tokens The list of tokens to be parsed destructively
     * @return The freshly parsed TypedArgumentListNode
     */
    @NotNull
    @Contract("_ -> new")
    static TypedArgumentListNode parse(@NotNull TokenList tokens) {
        return parse(tokens, false);
    }

    /**
     * Parse a TypedArgumentListNode from a list of tokens, possibly allowing
     * dynamically-typed arguments.
     * <p>
     *     The syntax for a list of typed arguments is: <code>"(" {@link
     *     TypedArgumentNode} *("," {@link TypedArgumentNode}) ["," "/"] *(","
     *     {@link TypedArgumentNode}) ["," "*"] *("," {@link
     *     TypedArgumentNode}) [","] ")"</code>.
     * </p>
     *
     * @param tokens The list of tokens to be parsed destructively
     * @param allowUntyped Whether or not to allow untyped arguments
     * @return The freshly parsed TypedArgumentListNode
     */
    @NotNull
    @Contract("_, _ -> new")
    private static TypedArgumentListNode parse(@NotNull TokenList tokens, boolean allowUntyped) {
        assert tokens.tokenIs("(");
        LineInfo info = tokens.lineInfo();
        tokens.nextToken(true);
        TypedArgumentListNode list = parseInsideParens(tokens, info, allowUntyped);
        if (!tokens.tokenIs(")")) {
            throw tokens.error("Unexpected " + tokens.getFirst());
        }
        tokens.nextToken();
        return list;
    }

    @NotNull
    @Contract("_, _, _ -> new")
    private static TypedArgumentListNode parseInsideParens(TokenList tokens, LineInfo info, boolean allowUntyped) {
        boolean untypedDecided = !allowUntyped;
        List<TypedArgumentNode> posArgs = new ArrayList<>(0);  // Never used, so don't alloc memory for it
        List<TypedArgumentNode> args = new ArrayList<>();
        List<TypedArgumentNode> kwArgs = new ArrayList<>();
        List<TypedArgumentNode> currentArgList = args;
        while (TypeNode.nextIsType(tokens)) {
            if (tokens.tokenIs("/")) {
                if (!posArgs.isEmpty() || currentArgList == kwArgs) {
                    throw tokens.error("Illegal use of name-only token");
                }
                posArgs = args;
                args = new ArrayList<>();
                currentArgList = args;
                tokens.nextToken(true);
            } else {
                if (tokens.tokenIs("*") && currentArgList != kwArgs) {
                    TypedArgumentNode next = TypedArgumentNode.parseAllowingEmpty(tokens, allowUntyped, untypedDecided);
                    if (next == null) {
                        currentArgList = kwArgs;
                    } else {
                        currentArgList.add(next);
                        if (!untypedDecided) {
                            allowUntyped = !currentArgList.get(currentArgList.size() - 1).getType().isDecided();
                        }
                    }
                } else {
                    if (!untypedDecided) {
                        allowUntyped = TypedArgumentNode.argumentIsUntyped(tokens);
                        untypedDecided = true;
                    }
                    currentArgList.add(TypedArgumentNode.parse(tokens, !allowUntyped));
                    tokens.passNewlines();
                }
            }
            if (!tokens.tokenIs(TokenType.COMMA)) {
                break;
            }
            tokens.nextToken(true);
        }
        return new TypedArgumentListNode(info, posArgs.toArray(new TypedArgumentNode[0]),
                args.toArray(new TypedArgumentNode[0]), kwArgs.toArray(new TypedArgumentNode[0]));
    }

    static TypedArgumentListNode parseOptionalParens(@NotNull TokenList tokens) {
        return tokens.tokenIs("(")
                ? parse(tokens, true)
                : parseInsideParens(tokens, tokens.lineInfo(), true);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (positionArgs.length > 0) {
            StringJoiner sj = new StringJoiner(", ", "", ", /");
            for (TypedArgumentNode t : positionArgs) {
                sj.add(t.toString());
            }
            sb.append(sj);
        }
        if (normalArgs.length > 0) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            StringJoiner sj = new StringJoiner(", ");
            for (TypedArgumentNode t : normalArgs) {
                sj.add(t.toString());
            }
            sb.append(sj);
        }
        if (nameArgs.length > 0) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            StringJoiner sj = new StringJoiner(", ", "*, ", "");
            for (TypedArgumentNode t : nameArgs) {
                sj.add(t.toString());
            }
            sb.append(sj);
        }
        return "(" + sb + ")";
    }
}
