import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a context statement.
 * @author Patrick Norton
 */
public class ContextDefinitionNode implements DefinitionNode {
    private VariableNode name;
    private StatementBodyNode enter;
    private StatementBodyNode exit;

    @Contract(pure = true)
    public ContextDefinitionNode(StatementBodyNode enter, StatementBodyNode exit) {
        this.enter = enter;
        this.exit = exit;
    }

    @Contract(pure = true)
    public ContextDefinitionNode(VariableNode name, StatementBodyNode enter, StatementBodyNode exit) {
        this.name = name;
        this.enter = enter;
        this.exit = exit;
    }

    @Override
    public VariableNode getName() {
        return name;
    }

    public StatementBodyNode getEnter() {
        return enter;
    }

    public StatementBodyNode getExit() {
        return exit;
    }

    @Override
    public StatementBodyNode getBody() {
        return enter;
    }

    /**
     * Parse a context definition from a list of tokens.
     * <p>
     *     The syntax for a context definition is as follows: <code>"context"
     *     [{@link VariableNode}] "{" ["enter" {@link StatementBodyNode}]
     *     ["exit" {@link StatementBodyNode}] "}"</code>. The
     *     ContextDefinitionNode must start with "context", passing a TokenList
     *     without that will result in an error.
     * </p>
     * @param tokens The list of tokens to be parsed destructively
     * @return The new ContextDefinitionNode
     */
    @NotNull
    @Contract("_ -> new")
    static ContextDefinitionNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs("context");
        tokens.nextToken();
        VariableNode name = VariableNode.parseOnToken(tokens, TokenType.NAME);
        if (!tokens.tokenIs("{")) {
            throw new ParserException("Context managers must be followed by a curly brace");
        }
        tokens.nextToken(true);
        StatementBodyNode enter = StatementBodyNode.parseOnToken(tokens, "enter");
        StatementBodyNode exit = StatementBodyNode.parseOnToken(tokens, "exit");
        tokens.passNewlines();
        if (!tokens.tokenIs("}")) {
            throw new ParserException("Context manager must end with close curly brace");
        }
        tokens.nextToken();
        return new ContextDefinitionNode(name, enter, exit);
    }

    @Override
    public String toString() {
        if (name.isEmpty()) {
            return "context";
        } else {
            return "context " + name;
        }
    }
}
