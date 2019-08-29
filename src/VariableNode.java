import java.util.LinkedList;

public class VariableNode implements NameNode {
    private String name;

    public VariableNode(String names) {
        this.name = names;
    }

    public VariableNode() {
        this.name = "";
    }

    public String getName() {
        return name;
    }

    public boolean isEmpty() {
        return this.name.isEmpty();
    }

    static VariableNode parseOnToken(TokenList tokens, TokenType... types) {
        if (tokens.tokenIs(types)) {
            return VariableNode.parse(tokens);
        } else {
            return new VariableNode();
        }
    }

    static VariableNode parse(TokenList tokens) {
        if (!tokens.tokenIs(TokenType.NAME)) {
            throw new ParserException("Expected name. got " + tokens.getFirst());
        }
        String name = tokens.getFirst().sequence;
        tokens.nextToken();
        return new VariableNode(name);
    }

    static VariableNode parseEllipsis(TokenList tokens) {
        assert tokens.tokenIs(TokenType.ELLIPSIS);
        tokens.nextToken();
        return new VariableNode("...");
    }

    static VariableNode[] parseList(TokenList tokens, boolean ignore_newlines) {
        LinkedList<VariableNode> variables = new LinkedList<>();
        if (ignore_newlines) {
            tokens.passNewlines();
        }
        if (tokens.tokenIs("(") && !tokens.braceContains("in", "for")) {
            tokens.nextToken();
            VariableNode[] vars = parseList(tokens, true);
            if (!tokens.tokenIs(")")) {
                throw new ParserException("Unmatched braces");
            }
            return vars;
        }
        while (true) {
            if (!tokens.tokenIs(TokenType.NAME)) {
                break;
            }
            if (tokens.tokenIs(TokenType.CLOSE_BRACE)) {
                throw new ParserException("Unmatched braces");
            }
            variables.add(VariableNode.parse(tokens));
            if (tokens.tokenIs(",")) {
                tokens.nextToken(ignore_newlines);
            } else {
                break;
            }
        }
        return variables.toArray(new VariableNode[0]);
    }
}
