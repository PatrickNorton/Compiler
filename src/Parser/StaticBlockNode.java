package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

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
    public EnumSet<DescriptorNode> getDescriptors() {
        return DescriptorNode.emptySet();
    }

    @Override
    public void addDescriptor(EnumSet<DescriptorNode> nodes) {
        throw new ParserException("Unexpected descriptor in static block");
    }

    @Override
    public EnumSet<DescriptorNode> validDescriptors() {
        return DescriptorNode.STATIC_BLOCK_VALID;
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
