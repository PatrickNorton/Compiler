package main.java.parser;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public interface VarLikeNode extends SubTestNode {
    VariableNode getVariable();
    boolean isTyped();

    default TypeLikeNode getType() {
        return TypeNode.empty();
    }

    /**
     * Parse a list of {@link VarLikeNode VarlikeNodes} from a list of tokens.
     *
     * @param tokens The list of tokens to be destructively parsed
     * @param ignoreNewlines Whether or not to ignore newlines
     * @return The freshly parsed list
     */
    @NotNull
    static VarLikeNode[] parseList(TokenList tokens, boolean ignoreNewlines) {
        List<VarLikeNode> vars = new ArrayList<>();
        while (TypeNode.nextIsType(tokens)) {
            vars.add(parse(tokens, ignoreNewlines));
            if (!tokens.tokenIs(",")) {
                break;
            }
            tokens.nextToken(ignoreNewlines);
        }
        return vars.toArray(new VarLikeNode[0]);
    }

    @NotNull
    private static VarLikeNode parse(TokenList tokens, boolean ignoreNewlines) {
        int size = TypeLikeNode.sizeOfType(tokens);
        if (size == 0 || !tokens.tokenIs(size, TokenType.NAME)) {
            return VariableNode.parse(tokens, ignoreNewlines);
        } else {
            return TypedVariableNode.parse(tokens, ignoreNewlines);
        }
    }
}
