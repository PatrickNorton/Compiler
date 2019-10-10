package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

/**
 * The class representing a switch statement.
 *
 * @author Patrick Norton
 * @see CaseStatementNode
 */
public class SwitchStatementNode extends SwitchLikeNode {

    @Contract(pure = true)
    public SwitchStatementNode(TestNode switched, boolean fallthrough, CaseStatementNode[] cases, DefaultStatementNode defaultStatement) {
        super(switched, fallthrough, cases, defaultStatement);
    }

    /**
     * Parse a switch statement from a list of tokens.
     * <p>
     *     The syntax for a switch statement is: <code>"switch" "{" *{@link
     *     CaseStatementNode} [{@link DefaultStatementNode}] "}"</code>. The
     *     list of tokens passed must begin with "switch", and all case
     *     statements must be of the same fallthrough type.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed SwitchStatementNode.
     */
    @NotNull
    @Contract("_ -> new")
    public static SwitchStatementNode parse(@NotNull TokenList tokens) {
        return (SwitchStatementNode) parse(tokens, true, false);
    }
}
