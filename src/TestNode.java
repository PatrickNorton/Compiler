import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.LinkedList;

/**
 * The interface representing all expressions, e.g. everything that isn't a
 * statement.
 * @author Patrick Norton
 */
public interface TestNode extends IndependentNode {
    /**
     * Whether or not the TestNode is empty.
     * @return if it is empty
     */
    default boolean isEmpty() {
        return false;
    }

    /**
     * Construct a new empty TestNode.
     * @return The empty node
     */
    @NotNull
    @Contract(value = " -> new", pure = true)
    static TestNode empty() {
        return new EmptyTestNode();
    }

    /**
     * Parse a TestNode from a list of statements.
     * <p>
     *     A TestNode is simply defined as the union of its subclasses, and so
     *     this is what this function does, it delegates to that.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed TestNode
     */
    @NotNull
    static TestNode parse(TokenList tokens) {
        return parse(tokens, false);
    }

    /**
     * Parse a TestNode from a list of statements, with or without newlines
     * ignored.
     * <p>
     *     A TestNode is simply defined as the union of its subclasses, and so
     *     this is what this function does, it delegates to that.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @param ignore_newline Whether or not to ignore newlines
     * @return The freshly parsed TestNode
     */
    @NotNull
    static TestNode parse(@NotNull TokenList tokens, boolean ignore_newline) {
        TestNode if_true = parseNoTernary(tokens, ignore_newline);
        if (tokens.tokenIs("if")) {
            tokens.nextToken(ignore_newline);
            TestNode statement = parseNoTernary(tokens, ignore_newline);
            if (!tokens.tokenIs("else")) {
                throw new ParserException("Ternary must have an else");
            }
            tokens.nextToken(ignore_newline);
            TestNode if_false = parse(tokens, ignore_newline);
            return new TernaryNode(if_true, statement, if_false);
        } else {
            return if_true;
        }
    }

    /**
     * Parse the non-ternary portion of a TestNode.
     * <p>
     *     This is necessary so that when TestNode parses everything else, which
     *     can be part of a {@link TernaryNode ternary}, it does not try to eat
     *     the ternary and go into an infinite recursive loop.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @param ignore_newline Whether or not to ignore newlines
     * @return The freshly parsed TestNode
     */
    @NotNull
    private static TestNode parseNoTernary(@NotNull TokenList tokens, boolean ignore_newline) {
        TestNode node;
        switch (tokens.getFirst().token) {
            case NAME:
            case OPEN_BRACE:
            case BOOL_OP:
            case OPERATOR:
                node = parseLeftVariable(tokens, ignore_newline);
                break;
            case NUMBER:
                node = NumberNode.parse(tokens);
                break;
            case OP_FUNC:
                node = parseOpFunc(tokens);
                break;
            case ELLIPSIS:
                node = VariableNode.parseEllipsis(tokens);
                break;
            case STRING:
                node = StringNode.parse(tokens);
                break;
            case KEYWORD:
                if (tokens.tokenIs("lambda")) {
                    node = LambdaNode.parse(tokens);
                    break;
                } else if (tokens.tokenIs("some")) {
                    node = SomeStatementNode.parse(tokens);
                    break;
                }
                // Intentional fallthrough here
            default:
                throw new ParserException("Unexpected "+tokens.getFirst());
        }
        if (node instanceof PostDottableNode && tokens.tokenIs(TokenType.DOT)) {
            return DottedVariableNode.fromExpr(tokens, node);
        } else {
            return node;
        }
    }

