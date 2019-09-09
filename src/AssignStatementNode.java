import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * The interface for assignment, whether it be declared or otherwise.
 * <p>
 * This also contains the parser creator method for both of its subclasses, as
 * they are difficult to tell apart early in parsing, but they are very
 * different in compilation.
 * </p>
 *
 * @author Patrick Norton
 * @see AssignmentNode
 * @see DeclaredAssignmentNode
 */
public interface AssignStatementNode extends SimpleStatementNode {
    /**
     * AssignStatementNodes must have something that is assigned to them
     * @return list of assigned names
     */
    NameNode[] getName();

    /**
     * Parse an assignment statement from a list of tokens.
     * @param tokens The tokens to be parsed. Removes tokens which are turned
     *               into the node.
     * @return The statement that was parsed out
     */
    @NotNull
    static AssignStatementNode parse(@NotNull TokenList tokens) {
        if (tokens.tokenIs(tokens.sizeOfVariable(), TokenType.ASSIGN, TokenType.COMMA)) {
            return AssignmentNode.parse(tokens);
        } else {
            return DeclaredAssignmentNode.parse(tokens);
        }
    }
}
