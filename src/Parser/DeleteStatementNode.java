package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The node representing the deletion of a variable.
 *
 * @author Patrick Norton
 */
public class DeleteStatementNode implements SimpleStatementNode {
    private TestNode deleted;

    @Contract(pure = true)
    public DeleteStatementNode(TestNode deleted) {
        this.deleted = deleted;
    }

    public TestNode getDeleted() {
        return deleted;
    }

    /**
     * Parse Parser.DeleteStatementNode from a list of tokens.
     * <p>
     *     The syntax of Parser.DeleteStatementNode is as follows: <code>"del"
     *     {@link TestNode}</code>.
     * </p>
     * @param tokens The list of tokens which is to be parsed destructively
     * @return The newly parsed Parser.DeleteStatementNode
     */
    @NotNull
    @Contract("_ -> new")
    static DeleteStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs("del");
        tokens.nextToken();
        TestNode deletion = TestNode.parse(tokens);
        return new DeleteStatementNode(deletion);
    }

    @Override
    public String toString() {
        return "del " + deleted;
    }
}
