import org.jetbrains.annotations.NotNull;

/**
 * The class representing a static block in a class/interface definition.
 * @author Patrick Norton
 */
public class StaticBlockNode implements ClassStatementNode {
    private StatementBodyNode stmts;

    public StaticBlockNode(@NotNull StatementBodyNode stmts) {
        this.stmts = stmts;
    }

    public StatementBodyNode getStmts() {
        return this.stmts;
    }

    @Override
    public DescriptorNode[] getDescriptors() {
        return new DescriptorNode[0];
    }

    @Override
    public void addDescriptor(DescriptorNode[] nodes) {
        throw new ParserException("Unexpected descriptor in static block");
    }

    @Override
    public String toString() {
        return "static " + stmts;
    }
}
