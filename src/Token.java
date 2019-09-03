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
        super();
        this.token = token;
        this.sequence = sequence;
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

    public String toString() {
        return this.sequence;
    }
}
