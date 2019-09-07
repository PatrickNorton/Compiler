import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

public class CaseStatementNode {
    private NameNode label;
    private StatementBodyNode body;
    private boolean fallthrough;

    @Contract(pure = true)
    public CaseStatementNode(NameNode label, StatementBodyNode body, boolean fallthrough) {
        this.label = label;
        this.body = body;
        this.fallthrough = fallthrough;
    }

    public NameNode getLabel() {
        return label;
    }

    public StatementBodyNode getBody() {
        return body;
    }

    public boolean hasFallthrough() {
        return fallthrough;
    }

    @NotNull
    @Contract("_ -> new")
    public static CaseStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs("case");
        return parse(tokens, tokens.tokenIs(tokens.sizeOfVariable(1), ":"));
    }

    @NotNull
    @Contract("_, _ -> new")
    public static CaseStatementNode parse(@NotNull TokenList tokens, boolean fallthrough) {
        assert tokens.tokenIs("case");
        tokens.nextToken();
        NameNode label = NameNode.parse(tokens);  // FIXME: Should be AtomicNode
        StatementBodyNode body;
        if (fallthrough) {  // TODO: Turn into StatementBodyNode.parseSwitch
            tokens.nextToken(true);
            LinkedList<BaseNode> statements = new LinkedList<>();
            while (!tokens.tokenIs("case")) {
                statements.add(BaseNode.parse(tokens));
            }
            body = new StatementBodyNode(statements.toArray(new BaseNode[0]));
        } else {
            body = StatementBodyNode.parse(tokens);
        }
        return new CaseStatementNode(label, body, fallthrough);
    }
}
