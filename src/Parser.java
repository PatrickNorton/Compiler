// TODO! Rename getName() to not conflict with standard name
// TODO: Reduce/remove nulls
// TODO? Remove SubTestNode & replace with something more meaningful
// TODO: operator + = (something)
// FIXME: Allow self/cls declarations
// TODO: Annotations
// TODO: "is" operator


import java.util.LinkedList;

public class Parser {

    private TokenList tokens;

    private Parser(LinkedList<Token> tokens) {
        this.tokens = new TokenList(tokens);
    }

    public TokenList getTokens() {
        return tokens;
    }

    public static TopNode parse(LinkedList<Token> tokens) {
        Parser parser = new Parser(tokens);
        TopNode topNode = new TopNode();
        while (!parser.tokens.tokenIs(TokenType.EPSILON)) {
            topNode.add(BaseNode.parse(parser.tokens));
            parser.tokens.passNewlines();
        }
        return topNode;
    }
}
