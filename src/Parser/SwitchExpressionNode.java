package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a switch expression.
 * <p>
 *     A switch expression is not the same as a {@link SwitchStatementNode
 *     switch statement}. A switch expression returns a value, and is an
 *     instance of {@link TestNode}, whereas a switch statement is not.
 * </p>
 *
 * @author Patrick Norton
 * @see SwitchLikeNode
 * @see SwitchExpressionNode
 */
public class SwitchExpressionNode extends SwitchLikeNode implements TestNode {

    @Contract(pure = true)
    public SwitchExpressionNode(LineInfo lineInfo, TestNode switched, CaseStatementNode[] cases,
                                DefaultStatementNode defaultStatement) {
        super(lineInfo, switched, false, cases, defaultStatement);
    }

    /**
     * Parse a switch expression from a list of tokens.
     * <p>
     *     The syntax for a switch expression is: <code>"switch" {@link
     *     TestNode} "{" *("case" "=>" {@link TestNode} *("," {@link TestNode})
     *     [","] *NEWLINE) "}"</code>.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed SwitchExpressionNode
     */
    @NotNull
    public static SwitchExpressionNode parse(@NotNull TokenList tokens) {
        return (SwitchExpressionNode) parse(tokens, true, true);
    }
}