    /**
     * Parses an expression starting with a variable from a list of tokens.
     * <p>
     *     <ul>
     *         <lh>Things beginning with a variable token (besides ternary):
     *         </lh>
     *         <li>Function call</li>
     *         <li><s>Declaration</s></li>
     *         <li><s>Declared assignment</s></li>
     *         <li><s>Assignment</s></li>
     *         <li>Lone expression</li>
     *     </ul>
     *     The list of tokens passed must begin with a variable token.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @param ignore_newline Whether or not to ignore newlines
     * @return The parsed TestNode
     */
    @NotNull
    private static TestNode parseLeftVariable(@NotNull TokenList tokens, boolean ignore_newline) {
        if (tokens.lineContains(TokenType.ASSIGN)) {
            throw new ParserException("Illegal assignment");
        } else if (tokens.contains(ignore_newline, TokenType.AUG_ASSIGN)) {
            throw new ParserException("Illegal augmented assignment");
        } else {
            LinkedList<TestNode> nodes = new LinkedList<>();
            while_loop:
            while (!tokens.tokenIs(TokenType.NEWLINE)) {
                switch (tokens.getFirst().token) {
                    case OPEN_BRACE:
                        TestNode last_node = nodes.peekLast();
                        if (last_node instanceof OperatorNode || last_node == null) {
                            nodes.add(parseOpenBrace(tokens));
                            break;
                        }
                        break while_loop;
                    case NAME:
                        nodes.add(DottedVariableNode.parseName(tokens));
                        break;
                    case NUMBER:
                        nodes.add(NumberNode.parse(tokens));
                        break;
                    case ARROW:
                        throw new ParserException("Unexpected " + tokens.getFirst());
                    case BOOL_OP:
                    case OPERATOR:
                        nodes.add(OperatorNode.empty(tokens));
                        tokens.nextToken();
                        break;
                    case OP_FUNC:
                        nodes.add(parseOpFunc(tokens));
                        break;
                    case EPSILON:
                    case COLON:
                    case COMMA:
                    case CLOSE_BRACE:
                        break while_loop;
                    case KEYWORD:
                        if (tokens.tokenIs("in", "casted")) {
                            nodes.add(OperatorNode.empty(tokens));
                            tokens.nextToken();
                            break;
                        } else if (tokens.tokenIs("if", "else", "for")) {
                            break while_loop;
                        }  // Lack of breaks here is intentional
                    default:
                        throw new ParserException("Unexpected " + tokens.getFirst());
                }
            }
            if (nodes.size() == 1) {
                return nodes.get(0);
            }
            for (OperatorTypeNode[] operators : OperatorTypeNode.orderOfOperations()) {
                parseExpression(nodes, operators);
            }
            if (nodes.size() > 1) {
                throw new ParserException("Too many tokens");
            }
            return nodes.get(0);
        }
    }

    /**
     * Parse an expression out of a list of nodes.
     * @param nodes The list of nodes to be recombined
     * @param expr The expressions to parse together
     */
    private static void parseExpression(@NotNull LinkedList<TestNode> nodes, OperatorTypeNode... expr) {
        if (nodes.size() == 1) {
            return;
        }
        for (int nodeNumber = 0; nodeNumber < nodes.size(); nodeNumber++) {
            TestNode node = nodes.get(nodeNumber);
            if (node instanceof OperatorNode) {
                if (!node.isEmpty()) {
                    continue;
                }
                OperatorTypeNode operator = ((OperatorNode) node).getOperator();
                if (Arrays.asList(expr).contains(operator)) {
                    if (operator == OperatorTypeNode.SUBTRACT) {
                        if (nodeNumber == 0) {
                            parseUnaryOp(nodes, nodeNumber);
                            continue;
                        }
                        TestNode nodePrevious = nodes.get(nodeNumber - 1);
                        if (nodePrevious instanceof OperatorNode) {
                            if (nodePrevious.isEmpty()) {
                                parseUnaryOp(nodes, nodeNumber);
                            } else {
                                parseOperator(nodes, nodeNumber);
                                nodeNumber--;
                            }
                        }
                        continue;
                    }
                    if (operator.isUnary()) {
                        parseUnaryOp(nodes, nodeNumber);
                    } else {
                        parseOperator(nodes, nodeNumber);
                        nodeNumber--;  // Adjust pointer to match new object
                    }
                }
            }
        }
    }

    /**
     * Parse out the operator from the LinkedList of nodes.
     * @param nodes The nodes to have the operator parsed out of them
     * @param nodeNumber The number giving the location of the operator
     */
    private static void parseOperator(@NotNull LinkedList<TestNode> nodes, int nodeNumber) {
        if (nodeNumber == 0 || nodeNumber + 1 == nodes.size()) {
            throw new ParserException("Unexpected operator" + nodes.get(nodeNumber));
        }
        TestNode node = nodes.get(nodeNumber);
        TestNode previous = nodes.get(nodeNumber - 1);
        TestNode next = nodes.get(nodeNumber + 1);
        TestNode op;
        if (node instanceof OperatorNode) {
            op = OperatorNode.fromEmpty((OperatorNode) node, previous, next);
        } else {
            throw new ParserException("Unexpected node for parseOperator");
        }
        nodes.set(nodeNumber, op);
        nodes.remove(nodeNumber + 1);
        nodes.remove(nodeNumber - 1);
    }

    /**
     * Parse out a unary operator from a list of nodes.
     * @param nodes The nodes to have the operator parsed out
     * @param nodeNumber The number giving the location of the operator
     */
    private static void parseUnaryOp(@NotNull LinkedList<TestNode> nodes, int nodeNumber) {
        if (nodeNumber + 1 == nodes.size()) {
            throw new ParserException("Unexpected operator " + nodes.get(nodeNumber));
        }
        TestNode node = nodes.get(nodeNumber);
        TestNode next = nodes.get(nodeNumber + 1);
        TestNode op;
        if (node instanceof OperatorNode) {
            op = OperatorNode.fromEmpty((OperatorNode) node, next);
        } else {
            throw new ParserException("Unexpected node for parseOperator");
        }
        nodes.set(nodeNumber, op);
        nodes.remove(nodeNumber + 1);
    }

