// TODO! Rename getName() to not conflict with standard name
// TODO: Reduce/remove nulls
// TODO? Remove SubTestNode & replace with something more meaningful
// TODO: operator + = (something)
// FIXME: Allow self/cls declarations
// TODO: Annotations
// TODO: Enums


import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class Parser {

    private TokenList tokens;

    @Contract(pure = true)
    private Parser(TokenList tokens) {
        this.tokens = tokens;
    }

    public TokenList getTokens() {
        return tokens;
    }

    @NotNull
    public static TopNode parse(TokenList tokens) {
        Parser parser = new Parser(tokens);
        TopNode topNode = new TopNode();
        while (!parser.tokens.tokenIs(TokenType.EPSILON)) {
            topNode.add(BaseNode.parse(parser.tokens));
            parser.tokens.passNewlines();
        }
        return topNode;
    }
}
