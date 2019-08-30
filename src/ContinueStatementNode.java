import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class ContinueStatementNode implements SimpleFlowNode {
    private TestNode cond;

    @Contract(pure = true)
    public ContinueStatementNode(TestNode cond) {
        this.cond = cond;
    }

    @Override
    public TestNode getCond() {
        return cond;
    }

    @NotNull
    @Contract("_ -> new")
    static ContinueStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs("continue");
        tokens.nextToken();
        TestNode cond = null;
        if (tokens.tokenIs("if")) {
            tokens.nextToken();
            cond = TestNode.parse(tokens);
        }
        tokens.Newline();
        return new ContinueStatementNode(cond);
    }
}
