package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * The class representing a static block in a class/interface definition.
 * @author Patrick Norton
 */
public class StaticBlockNode implements ClassStatementNode {
    private LineInfo lineInfo;
    private StatementBodyNode stmts;

    @Contract(pure = true)
    public StaticBlockNode(LineInfo lineInfo, @NotNull StatementBodyNode stmts) {
        this.lineInfo = lineInfo;
        this.stmts = stmts;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
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

    /**
     * Parse a StaticBlockNode from a list of tokens.
     * <p>
     *     The syntax for a static block is: <code>"static" {@link
     *     StatementBodyNode}</code>.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed StaticBlockNode
     */
    @NotNull
    @Contract("_ -> new")
    public static StaticBlockNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs("static") && tokens.tokenIs(1, "{");
        LineInfo lineInfo = tokens.lineInfo();
        tokens.nextToken();
        return new StaticBlockNode(lineInfo, StatementBodyNode.parse(tokens));
    }

    @Override
    public String toString() {
        return "static " + stmts;
    }
}
