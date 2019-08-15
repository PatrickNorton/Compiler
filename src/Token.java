public class Token {
    public static final int EPSILON = 0;
    public static final int NEWLINE = 1;
    public static final int DESCRIPTOR = 2;
    public static final int KEYWORD = 3;
    public static final int SELF_CLS = 4;
    public static final int OPEN_BRACE = 5;
    public static final int CLOSE_BRACE = 6;
    public static final int COMMA = 7;
    public static final int AUG_ASSIGN = 8;
    public static final int OPERATOR = 9;
    public static final int ASSIGN = 10;
    public static final int STRING = 11;
    public static final int BOOL_OP = 12;
    public static final int INTEGER = 13;
    public static final int OPERATOR_SP = 14;
    public static final int VARIABLE = 15;
    public static final int OP_FUNC = 16;
    public static final int COLON = 17;
    public static final int ELLIPSIS = 18;

    public final int token;
    public final String sequence;

    public Token(int token, String sequence) {
        super();
        this.token = token;
        this.sequence = sequence;
    }

    public boolean is(int... tokens) {
        for (int token : tokens) {
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
