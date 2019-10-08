package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The class representing a lexer token.
 * @author Patrick Norton
 */
public class Token {
    public final TokenType token;
    public final String sequence;

    /**
     * Create a new instance of Token.
     * @param token The type of token which this is
     * @param sequence The sequence of text the token belongs to
     */
    @Contract(pure = true)
    public Token(@NotNull TokenType token, @NotNull String sequence) {
        this.token = token;
        this.sequence = sequence;
    }

    /**
     * Test whether or not the token is a certain type.
     * @param token The type to test
     * @return If the token is that type
     */
    public boolean is(TokenType token) {
        return this.token == token;
    }

    /**
     * Test whether or not the token is one of a certain number of token types.
     * @param tokens The list of token types to test if this is a member of
     * @return Whether or not this is one of those types
     */
    public boolean is(@NotNull TokenType... tokens) {
        for (TokenType token : tokens) {
            if (this.token == token) {
                return true;
            }
        }
        return false;
    }

    /**
     * Test whether or not the token is a certain sequence.
     * @param sequence The type to test
     * @return If the token is that type
     */
    public boolean is(String sequence) {
        return this.sequence.equals(sequence);
    }

    /**
     * Test whether or not the token has one of certain sequences.
     * @param sequences The sequences to be tested
     * @return Whether or not this is one of those sequences
     */
    public boolean is(@NotNull String... sequences) {
        for (String sequence : sequences) {
            if (this.sequence.equals(sequence)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Test whether or not the token is a certain keyword.
     * @param keyword The keyword to test
     * @return If the token is that type
     */
    public boolean is(Keyword keyword) {
        return is(TokenType.KEYWORD) && Keyword.find(this.sequence) == keyword;
    }

    /**
     * Test whether or not the token is one of a certain set of tokens.
     * @param tokens The keywords to test
     * @return If the token is one of those keywords
     */
    public boolean is(@NotNull Keyword... tokens) {
        if (this.is(TokenType.KEYWORD)) {
            Keyword keyword = Keyword.find(this.sequence);
            for (Keyword k : tokens) {
                if (keyword == k) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Return an Epsilon token.
     * @return The token
     */
    @NotNull
    @Contract(value = " -> new", pure = true)
    public static Token Epsilon() {
        return new Token(TokenType.EPSILON, "");
    }

    /**
     * Return a Newline token.
     * @return The token
     */
    @NotNull
    @Contract(value = " -> new", pure = true)
    public static Token Newline() {
        return new Token(TokenType.NEWLINE, "\n");
    }

    public String toString() {
        return this.sequence;
    }
}
