package main.java.parser;

import org.jetbrains.annotations.NotNull;

/**
 * The interface representing a simple statement.
 * @author Patrick Norton
 */
public interface SimpleStatementNode extends StatementNode {
    /**
     * Parse an increment or decrement from a list of tokens.
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed node
     */
    @NotNull
    static SimpleStatementNode parseIncDec(@NotNull TokenList tokens) {
        Token amount = tokens.getToken(tokens.sizeOfVariable());
        if (amount.is("++")) {
            return IncrementNode.parse(tokens);
        } else if (amount.is("--")) {
            return DecrementNode.parse(tokens);
        } else {
            throw tokens.internalError("parseIncDec must use ++ or --");
        }
    }
}
