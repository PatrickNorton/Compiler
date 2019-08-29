import java.util.LinkedList;

public class DottedVariableNode implements NameNode {
    private TestNode preDot;
    private NameNode[] postDots;

    public DottedVariableNode(TestNode preDot, NameNode... postDot) {
        this.preDot = preDot;
        this.postDots = postDot;
    }

    public DottedVariableNode() {
        this.preDot = new VariableNode();
        this.postDots = new NameNode[0];
    }

    public TestNode getPreDot() {
        return preDot;
    }

    public TestNode[] getPostDots() {
        return postDots;
    }

    public boolean isEmpty() {
        return (preDot instanceof VariableNode) && ((VariableNode) preDot).isEmpty();
    }

    static DottedVariableNode parse(TokenList tokens) {  // FIXME: parse ought to be replaced with parseName
        LinkedList<VariableNode> names = new LinkedList<>();
        while (tokens.tokenIs(TokenType.NAME)) {
            names.add(VariableNode.parse(tokens));
            if (!tokens.tokenIs(TokenType.DOT)) {
                break;
            }
            tokens.nextToken();
        }
        return new DottedVariableNode(names.removeFirst(), names.toArray(new VariableNode[0]));
    }

    static DottedVariableNode parseName(TokenList tokens) {  // FIXME? Equivalent to .parse
        assert tokens.tokenIs(TokenType.NAME);
        NameNode name = NameNode.parse(tokens);
        if (tokens.tokenIs(TokenType.DOT)) {
            return DottedVariableNode.fromExpr(tokens, name);
        } else {
            return new DottedVariableNode(name);
        }
    }

    static DottedVariableNode fromExpr(TokenList tokens, TestNode preDot) {
        assert tokens.tokenIs(TokenType.DOT);
        tokens.nextToken();
        LinkedList<NameNode> postDot = new LinkedList<>();
        while (tokens.tokenIs(TokenType.NAME, TokenType.OPERATOR_SP)) {
            if (tokens.tokenIs(TokenType.OPERATOR_SP)) {
                String op_type = tokens.getFirst().sequence.replaceFirst("operator *",  "");
                tokens.nextToken();
                postDot.add(new SpecialOpNameNode(OperatorTypeNode.find_op(op_type)));
                break;
            }
            postDot.add(NameNode.parse(tokens));
            if (!tokens.tokenIs(TokenType.DOT)) {
                break;
            }
            tokens.nextToken();
        }
        return new DottedVariableNode(preDot, postDot.toArray(new NameNode[0]));
    }

    static DottedVariableNode[] parseList(TokenList tokens, boolean ignore_newlines) {
        LinkedList<DottedVariableNode> variables = new LinkedList<>();
        if (ignore_newlines) {
            tokens.passNewlines();
        }
        if (tokens.tokenIs("(") && !tokens.braceContains("in", "for")) {
            tokens.nextToken();
            DottedVariableNode[] vars = parseList(tokens, true);
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
            variables.add(DottedVariableNode.parseName(tokens));
            if (tokens.tokenIs(",")) {
                tokens.nextToken(ignore_newlines);
            } else {
                break;
            }
        }
        return variables.toArray(new DottedVariableNode[0]);
    }
}
