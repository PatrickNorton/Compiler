public class Token {
    public final TokenType token;
    public final String sequence;

    public Token(TokenType token, String sequence) {
        super();
        this.token = token;
        this.sequence = sequence;
    }

    public boolean is(TokenType... tokens) {
        for (TokenType token : tokens) {
            if (this.token == token) {
                return true;
            }
        }
        return false;
    }

    public boolean is(String... sequences) {
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
