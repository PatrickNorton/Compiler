import java.util.ArrayList;

public class InterfaceBodyNode extends StatementBodyNode {
    private InterfaceStatementNode[] statements;

    public InterfaceBodyNode(InterfaceStatementNode... statements) {
        this.statements = statements;
    }

    @Override
    public InterfaceStatementNode[] getStatements() {
        return statements;
    }

    static InterfaceBodyNode parse(TokenList tokens) {
        if (!tokens.tokenIs("{")) {
            throw new ParserException("The body of a class must be enclosed in curly brackets");
        }
        tokens.nextToken(false);
        ArrayList<InterfaceStatementNode> statements = new ArrayList<>();
        while (!tokens.tokenIs("}")) {
            statements.add(InterfaceStatementNode.parse(tokens));
            tokens.passNewlines();
        }
        tokens.nextToken();
        tokens.Newline();
        return new InterfaceBodyNode(statements.toArray(new InterfaceStatementNode[0]));
    }
}
