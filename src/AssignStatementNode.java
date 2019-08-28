import java.util.ArrayList;

public interface AssignStatementNode extends SimpleStatementNode {
    NameNode[] getName();

    static AssignStatementNode parse(TokenList tokens) {
        ArrayList<TypeNode> var_type = new ArrayList<>();
        ArrayList<NameNode> vars = new ArrayList<>();
        while (!tokens.tokenIs(TokenType.ASSIGN)) {
            if (!tokens.getToken(tokens.sizeOfVariable()).is(TokenType.ASSIGN, TokenType.COMMA)) {
                var_type.add(TypeNode.parse(tokens));
                vars.add(DottedVariableNode.parse(tokens));
                if (tokens.tokenIs(TokenType.ASSIGN)) {
                    break;
                }
                if (!tokens.tokenIs(TokenType.COMMA)) {
                    throw new ParserException("Expected comma, got " + tokens.getFirst());
                }
                tokens.nextToken();
            } else {
                var_type.add(new TypeNode(new DottedVariableNode()));
                vars.add(DottedVariableNode.parseName(tokens));
                if (tokens.tokenIs(TokenType.ASSIGN)) {
                    break;
                }
                if (!tokens.tokenIs(TokenType.COMMA)) {
                    throw new ParserException("Expected comma, got " + tokens.getFirst());
                }
                tokens.nextToken();
            }
        }
        boolean is_colon = tokens.tokenIs(":=");
        tokens.nextToken();
        TestNode[] assignments = TestNode.parseList(tokens, false);
        tokens.Newline();
        TypeNode[] type_array = var_type.toArray(new TypeNode[0]);
        NameNode[] vars_array = vars.toArray(new NameNode[0]);
        boolean is_declared = false;
        for (TypeNode type : var_type) {
            if (!type.getName().isEmpty()) {
                is_declared = true;
                break;
            }
        }
        if (is_declared) {
            return new DeclaredAssignmentNode(is_colon, type_array, vars_array, assignments);
        } else {
            return new AssignmentNode(is_colon, vars_array, assignments);
        }
    }
}
