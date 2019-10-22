package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The interface representing all expressions, e.g. everything that isn't a
 * statement.
 * @author Patrick Norton
 */
public interface TestNode extends IndependentNode, EmptiableNode {
    Set<TokenType> PARSABLE_TOKENS = Collections.unmodifiableSet(
            EnumSet.of(TokenType.NAME, TokenType.OPEN_BRACE, TokenType.BOOL_OP,
                    TokenType.OPERATOR, TokenType.NUMBER, TokenType.OP_FUNC,
                    TokenType.ELLIPSIS)
    );
    Set<Keyword> PARSABLE_KEYWORDS = Collections.unmodifiableSet(
            EnumSet.of(Keyword.LAMBDA, Keyword.SOME, Keyword.SWITCH)
    );
    TestNode EMPTY = new EmptyTestNode();

    /**
     * Whether or not the TestNode is empty.
     * @return if it is empty
     */
    @Override
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
        return EMPTY;
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
        if (tokens.tokenIs(Keyword.IF)) {
            tokens.nextToken(ignore_newline);
            TestNode statement = parseNoTernary(tokens, ignore_newline);
            if (!tokens.tokenIs(Keyword.ELSE)) {
                throw tokens.error("Ternary must have an else");
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
     * @param ignoreNewlines Whether or not to ignore newlines
     * @return The freshly parsed TestNode
     */
    @NotNull
    static TestNode parseNoTernary(@NotNull TokenList tokens, boolean ignoreNewlines) {
        TestNode node;
        if (tokens.tokenIs(TokenType.KEYWORD)) {
            switch (Keyword.find(tokens.getFirst())) {
                case LAMBDA:
                    node = LambdaNode.parse(tokens);
                    break;
                case SOME:
                    node = SomeStatementNode.parse(tokens);
                    break;
                case SWITCH:
                    node = SwitchExpressionNode.parse(tokens);
                    break;
                default:
                    throw tokens.error("Unexpected "+tokens.getFirst());
            }
        } else {
            node = parseLeftVariable(tokens, ignoreNewlines);
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
            throw tokens.error("Illegal assignment");
        } else if (ignore_newline && tokens.braceContains(TokenType.AUG_ASSIGN)) {
            throw tokens.error("Illegal augmented assignment");
        } else if (!ignore_newline && tokens.lineContains(TokenType.AUG_ASSIGN)) {
            throw tokens.error("Illegal augmented assignment");
        } else {
            return parseExpression(tokens, ignore_newline);
        }
    }

    /**
     * Parse an expression from a list of tokens.
     * @param tokens The list of tokens to be destructively parsed
     * @param ignoreNewlines Whether or not to ignore newlines
     * @return The freshly parsed expression
     */
    private static TestNode parseExpression(@NotNull TokenList tokens, boolean ignoreNewlines) {
        /*
         * How this works:
         * Operators always come in pairs with an operand and a trailing operand
         * at the end, e.g. 1 + 2 * 3. Thus, this method works by parsing these
         * pairs and then assembling them in their correct order of operations.
         * In essence, it takes these operand-operator pairs and turns them
         * into a deque, looking like [1 +] [2 *] [3], and then puts them
         * together, giving priority to the order of operations, e.g.
         * [1 +] [2 *] [3] -> [1 +] [2 * 3] -> [1 + (2 * 3)].
         */
        Deque<OperatorNode> tempNodes = new ArrayDeque<>();
        TestNode nextNode;
        while (true) {
            LineInfo info = tokens.lineInfo();
            nextNode = parseNode(tokens, ignoreNewlines, true);
            if (nextNode == null) {
                throw ParserException.of("Illegal token " + tokens.getFirst(), info);
            }
            if (nextNode instanceof OperatorTypeNode) {
                OperatorTypeNode nextOp = (OperatorTypeNode) nextNode;
                if (nextOp == OperatorTypeNode.SUBTRACT) {
                    nextOp = OperatorTypeNode.U_SUBTRACT;
                }
                if (!nextOp.isUnary()) {
                    throw ParserException.of("Illegal token " + nextOp, info);
                }
                addUnary(tempNodes, new OperatorNode(nextOp));
            } else {
                LineInfo info1 = tokens.lineInfo();
                TestNode doubleNext = parseNode(tokens, ignoreNewlines, false);
                if (doubleNext == null) {
                    break;
                }
                if (!(doubleNext instanceof OperatorTypeNode)) {
                    throw ParserException.of("Illegal token " + doubleNext, info1);
                }
                OperatorTypeNode nextOp = (OperatorTypeNode) doubleNext;
                if (nextOp.isUnary()) {
                    throw ParserException.of("Illegal token " + doubleNext, info1);
                }
                addBinary(tempNodes, new OperatorNode(info1, nextOp, nextNode));
            }
        }
        OperatorNode previous;
        if (tempNodes.isEmpty()) {
            return nextNode;
        }
        previous = tempNodes.removeLast().addArgument(nextNode);
        while (!tempNodes.isEmpty()) {
            if (tempNodes.peekLast().precedence() > previous.precedence()) {
                previous = tempNodes.removeLast().addArgument(previous);
            } else {
                previous = previous.addArgument(0, tempNodes.removeLast());
            }
        }
        return previous;
    }

    private static void addUnary(@NotNull Deque<OperatorNode> nodes, @NotNull OperatorNode node) {
        assert node.getOperator().isUnary();
        nodes.addLast(node);
    }

    private static void addBinary(@NotNull Deque<OperatorNode> nodes, @NotNull OperatorNode node) {
        assert !node.getOperator().isUnary() && node.getOperands().length > 0;
        if (nodes.isEmpty()) {
            nodes.addLast(node);
            return;
        }
        if (nodes.getLast().precedence() > node.precedence()) {
            nodes.addLast(node);
            return;
        } else if (nodes.getLast().getOperator() == node.getOperator()) {
            nodes.getLast().addArgument(node.getOperands()[0]);
            return;
        }
        while (!nodes.isEmpty() && nodes.getLast().precedence() <= node.precedence()) {
            OperatorNode top = nodes.removeLast();
            node.setArgument(0, top.addArgument(node.getOperands()[0]));
        }
        nodes.addLast(node);
    }

    @Nullable
    private static TestNode parseNode(@NotNull TokenList tokens, boolean ignoreNewlines, boolean parseCurly) {
        if (ignoreNewlines) {
            tokens.passNewlines();
        }
        switch (tokens.getFirst().token) {
            case OPEN_BRACE:
                if (parseCurly || !tokens.tokenIs("{")) {
                    return parseOpenBrace(tokens);
                }
                return null;
            case NAME:
                return DottedVariableNode.parseName(tokens);
            case NUMBER:
                return NumberNode.parse(tokens);
            case ARROW:
                throw tokens.error("Unexpected " + tokens.getFirst());
            case BOOL_OP:
            case OPERATOR:
                return OperatorTypeNode.parse(tokens);
            case OP_FUNC:
                return EscapedOperatorNode.parse(tokens);
            case NEWLINE:
                if (ignoreNewlines) {
                    throw tokens.internalError("Illegal place for newline");
                }
                return null;
            case EPSILON:
            case COLON:
            case COMMA:
            case CLOSE_BRACE:
                return null;
            case STRING:
                return StringNode.parse(tokens);
            case KEYWORD:
                if (tokens.tokenIs(Keyword.IN, Keyword.CASTED)) {
                    return OperatorTypeNode.parse(tokens);
                } else if (tokens.tokenIs(Keyword.IF, Keyword.ELSE, Keyword.FOR)) {
                    return null;
                }  // Lack of breaks here is intentional
            default:
                throw tokens.error("Unexpected " + tokens.getFirst());
        }
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
            if (tokens.braceContains(Keyword.FOR)) {
                stmt = ComprehensionNode.parse(tokens);
            } else if (tokens.braceContains(",")) {
                stmt = LiteralNode.parse(tokens);
            } else {
                tokens.nextToken(true);
                TestNode contained = parse(tokens, true);
                if (!tokens.tokenIs(")")) {
                    throw tokens.error("Unmatched brace");
                }
                tokens.nextToken();
                stmt = contained;
            }
            if (tokens.tokenIs("(")) {
                stmt = new FunctionCallNode(stmt, ArgumentNode.parseList(tokens));
            }
        } else if (tokens.tokenIs("[")) {
            if (tokens.braceContains(TokenType.COLON)) {
                stmt = RangeLiteralNode.parse(tokens);
            } else if (tokens.braceContains(Keyword.FOR)) {
                stmt = ComprehensionNode.parse(tokens);
            } else {
                stmt = LiteralNode.parse(tokens);
            }
        } else if (tokens.tokenIs("{")) {
            if (tokens.braceContains(Keyword.FOR)) {
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
        return parseList(tokens, ignore_newlines, false);
    }

    @NotNull
    private static TestNode[] parseList(TokenList tokens, boolean ignore_newlines, boolean danglingIf) {
        if (!ignore_newlines && tokens.tokenIs(TokenType.NEWLINE)) {
            return new TestNode[0];
        }
        List<TestNode> tests = new ArrayList<>();
        while (!tokens.tokenIs(TokenType.NEWLINE, "}")) {
            if (!danglingIf || TernaryNode.beforeDanglingIf(tokens)) {
                tests.add(parse(tokens, ignore_newlines));
            } else {
                tests.add(parseNoTernary(tokens, ignore_newlines));
            }
            if (!tokens.tokenIs(TokenType.COMMA)) {
                break;
            }
            tokens.nextToken(ignore_newlines);
            if (!nextIsTest(tokens)) {
                break;
            }
        }
        return tests.toArray(new TestNode[0]);
    }

    @NotNull
    static TestNode[] parseListDanglingIf(TokenList tokens, boolean ignore_newlines) {
        return parseList(tokens, ignore_newlines, true);
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
        List<TestNode> tests = new ArrayList<>();
        while (!tokens.tokenIs("{")) {
            tests.add(TestNode.parse(tokens));
            if (tokens.tokenIs(TokenType.COMMA)) {
                tokens.nextToken();
                continue;
            }
            if (!tokens.tokenIs("{")) {
                throw tokens.error("Comma must separate values");
            }
        }
        return tests.toArray(new TestNode[0]);
    }

    private static boolean nextIsTest(@NotNull TokenList tokens) {
        return PARSABLE_TOKENS.contains(tokens.getFirst().token) ||
                (tokens.tokenIs(TokenType.KEYWORD) &&
                        PARSABLE_KEYWORDS.contains(Keyword.find(tokens.getFirst().sequence)));
    }
}
