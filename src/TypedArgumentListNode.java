import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.StringJoiner;

/**
 * The class representing a typed argument list.
 * @author Patrick Norton
 * @see TypedArgumentNode
 */
public class TypedArgumentListNode implements BaseNode {
    private TypedArgumentNode[] positionArgs;
    private TypedArgumentNode[] normalArgs;
    private TypedArgumentNode[] nameArgs;

    @Contract(pure = true)
    public TypedArgumentListNode(TypedArgumentNode... args) {
        this.normalArgs = args;
        this.positionArgs = new TypedArgumentNode[0];
        this.nameArgs = new TypedArgumentNode[0];
    }

    @Contract(pure = true)
    public TypedArgumentListNode(TypedArgumentNode[] positionArgs, TypedArgumentNode[] normalArgs, TypedArgumentNode[] nameArgs) {
        this.normalArgs = normalArgs;
        this.positionArgs = positionArgs;
        this.nameArgs = nameArgs;
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

    public boolean isEmpty() {
        return positionArgs.length + nameArgs.length + normalArgs.length > 0;
    }

    /**
     * Parse a typed argument list if and only if the next token is of the type
     * specified.
     * <p>
     *     This will <i>not</i> parse the token given, as it is currently
     *     in use as an open-paren tester. This <b>may</b> change in the future.
     *     For the grammar, see {@link TypedArgumentListNode#parse}
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @param tester The values to test for
     * @return The freshly parsed TypedArgumentListNode
     */
    @NotNull
    static TypedArgumentListNode parseOnToken(@NotNull TokenList tokens, String... tester) {
        if (tokens.tokenIs(tester)) {
            return parse(tokens);
        } else {
            return new TypedArgumentListNode();
        }
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
        assert tokens.tokenIs("(");
        boolean has_posArgs = tokens.braceContains("/");
        if (!tokens.tokenIs("(")) {
            throw new ParserException("Argument lists must start with an open-paren");
        }
        tokens.nextToken(true);
        ArrayList<TypedArgumentNode> posArgs = new ArrayList<>();
        ArrayList<TypedArgumentNode> args = new ArrayList<>();
        ArrayList<TypedArgumentNode> kwArgs = new ArrayList<>();
        if (has_posArgs) {
            while (!tokens.tokenIs("/")) {
                posArgs.add(TypedArgumentNode.parse(tokens));
                if (tokens.tokenIs(TokenType.COMMA)) {
                    tokens.nextToken(true);
                }
            }
            tokens.nextToken();
            if (tokens.tokenIs(TokenType.COMMA)) {
                tokens.nextToken(true);
            } else if (!tokens.tokenIs(TokenType.CLOSE_BRACE)) {
                throw new ParserException("Unexpected " + tokens.getFirst());
            }
        }
        ArrayList<TypedArgumentNode> which_args = args;
        while (!tokens.tokenIs(")")) {
            if (tokens.tokenIs("*") && tokens.getToken(1).is(",", ")")) {
                which_args = kwArgs;
                tokens.nextToken(true);
                tokens.nextToken(true);
                continue;
            }
            which_args.add(TypedArgumentNode.parse(tokens));
            tokens.passNewlines();
            if (tokens.tokenIs(TokenType.COMMA)) {
                tokens.nextToken(true);
                continue;
            }
            if (!tokens.tokenIs(")")) {
                throw new ParserException("Comma must separate arguments");
            }
        }
        tokens.nextToken();
        return new TypedArgumentListNode(posArgs.toArray(new TypedArgumentNode[0]), args.toArray(new TypedArgumentNode[0]),
                kwArgs.toArray(new TypedArgumentNode[0]));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (positionArgs.length > 0) {
            StringJoiner sj = new StringJoiner(", ", "", ", /,");
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
