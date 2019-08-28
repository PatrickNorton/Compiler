public interface NameNode extends AtomicNode {
    static NameNode parse(TokenList tokens) {
        assert tokens.tokenIs(TokenType.NAME);
        NameNode name = VariableNode.parse(tokens);
        while_brace:
        while (tokens.tokenIs(TokenType.OPEN_BRACE)) {
            switch (tokens.getFirst().sequence) {
                case "(":
                    name = new FunctionCallNode(name, ArgumentNode.parseList(tokens));
                    break;
                case "[":
                    if (tokens.braceContains(":")) {
                        name = new IndexNode(name, SliceNode.parse(tokens));
                    } else {
                        name = new IndexNode(name, LiteralNode.parse(tokens).getBuilders());
                    }
                    break;
                case "{":
                    break while_brace;
                default:
                    throw new RuntimeException("Unexpected brace");
            }
        }
        return name;
    }
}
