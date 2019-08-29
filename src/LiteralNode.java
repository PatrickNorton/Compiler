import java.util.LinkedList;

public class LiteralNode implements SubTestNode {
    private String brace_type;
    private TestNode[] builders;
    private Boolean[] is_splats;

    public LiteralNode(String brace_type, TestNode[] builders, Boolean[] is_splats) {
        this.brace_type = brace_type;
        this.builders = builders;
        this.is_splats = is_splats;
    }

    public String getBrace_type() {
        return brace_type;
    }

    public TestNode[] getBuilders() {
        return builders;
    }

    public Boolean[] getIs_splats() {
        return is_splats;
    }

    static LiteralNode parse(TokenList tokens) {
        assert tokens.tokenIs(TokenType.OPEN_BRACE);
        String brace_type = tokens.getFirst().sequence;
        tokens.nextToken(true);
        String balanced_brace = TokenList.matchingBrace(brace_type);
        LinkedList<TestNode> values = new LinkedList<>();
        LinkedList<Boolean> is_splat = new LinkedList<>();
        while (true) {
            if (tokens.tokenIs(balanced_brace)) {
                break;
            } else if (tokens.tokenIs(TokenType.CLOSE_BRACE)) {
                throw new ParserException("Unmatched braces");
            }
            if (tokens.tokenIs("*")) {
                is_splat.add(true);
                tokens.nextToken(true);
            } else {
                is_splat.add(false);
            }
            values.add(TestNode.parse(tokens, true));
            if (tokens.tokenIs(",")) {
                tokens.nextToken(true);
            } else {
                break;
            }
        }
        if (tokens.tokenIs(balanced_brace)) {
            tokens.nextToken();
        } else {
            throw new ParserException("Unmatched braces");
        }
        return new LiteralNode(brace_type, values.toArray(new TestNode[0]), is_splat.toArray(new Boolean[0]));
    }
}