    /**
     * Parse an open brace from a list of tokens.
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed TestNode
     */
    @NotNull
    static TestNode parseOpenBrace(@NotNull TokenList tokens) {
        // Types of brace statement: comprehension, literal, grouping paren, casting
        TestNode stmt;
        if (tokens.tokenIs("(")) {
            if (tokens.braceContains("for")) {
                stmt = ComprehensionNode.parse(tokens);
            } else if (tokens.braceContains(",")) {
                stmt = LiteralNode.parse(tokens);
            } else {
                tokens.nextToken(true);
                TestNode contained = parse(tokens, true);
                if (!tokens.tokenIs(")")) {
                    throw new ParserException("Unmatched brace");
                }
                tokens.nextToken();
                stmt = contained;
            }
            if (tokens.tokenIs("(")) {
                stmt = new FunctionCallNode(stmt, ArgumentNode.parseList(tokens));
            }
        } else if (tokens.tokenIs("[")) {
            if (tokens.braceContains("for")) {
                stmt = ComprehensionNode.parse(tokens);
            } else {
                stmt = LiteralNode.parse(tokens);
            }
        } else if (tokens.tokenIs("{")) {
            if (tokens.braceContains("for")) {
                stmt = tokens.braceContains(":") ? DictComprehensionNode.parse(tokens) : ComprehensionNode.parse(tokens);
            } else if (tokens.braceContains(":")) {
                stmt = DictLiteralNode.parse(tokens);
            } else {
                stmt = LiteralNode.parse(tokens);
            }
        } else {
            throw new RuntimeException("Some unknown brace was found");
        }
        if (tokens.tokenIs(TokenType.DOT)) {
            return DottedVariableNode.fromExpr(tokens, stmt);
        } else {
            return stmt;
        }
    }

    /**
     * Parse a list of TestNodes from a list of tokens.
     * @param tokens The list of tokens to be destructively parsed
     * @param ignore_newlines Whether or not to ignore newlines
     * @return The list of TestNodes
     */
    @NotNull
    static TestNode[] parseList(@NotNull TokenList tokens, boolean ignore_newlines) {
        if (!ignore_newlines && tokens.tokenIs(TokenType.NEWLINE)) {
            return new TestNode[0];
        }
        LinkedList<TestNode> tests = new LinkedList<>();
        while (!tokens.tokenIs(TokenType.NEWLINE, "}")) {
            tests.add(parse(tokens, ignore_newlines));
            if (tokens.tokenIs(TokenType.COMMA)) {
                tokens.nextToken(ignore_newlines);
                continue;
            }
            if (!ignore_newlines && !tokens.tokenIs(TokenType.NEWLINE, "}")) {
                throw new ParserException("Comma must separate values");
            } else if (ignore_newlines) {
                break;
            }
        }
        if (!ignore_newlines && !tokens.tokenIs(TokenType.NEWLINE, "}")) {
            throw new ParserException("Expected newline, got "+tokens.getFirst());
        }
        return tests.toArray(new TestNode[0]);
    }

    /**
     * Parse an operator function from a list of tokens.
     * @param tokens The list of tokens to be destructively parsed
     * @return The parsed operator function
     */
    @NotNull
    static SubTestNode parseOpFunc(@NotNull TokenList tokens) {  // FIXME: Move this somewhere more reasonable
        assert tokens.tokenIs(TokenType.OP_FUNC);
        OperatorTypeNode op_code = OperatorTypeNode.parse(tokens);
        if (tokens.tokenIs("(")) {
            return new OperatorNode(op_code, ArgumentNode.parseList(tokens));
        } else {
            return op_code;
        }
    }

    /**
     * Parse the iterables in a for loop.
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed list of TestNodes
     */
    @NotNull
    static TestNode[] parseForIterables(@NotNull TokenList tokens) {
        if (tokens.tokenIs(TokenType.NEWLINE)) {
            return new TestNode[0];
        }
        LinkedList<TestNode> tests = new LinkedList<>();
        while (!tokens.tokenIs("{")) {
            tests.add(TestNode.parse(tokens));
            if (tokens.tokenIs(TokenType.COMMA)) {
                tokens.nextToken();
                continue;
            }
            if (!tokens.tokenIs("{")) {
                throw new ParserException("Comma must separate values");
            }
        }
        return tests.toArray(new TestNode[0]);
    }
}
