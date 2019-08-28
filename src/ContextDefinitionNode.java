public class ContextDefinitionNode implements DefinitionNode {
    private VariableNode name;
    private StatementBodyNode enter;
    private StatementBodyNode exit;

    public ContextDefinitionNode(StatementBodyNode enter, StatementBodyNode exit) {
        this.enter = enter;
        this.exit = exit;
    }

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

    static ContextDefinitionNode parse(TokenList tokens) {
        assert tokens.tokenIs("context");
        tokens.nextToken();
        VariableNode name = new VariableNode();
        if (tokens.tokenIs(TokenType.NAME)) {
            name = VariableNode.parse(tokens);
        }
        if (!tokens.tokenIs("{")) {
            throw new ParserException("Context managers must be followed by a curly brace");
        }
        tokens.nextToken(true);
        StatementBodyNode enter = new StatementBodyNode();
        StatementBodyNode exit = new StatementBodyNode();
        if (tokens.tokenIs("enter")) {
            enter = StatementBodyNode.parse(tokens);
        }
        if (tokens.tokenIs("exit")) {
            exit = StatementBodyNode.parse(tokens);
        }
        tokens.passNewlines();
        if (!tokens.tokenIs("}")) {
            throw new ParserException("Context manager must end with close curly brace");
        }
        tokens.nextToken();
        tokens.Newline();
        if (enter.isEmpty()) {
            enter = new StatementBodyNode();
        }
        if (exit.isEmpty()) {
            exit = new StatementBodyNode();
        }
        tokens.Newline();
        return new ContextDefinitionNode(name, enter, exit);
    }
}
