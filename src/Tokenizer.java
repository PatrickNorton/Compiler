import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


// TODO? Remove self_cls tokens
public class Tokenizer {
    public class TokenInfo {
        public final Pattern regex;
        public final int token;

        public TokenInfo(Pattern regex, int token) {
            super();
            this.regex = regex;
            this.token = token;
        }
    }


    private LinkedList<TokenInfo> tokenInfos;
    private LinkedList<Token> tokens;

    public Tokenizer() {
        tokenInfos = new LinkedList<>();
        tokens = new LinkedList<>();
    }

    public void add(Pattern regex, int token) {
        tokenInfos.add(
                new TokenInfo(regex, token)
        );
    }

    public void tokenize(String str) {
        String s = str;
        tokens.clear();
        while (!s.equals("")) {
            boolean match = false;
            for (TokenInfo info : tokenInfos) {
                Matcher m = info.regex.matcher(s);
                if (m.find()) {
                    match = true;
                    if (info.token != 0) {
                        String tok = m.group();
                        tokens.add(new Token(info.token, tok));
                    }
                    s = m.replaceFirst("");
                    break;
                }
            }
            if (!match) throw new RuntimeException(s);
        }
    }

    public LinkedList<Token> getTokens() {
        return tokens;
    }

    public static Tokenizer parse(String str) {
        Tokenizer tokenizer = new Tokenizer();
        // Comments and whitespace
        tokenizer.add(Pattern.compile("^(#\\|((?!\\|#).|\\n)*\\|#|#.*| +|\\\\\\n)"), 0);
        // Newlines
        tokenizer.add(Pattern.compile("^\\R"), 1);
        // Descriptors, e.g. private, etc.
        tokenizer.add(Pattern.compile("^\\b(private|const|final|pubget|static)\\b"), 2);
        // Other keywords, such as for control flow or typeget
        tokenizer.add(Pattern.compile("^\\b(if|for|else|do|func|class|method|while|in|from|(im|ex)port"
                               +"|typeget|dotimes|break|continue|return|context|get|set|lambda"
                               +"|property|enter|exit|try|except|finally|with|as|assert|del|yield"
                               +"|raise|typedef|some|interface)\\b"), 3);
        // The self and cls keywords
        // TODO? Move this into variable section
        tokenizer.add(Pattern.compile("^\\b(self|cls)(\\.[_a-zA-Z][_a-zA-Z0-9.]*)?\\b"), 4);
        // Opening braces
        tokenizer.add(Pattern.compile("^[\\[({]"), 5);
        // Closing braces
        tokenizer.add(Pattern.compile("^[])}]"), 6);
        // The comma
        tokenizer.add(Pattern.compile("^,"), 7);
        // Augmented assignment operators
        // These are separate from other operators because they act differently,
        // and thus need to be parsed separately
        tokenizer.add(Pattern.compile("^([+\\-%]|([*/])\\2?|<<|>>|[&|^~])="), 8);
        // Normal operators
        // Operators in their natural habitat, not masquerading as anything else
        // TODO? Move arrow operator to its own section
        tokenizer.add(Pattern.compile("^(->|==|!=|[><]=?|([+\\-*/])\\2?|<<|>>|[&|^~%])"), 9);
        // Assignment operators
        tokenizer.add(Pattern.compile("^:?="), 10);
        // String literals
        // These are token-ed separately, so they don't mess with the syntax of everything else
        tokenizer.add(Pattern.compile("^[rfb]?\"([^\"]|\\n)+\""), 11);
        // Boolean operators
        tokenizer.add(Pattern.compile("^\\b(and|or|not|xor)\\b"), 12);
        // Digits, incl. those in other bases
        tokenizer.add(Pattern.compile("^(0[xob])?[0-9]+"), 13);
        // The crazy operator syntax
        tokenizer.add(Pattern.compile("^\\b(operator *(r?(==|!=|([+\\-*/])\\4?|[><]=?)|\\[]=?|\\(\\)" +
                             "|u-|iter|new|in|missing|del|str|repr|bool|del(\\[])?|<<|>>|[&|^~%]))"), 14);
        // Dotted variable names
        tokenizer.add(Pattern.compile("^\\b(\\.[_a-zA-Z]+)+\\b"), 19);
        // Variable names
        // Includes a check to make sure operator never shows up as a variable,
        // because it is a keyword, even though it doesn't show up in the keyword check
        tokenizer.add(Pattern.compile("^\\b(?!operator\\b)[_a-zA-Z](\\.[_a-zA-Z0-9]+)*\\b"), 15);
        // Operator-functions
        tokenizer.add(Pattern.compile("^\\\\(==|!=|[><]=?|r?[+\\-*/]{1,2}|u-|<<|>>|[&|^~%])"), 16);
        // Colons, for slice syntax and others
        tokenizer.add(Pattern.compile("^::?"), 17);
        // The ellipsis
        tokenizer.add(Pattern.compile("^\\.{3}"), 18);
        try {
            tokenizer.tokenize(str);
        } catch (RuntimeException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return tokenizer;
    }
}
