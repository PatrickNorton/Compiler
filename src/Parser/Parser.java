package Parser;
// TODO! Rename getName() to not conflict with standard name
// TODO? Remove SubTestNode & replace with something more meaningful


import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class Parser {
    private final TokenList tokens;
    private final TopNode top;

    @Contract(pure = true)
    private Parser(TokenList tokens) {
        this(tokens, new TopNode());
    }

    @Contract(pure = true)
    private Parser(TokenList tokens, TopNode top) {
        this.tokens = tokens;
        this.top = top;
    }

    public TokenList getTokens() {
        return tokens;
    }

    private TopNode parse() {
        while (!tokens.tokenIs(TokenType.EPSILON)) {
            top.add(IndependentNode.parse(tokens));
            if (tokens.tokenIs(TokenType.EPSILON)) {
                break;
            }
            tokens.Newline();
        }
        return top;
    }

    @NotNull
    public static TopNode parse(TokenList tokens) {
        Parser parser = new Parser(tokens);
        return parser.parse();
    }

    @NotNull
    public static TopNode parse(File f) {
        return parse(Tokenizer.parse(f));
    }
}
