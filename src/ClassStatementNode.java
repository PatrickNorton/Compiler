import java.util.ArrayList;

public interface ClassStatementNode extends InterfaceStatementNode {
    static ClassStatementNode parse(TokenList tokens) {
        if (tokens.tokenIs("static") && tokens.getToken(1).is("{")) {
            tokens.nextToken();
            return new StaticBlockNode(StatementBodyNode.parse(tokens));  // TODO? StaticBlockNode.parse
        }
        BaseNode stmt = BaseNode.parse(tokens);
        if (stmt instanceof ClassStatementNode) {
            return (ClassStatementNode) stmt;
        }
        throw new ParserException(tokens.getFirst()+" is not a valid class statement");
    }

    static ClassStatementNode parseDescriptor(TokenList tokens) {
        ArrayList<DescriptorNode> descriptors = new ArrayList<>();
        if (tokens.tokenIs("class")) {
            descriptors.add(new DescriptorNode("class"));
            tokens.nextToken();
        }
        while (tokens.tokenIs(TokenType.DESCRIPTOR)) {
            descriptors.add(new DescriptorNode(tokens.getFirst().sequence));
            tokens.nextToken();
        }
        switch (tokens.getFirst().token) {
            case KEYWORD:
                return parseDescriptorKeyword(tokens, descriptors);
            case OPERATOR_SP:
                return parseDescriptorOpSp(tokens, descriptors);
            case NAME:
                return parseDescriptorVar(tokens, descriptors);
            default:
                throw new ParserException("Invalid descriptor placement");
        }
    }

    private static ClassStatementNode parseDescriptorKeyword(TokenList tokens, ArrayList<DescriptorNode> descriptors) {
        assert tokens.tokenIs(TokenType.KEYWORD);
        ClassStatementNode node;
        switch (tokens.getFirst().sequence) {
            case "class":
                node = ClassDefinitionNode.parse(tokens);
                break;
            case "method":
                node = MethodDefinitionNode.parse(tokens);
                break;
            case "interface":
                node = InterfaceDefinitionNode.parse(tokens);
                break;
            case "func":
            case "if":
            case "for":
            case "else":
            case "do":
            case "dotimes":
            case "while":
            case "in":
            case "from":
            case "import":
            case "export":
            case "typeget":
            case "break":
            case "continue":
            case "return":
                throw new ParserException("Expected descriptor-usable keyword, got "+tokens.getFirst());
            default:
                throw new RuntimeException("Keyword mismatch");
        }
        node.addDescriptor(descriptors.toArray(new DescriptorNode[0]));
        return node;
    }

    private static ClassStatementNode parseDescriptorOpSp(TokenList tokens, ArrayList<DescriptorNode> descriptors) {
        assert tokens.tokenIs(TokenType.OPERATOR_SP);
        OperatorDefinitionNode op_sp = OperatorDefinitionNode.parse(tokens);
        op_sp.addDescriptor(descriptors.toArray(new DescriptorNode[0]));
        return op_sp;
    }

    private static ClassStatementNode parseDescriptorVar(TokenList tokens, ArrayList<DescriptorNode> descriptors) {
        assert tokens.tokenIs(TokenType.NAME);
        ClassStatementNode stmt;
        if (tokens.lineContains(TokenType.ASSIGN)) {
            stmt = DeclaredAssignmentNode.parse(tokens);
        } else {
            stmt = DeclarationNode.parse(tokens);
        }
        stmt.addDescriptor(descriptors.toArray(new DescriptorNode[0]));
        return stmt;
    }
}
