package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.Pair;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

/**
 * The interface representing all expressions, e.g. everything that isn't a
 * statement.
 * @author Patrick Norton
 */
public interface TestNode extends IndependentNode, EmptiableNode {
    Set<TokenType> PARSABLE_TOKENS = Collections.unmodifiableSet(
            EnumSet.of(TokenType.NAME, TokenType.OPEN_BRACE, TokenType.OPERATOR,
                    TokenType.NUMBER, TokenType.OP_FUNC, TokenType.ELLIPSIS, TokenType.STRING)
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
            TestNode statement = parse(tokens, ignore_newline);
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
     * Parse a TestNode if the next token is the one specified.
     *
     * @param tokens The list of tokens to be parsed destructively
     * @param token The token to check for
     * @param ignoreNewlines Whether or not to ignore newlines
     * @return The TestNode
     */
    static TestNode parseOnToken(@NotNull TokenList tokens, String token, boolean ignoreNewlines) {
        if (tokens.tokenIs(token)) {
            tokens.nextToken(ignoreNewlines);
            return parse(tokens, ignoreNewlines);
        } else {
            return empty();
        }
    }

    /**
     * Parse a TestNode if the next token is the one specified.
     *
     * @param tokens The list of tokens to be parsed destructively
     * @param token The token to check for
     * @param ignoreNewlines Whether or not to ignore newlines
     * @return The TestNode
     */
    static TestNode parseOnToken(@NotNull TokenList tokens, Keyword token, boolean ignoreNewlines) {
        if (tokens.tokenIs(token)) {
            tokens.nextToken(ignoreNewlines);
            return parse(tokens, ignoreNewlines);
        } else {
            return empty();
        }
    }

    static TestNode parseNoTernaryOnToken(@NotNull TokenList tokens, Keyword token, boolean ignoreNewlines) {
        if (tokens.tokenIs(token)) {
            tokens.nextToken(ignoreNewlines);
            return parseNoTernary(tokens, ignoreNewlines);
        } else {
            return empty();
        }
    }

    /**
     * Parse the non-ternary portion of a TestNode.
     * <p>
     *     This is necessary so that when TestNode parses everything else, which
     *     can be part of a {@link TernaryNode ternary}, it does not try to eat
     *     the ternary and go into an infinite recursive loop.
     *     <ul>
     *         <lh>Types of TestNode (besides ternary):
     *         </lh>
     *         <li>Function call</li>
     *         <li><s>Declaration</s></li>
     *         <li><s>Declared assignment</s></li>
     *         <li><s>Assignment</s></li>
     *         <li>Lone expression</li>
     *         <li>Literal</li>
     *         <li>Lambda</li>
     *     </ul>
     * </p>
     *
     * @param tokens The list of tokens to be destructively parsed
     * @param ignoreNewlines Whether or not to ignore newlines
     * @return The freshly parsed TestNode
     */
    @NotNull
    static TestNode parseNoTernary(@NotNull TokenList tokens, boolean ignoreNewlines) {
        LineInfo info = tokens.lineInfo();
        TestNode node = parseExpression(tokens, ignoreNewlines);
        if (tokens.tokenIs(TokenType.ASSIGN, TokenType.AUG_ASSIGN)) {
            throw ParserException.of("Illegal assignment", info);
        }
        if (node instanceof PostDottableNode && tokens.tokenIs(TokenType.DOT)) {
            return DottedVariableNode.fromExpr(tokens, node, ignoreNewlines);
        } else {
            return node;
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
            nextNode = parseNode(tokens, ignoreNewlines, tempNodes.isEmpty() || !tempNodes.getLast().isUnary());
            if (nextNode == null) {
                if (!tempNodes.isEmpty() && tempNodes.getLast().isPostfix()) {
                    nextNode = tempNodes.removeLast();
                    break;
                } else {
                    throw ParserException.of("Illegal token " + tokens.getFirst(), info);
                }
            }
            if (nextNode instanceof OperatorTypeNode) {
                OperatorTypeNode nextOp = (OperatorTypeNode) nextNode;
                if (nextOp == OperatorTypeNode.SUBTRACT) {
                    nextOp = OperatorTypeNode.U_SUBTRACT;
                }
                if (!nextOp.isUnary()) {
                    throw ParserException.of("Illegal token " + nextOp, info);
                }
                if (nextOp.isPostfix()) {
                    addEmptyPostfix(tempNodes, new OperatorNode(info, nextOp));
                } else {
                    addUnary(tempNodes, new OperatorNode(info, nextOp));
                }
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
                if (nextOp.isUnary() && !nextOp.isPostfix()) {
                    throw ParserException.of("Illegal token " + doubleNext, info1);
                }
                if (nextOp.isPostfix()) {
                    addFilledPostfix(tempNodes, new OperatorNode(info1, nextOp, nextNode));
                } else {
                    addBinary(tempNodes, new OperatorNode(info1, nextOp, nextNode));
                }
            }
        }
        if (tempNodes.isEmpty()) {
            return nextNode;
        }
        return finalizeArguments(tempNodes, nextNode);
    }

    private static void addUnary(@NotNull Deque<OperatorNode> nodes, @NotNull OperatorNode node) {
        assert node.isUnary();
        nodes.addLast(node);
    }

    private static void addBinary(@NotNull Deque<OperatorNode> nodes, @NotNull OperatorNode node) {
        assert !node.isUnary() && node.getOperands().length > 0;
        if (nodes.isEmpty()) {
            nodes.addLast(node);
            return;
        }
        OperatorNode lastNode = nodes.getLast();
        if (lastNode.precedence() > node.precedence()) {
            nodes.addLast(node);
            return;
        } else if (lastNode.getOperator() == node.getOperator()) {
            nodes.getLast().addArgument(node.getOperands()[0]);
            return;
        }
        while (!nodes.isEmpty() && nodes.getLast().precedence() <= node.precedence()) {
            OperatorNode top = nodes.removeLast();
            node.setArgument(0, top.addArgument(node.getOperands()[0]));
        }
        nodes.addLast(node);
    }

    private static void addFilledPostfix(@NotNull Deque<OperatorNode> nodes, @NotNull OperatorNode node) {
        assert node.isPostfix() && node.getOperands().length > 0;
        if (nodes.isEmpty()) {
            nodes.addLast(node);
            return;
        }
        OperatorNode lastNode = nodes.getLast();
        if (lastNode.precedence() > node.precedence()) {
            nodes.addLast(node);
            return;
        }
        while (!nodes.isEmpty() && nodes.getLast().precedence() <= node.precedence()) {
            OperatorNode top = nodes.removeLast();
            node.setArgument(0, top.addArgument(node.getOperands()[0]));
        }
        nodes.addLast(node);
    }

    private static void addEmptyPostfix(@NotNull Deque<OperatorNode> nodes, @NotNull OperatorNode node) {
        assert node.isPostfix() && node.getOperands().length == 0;
        if (nodes.isEmpty()) {
            throw ParserInternalError.of("Illegal postfix operator", node.getLineInfo());
        }
        OperatorNode lastNode = nodes.getLast();
        if (lastNode.getOperands().length < (lastNode.isUnary() ? 1 : 2)) {
            throw ParserInternalError.of("Illegal postfix operator", node.getLineInfo());
        }
        if (lastNode.precedence() > node.precedence()) {
            ArgumentNode[] operands = lastNode.getOperands();
            lastNode.setArgument(operands.length - 1, node.addArgument(operands[operands.length - 1]));
            return;
        }
        while (!nodes.isEmpty() && nodes.getLast().precedence() <= node.precedence()) {
            OperatorNode top = nodes.removeLast();
            node.setArgument(0, top.addArgument(node.getOperands()[0]));
        }
        nodes.addLast(node);
    }

    private static OperatorNode finalizeArguments(@NotNull Deque<OperatorNode> nodes, TestNode next) {
        OperatorNode previous = nodes.removeLast();
        if (previous.isPostfix()) {
            throw ParserException.of("Illegal postfix operator", nodes.getLast().getLineInfo());
        }
        previous.addArgument(next);
        while (!nodes.isEmpty()) {
            OperatorNode last = nodes.getLast();
            if (last.precedence() > previous.precedence()) {
                previous = nodes.removeLast().addArgument(previous);
            } else {
                previous = previous.addArgument(0, nodes.removeLast());
            }
        }
        return previous;
    }

    @Nullable
    private static TestNode parseNode(@NotNull TokenList tokens, boolean ignoreNewlines, boolean parseCurly) {
        TestNode node = parseInternalNode(tokens, ignoreNewlines, parseCurly);
        if (node == null) {
            return null;
        } else if (node instanceof PostDottableNode) {
            node = parsePostBraces(tokens, node, ignoreNewlines);
            if (tokens.tokenIs(TokenType.DOT)) {
                return DottedVariableNode.fromExpr(tokens, node, ignoreNewlines);
            }
        }
        return node;
    }

    /**
     * Parse a node from a list of tokens, or return null if no next node
     * exists.
     *
     * @param tokens The list of tokens to be destructively parsed
     * @param ignoreNewlines Whether or not to ignore newlines
     * @param parseCurly Whether or not to parse a curly brace
     * @return The freshly parsed node
     */
    @Nullable
    private static TestNode parseInternalNode(@NotNull TokenList tokens, boolean ignoreNewlines, boolean parseCurly) {
        if (ignoreNewlines) {
            tokens.passNewlines();
        }
        switch (tokens.tokenType()) {
            case OPEN_BRACE:
                if (parseCurly || !tokens.tokenIs("{")) {
                    return parseOpenBrace(tokens, ignoreNewlines);
                }
                return null;
            case NAME:
                return NameNode.parse(tokens, ignoreNewlines);
            case NUMBER:
                return NumberNode.parse(tokens);
            case OPERATOR:
                return OperatorTypeNode.parse(tokens);
            case OP_FUNC:
                return EscapedOperatorNode.parse(tokens, ignoreNewlines);
            case NEWLINE:
                if (ignoreNewlines) {
                    throw tokens.internalError("Illegal place for newline");
                }
                return null;
            case STRING:
                return StringLikeNode.parse(tokens);
            case KEYWORD:
                return parseKeywordNode(tokens, ignoreNewlines);
            default:
                return null;
        }
    }

    /**
     * Parse a node starting with a keyword from a list of tokens, or null if
     * there is no following node.
     *
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed node
     */
    @Nullable
    private static TestNode parseKeywordNode(@NotNull TokenList tokens, boolean ignoreNewlines) {
        assert tokens.tokenIs(TokenType.KEYWORD);
        switch (Keyword.find(tokens.getFirst())) {
            case SOME:
                return SomeStatementNode.parse(tokens);
            case IN:
                return OperatorTypeNode.parse(tokens);
            case SWITCH:
                return SwitchStatementNode.parse(tokens);
            case LAMBDA:
                return LambdaNode.parse(tokens, ignoreNewlines);
            default:
                return null;
        }
    }

    /**
     * Parse an open brace from a list of tokens.
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed TestNode
     */
    @NotNull
    static TestNode parseOpenBrace(@NotNull TokenList tokens, boolean ignoreNewlines) {
        // Types of brace statement: comprehension, literal, grouping paren, casting
        assert tokens.tokenIs(TokenType.OPEN_BRACE);
        TestNode stmt = parsePostBraces(tokens, parseOpenBraceNoDot(tokens), ignoreNewlines);
        if (tokens.tokenIs(TokenType.DOT)) {
            return DottedVariableNode.fromExpr(tokens, stmt, ignoreNewlines);
        } else {
            return stmt;
        }
    }

    static TestNode parsePostBraces(@NotNull TokenList tokens, TestNode pre, boolean ignoreNewlines) {
        if (ignoreNewlines) {
            tokens.passNewlines();
        }
        postBraceLoop:
        while (tokens.tokenIs(TokenType.OPEN_BRACE)) {
            switch (tokens.tokenSequence()) {
                case "(":
                    pre = new FunctionCallNode(pre, ArgumentNode.parseList(tokens));
                    break;
                case "[":
                    if (tokens.braceContains(TokenType.COLON)) {
                        pre = new IndexNode(pre, SliceNode.parse(tokens));
                    } else {
                        pre = new IndexNode(pre, LiteralNode.parse(tokens).getBuilders());
                    }
                    break;
                case "{":
                    break postBraceLoop;
            }
            if (ignoreNewlines) {
                tokens.passNewlines();
            }
        }
        return pre;
    }

    @NotNull
    private static TestNode parseOpenBraceNoDot(@NotNull TokenList tokens) {
        assert tokens.tokenIs(TokenType.OPEN_BRACE);
        switch (tokens.tokenSequence()) {
            case "(":
                if (tokens.braceContains(Keyword.FOR)) {
                    return ComprehensionNode.parse(tokens);
                } else if (tokens.braceContains(",") || tokens.braceIsEmpty()) {
                    return LiteralNode.parse(tokens);
                } else {
                    tokens.nextToken(true);
                    TestNode contained = parse(tokens, true);
                    if (!tokens.tokenIs(")")) {
                        throw tokens.error("Unmatched brace");
                    }
                    tokens.nextToken();
                    return contained;
                }
            case "[":
                if (tokens.braceContains(TokenType.COLON)) {
                    return RangeLiteralNode.parse(tokens);
                } else if (tokens.braceContains(Keyword.FOR)) {
                    return ComprehensionNode.parse(tokens);
                } else {
                    return LiteralNode.parse(tokens);
                }
            case "{":
                if (tokens.braceContains(Keyword.FOR)) {
                    return ComprehensionLikeNode.parse(tokens);
                } else if (tokens.braceContains(":")) {
                    return DictLiteralNode.parse(tokens);
                } else {
                    return LiteralNode.parse(tokens);
                }
            default:
                throw tokens.internalError("Some unknown brace was found");
        }
    }

    /**
     * Parse a list of TestNodes from a list of tokens.
     * @param tokens The list of tokens to be destructively parsed
     * @return The list of TestNodes
     */
    @NotNull
    static TestNode[] parseList(TokenList tokens, boolean ignoreNewlines) {
        if (!ignoreNewlines && tokens.tokenIs(TokenType.NEWLINE)) {
            return new TestNode[0];
        }
        List<TestNode> tests = new ArrayList<>();
        while (nextIsTest(tokens)) {
            tests.add(parse(tokens, ignoreNewlines));
            if (!tokens.tokenIs(TokenType.COMMA)) {
                break;
            }
            tokens.nextToken(ignoreNewlines);
        }
        return tests.toArray(new TestNode[0]);
    }

    @NotNull
    @Contract("_, _ -> new")
    static Pair<TestNode, TestNode> parseMaybePostIf(TokenList tokens, boolean ignoreNewlines) {
        TestNode preIf = parseNoTernary(tokens, ignoreNewlines);
        if (!tokens.tokenIs(Keyword.IF)) {
            return Pair.of(preIf, null);
        }
        tokens.nextToken(ignoreNewlines);
        TestNode postIf = parse(tokens, ignoreNewlines);
        if (tokens.tokenIs(Keyword.ELSE)) {
            tokens.nextToken(ignoreNewlines);
            TestNode ternary = new TernaryNode(preIf, postIf, parse(tokens, ignoreNewlines));
            return Pair.of(ternary, null);
        } else {
            return Pair.of(preIf, postIf);
        }
    }

    static boolean nextIsTest(@NotNull TokenList tokens) {
        return tokens.tokenIs(PARSABLE_TOKENS) || tokens.tokenIsKeyword(PARSABLE_KEYWORDS);
    }

    static String toString(@NotNull TestNode... vars) {
        StringJoiner sj = new StringJoiner(", ");
        for (TestNode t : vars) {
            sj.add(t.toString());
        }
        return sj.toString();
    }
}
