package main.java.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;

/**
 * The class representing a static block in a class/interface definition.
 * @author Patrick Norton
 */
public class StaticBlockNode implements ClassStatementNode, ComplexStatementNode {
    private LineInfo lineInfo;
    private StatementBodyNode body;

    @Contract(pure = true)
    public StaticBlockNode(LineInfo lineInfo, @NotNull StatementBodyNode body) {
        this.lineInfo = lineInfo;
        this.body = body;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
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
    public Set<DescriptorNode> validDescriptors() {
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
        return "static " + body;
    }
}
