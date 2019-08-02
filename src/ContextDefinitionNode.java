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
}
