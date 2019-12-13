package main.java.parser;
// TODO! Rename getName() to not conflict with standard name
// TODO? Remove SubTestNode & replace with something more meaningful


import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;

public class Parser {
    private final TokenList tokens;
    private final TopNode top;

    @Contract(pure = true)
    private Parser(Path path, TokenList tokens) {
        this(tokens, new TopNode(path));
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
    public static TopNode parse(Path path, TokenList tokens) {
        Parser parser = new Parser(path, tokens);
        return parser.parse();
    }

    @NotNull
    public static TopNode parse(@NotNull File f) {
        return parse(f.toPath(), Tokenizer.parse(f));
    }
}
