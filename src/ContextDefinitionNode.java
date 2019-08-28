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
        tokens.Newline();
        return new ContextDefinitionNode(name, enter, exit);
    }
}
