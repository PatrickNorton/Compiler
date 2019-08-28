import java.util.ArrayList;

public class StatementBodyNode implements BaseNode {
    private BaseNode[] statements;

    public StatementBodyNode(BaseNode... statements) {
        this.statements = statements;
    }

    public BaseNode[] getStatements() {
        return statements;
    }

    public boolean isEmpty() {
        return statements.length > 0;
    }

    static StatementBodyNode parse(TokenList tokens) {
        if (!tokens.tokenIs("{")) {
            throw new ParserException("The body of a function must be enclosed in curly brackets");
        }
        tokens.nextToken(true);
        ArrayList<BaseNode> statements = new ArrayList<>();
        while (!tokens.tokenIs("}")) {
            statements.add(BaseNode.parse(tokens));
            tokens.passNewlines();
        }
        tokens.nextToken();
        return new StatementBodyNode(statements.toArray(new BaseNode[0]));
    }
}
