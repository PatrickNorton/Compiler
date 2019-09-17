import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

public interface DecoratableNode extends IndependentNode {
    void addDecorators(NameNode... decorators);
    NameNode[] getDecorators();

    @NotNull
    static DecoratableNode parseLeftDecorator(@NotNull TokenList tokens) {
        LinkedList<NameNode> decorators = new LinkedList<>();
        while (tokens.tokenIs(TokenType.AT)) {
            tokens.nextToken();
            decorators.add(NameNode.parse(tokens));
        }
        DecoratableNode stmt = DecoratableNode.parse(tokens);
        stmt.addDecorators(decorators.toArray(new NameNode[0]));
        return stmt;
    }

    @NotNull
    static DecoratableNode parse(TokenList tokens) {
        IndependentNode stmt = IndependentNode.parse(tokens);
        if (stmt instanceof DecoratableNode) {
            return (DecoratableNode) stmt;
        } else {
            throw new ParserException("Illegal decorator");
        }
    }
}
