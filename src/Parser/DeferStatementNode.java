package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class DeferStatementNode implements FlowStatementNode {
    private StatementBodyNode body;

    @Contract(pure = true)
    public DeferStatementNode(StatementBodyNode body) {
        this.body = body;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
    }

    @NotNull
    @Contract("_ -> new")
    public static DeferStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs("defer");
        tokens.nextToken();
        StatementBodyNode body = StatementBodyNode.parse(tokens);
        return new DeferStatementNode(body);
    }
}
