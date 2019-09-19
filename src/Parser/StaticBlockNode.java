package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a static block in a class/interface definition.
 * @author Patrick Norton
 */
public class StaticBlockNode implements ClassStatementNode {
    private StatementBodyNode stmts;

    @Contract(pure = true)
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

    @NotNull
    @Contract("_ -> new")
    public static StaticBlockNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs("static") && tokens.tokenIs(1, "{");
        tokens.nextToken();
        return new StaticBlockNode(StatementBodyNode.parse(tokens));
    }

    @Override
    public String toString() {
        return "static " + stmts;
    }
}
