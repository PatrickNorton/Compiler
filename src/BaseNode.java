import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The base for all nodes.
 * <p>
 *     This class has two purposes: to function as a base for all other nodes,
 *     and to hold the delegator methods which are to call other parsing
 *     methods.
 * </p>
 * @author Patrick Norton
 */
public interface BaseNode {
    /**
     * Parse any node at all from the list of tokens given.
     * @param tokens The list of tokens which is to be parsed
     * @return The parsed node
     */
    @Contract("_ -> new")
    static BaseNode parse(@NotNull TokenList tokens) {
        tokens.passNewlines();
        switch (tokens.getFirst().token) {
            case KEYWORD:
                return parseKeyword(tokens);
            case DESCRIPTOR:
                return ClassStatementNode.parseDescriptor(tokens);
            case OPEN_BRACE:
                return TestNode.parse(tokens);
            case CLOSE_BRACE:
                throw new ParserException("Unmatched close brace");
            case NAME:
                return parseLeftVariable(tokens);
            case COMMA:
                throw new ParserException("Unexpected comma");
            case AUG_ASSIGN:
                throw new ParserException("Unexpected operator");
            case ARROW:
                throw new ParserException("Unexpected ->");
            case OPERATOR:
                return OperatorNode.parse(tokens, false);
            case ASSIGN:
                throw new ParserException("Unexpected assignment");
            case STRING:
                return StringNode.parse(tokens);
            case BOOL_OP:
                return OperatorNode.parseBoolOp(tokens, false);
            case NUMBER:
                return NumberNode.parse(tokens);
            case OPERATOR_SP:
                if (tokens.tokenIs(1, "=")) {
                    return SpecialOpAssignmentNode.parse(tokens);
                } else {
                    return OperatorDefinitionNode.parse(tokens);
                }
            case OP_FUNC:
                return TestNode.parseOpFunc(tokens);
            case COLON:
                throw new ParserException("Unexpected colon");
            case ELLIPSIS:
                return VariableNode.parseEllipsis(tokens);
            case DOT:
                throw new ParserException("Unexpected dot");
            default:
                throw new RuntimeException("Nonexistent token found");
        }
    }

    /**
     * Parse any token, given that the first item in the list is a keyword.
     * @param tokens The token list to be parsed
     * @return The parsed node
     */
    @Contract("_ -> new")
    private static BaseNode parseKeyword(@NotNull TokenList tokens) {
        assert tokens.tokenIs(TokenType.KEYWORD);
        switch (tokens.getFirst().sequence) {
            case "class":
                if (tokens.tokenIs(1, TokenType.DESCRIPTOR, TokenType.KEYWORD)) {
                    return ClassStatementNode.parseDescriptor(tokens);
                }
                return ClassDefinitionNode.parse(tokens);
            case "func":
                return FunctionDefinitionNode.parse(tokens);
            case "if":
                return IfStatementNode.parse(tokens);
            case "for":
                return ForStatementNode.parse(tokens);
            case "elif":
            case "else":
                throw new ParserException(tokens.getFirst() + " must have a preceding if");
            case "do":
                return DoStatementNode.parse(tokens);
            case "dotimes":
                return DotimesStatementNode.parse(tokens);
            case "method":
                return MethodDefinitionNode.parse(tokens);
            case "while":
                return WhileStatementNode.parse(tokens);
            case "casted":
            case "in":
                throw new ParserException(tokens.getFirst() + " must have a preceding token");
            case "from":
                return ImportExportNode.parse(tokens);
            case "import":
                return ImportStatementNode.parse(tokens);
            case "export":
                return ExportStatementNode.parse(tokens);
            case "typeget":
                return TypegetStatementNode.parse(tokens);
            case "break":
                return BreakStatementNode.parse(tokens);
            case "continue":
                return ContinueStatementNode.parse(tokens);
            case "return":
                return ReturnStatementNode.parse(tokens);
            case "property":
                return PropertyDefinitionNode.parse(tokens);
            case "get":
            case "set":
                throw new ParserException("get and set must be in a property block");
            case "lambda":
                return TestNode.parse(tokens);
            case "context":
                return ContextDefinitionNode.parse(tokens);
            case "enter":
            case "exit":
                throw new ParserException("enter and exit must be in a context block");
            case "try":
                return TryStatementNode.parse(tokens);
            case "except":
            case "finally":
                throw new ParserException("except and finally must come after a try");
            case "with":
                return WithStatementNode.parse(tokens);
            case "as":
                throw new ParserException("as must come in a with statement");
            case "assert":
                return AssertStatementNode.parse(tokens);
            case "del":
                return DeleteStatementNode.parse(tokens);
            case "yield":
                return YieldStatementNode.parse(tokens);
            case "raise":
                return RaiseStatementNode.parse(tokens);
            case "typedef":
                return TypedefStatementNode.parse(tokens);
            case "some":
                return SomeStatementNode.parse(tokens);
            case "interface":
                return InterfaceDefinitionNode.parse(tokens);
            case "switch":
                return SwitchStatementNode.parse(tokens);
            case "case":
                throw new ParserException("Unexpected case");
            default:
                throw new RuntimeException("Keyword mismatch");
        }
    }

    /**
     * Parse a node, given that it starts with a variable token.
     * <p>
     *     Things starting with a variable token:
     *     <ul>
     *      <li>Function call
     *      <li>Lone variable, just sitting there
     *      <li>Declaration
     *      <li>Declared assignment
     *      <li>Assignment
     *      <li>Lone expression
     *     </ul>
     * </p>
     * @param tokens List of tokens to be parsed
     * @return The parsed token
     */
    private static BaseNode parseLeftVariable(@NotNull TokenList tokens) {
        assert tokens.tokenIs(TokenType.NAME);
        Token after_var = tokens.getToken(tokens.sizeOfVariable());
        if (tokens.lineContains(TokenType.ASSIGN)) {
            return AssignStatementNode.parse(tokens);
        } else if (tokens.lineContains(TokenType.AUG_ASSIGN)) {
            DottedVariableNode var = DottedVariableNode.parseName(tokens);
            if (!tokens.tokenIs(TokenType.AUG_ASSIGN)) {
                throw new ParserException("Expected augmented assignment, got " + tokens.getFirst());
            }
            OperatorTypeNode op = OperatorTypeNode.parse(tokens);
            TestNode assignment = TestNode.parse(tokens);
            return new AugmentedAssignmentNode(op, var, assignment);
        } else if (after_var.is("++", "--")) {
            return SimpleStatementNode.parseIncDec(tokens);
        } else if (tokens.lineContains(TokenType.BOOL_OP, TokenType.OPERATOR)) {
            return TestNode.parse(tokens);
        } else if (after_var.is(TokenType.NAME)) {
            return DeclarationNode.parse(tokens);
        } else {
            DottedVariableNode var = DottedVariableNode.parseName(tokens);
            tokens.Newline();
            return var;
        }
    }
}
