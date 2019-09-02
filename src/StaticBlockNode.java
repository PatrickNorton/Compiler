import org.jetbrains.annotations.NotNull;

/**
 * The class representing a static block in a class/interface definition.
 * @author Patrick Norton
 */
public class StaticBlockNode implements ClassStatementNode {
    private BaseNode[] stmts;

    public StaticBlockNode(@NotNull StatementBodyNode stmts) {
        this.stmts = stmts.getStatements();
    }

    public BaseNode[] getStmts() {
        return this.stmts;
    }

    @Override
    public void addDescriptor(DescriptorNode[] nodes) {
        throw new ParserException("Unexpected descriptor in static block");
    }
}
