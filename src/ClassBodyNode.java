import java.util.ArrayList;

public class ClassBodyNode extends StatementBodyNode {
    private ClassStatementNode[] statements;

    public ClassBodyNode(ClassStatementNode... statements) {
        // super(statements);
        this.statements = statements;
    }

    public ClassStatementNode[] getStatements() {
        return statements;
    }

    static ClassBodyNode parse(TokenList tokens) {
        if (!tokens.tokenIs("{")) {
            throw new ParserException("The body of a class must be enclosed in curly brackets");
        }
        tokens.nextToken(true);
        ArrayList<ClassStatementNode> statements = new ArrayList<>();
        while (!tokens.tokenIs("}")) {
            statements.add(ClassStatementNode.parse(tokens));
            tokens.passNewlines();
        }
        tokens.nextToken();
        tokens.Newline();
        return new ClassBodyNode(statements.toArray(new ClassStatementNode[0]));
    }
}
