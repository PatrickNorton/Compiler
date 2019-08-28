public interface SimpleStatementNode extends StatementNode {
    static SimpleStatementNode parseIncDec(TokenList tokens) {
        Token amount = tokens.getToken(tokens.sizeOfVariable());
        if (amount.is("++")) {
            return IncrementNode.parse(tokens);
        } else if (amount.is("--")) {
            return DecrementNode.parse(tokens);
        } else {
            throw new RuntimeException("inc_or_dec must use ++ or --");
        }
    }
}
