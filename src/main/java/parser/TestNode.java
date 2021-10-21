package main.java.parser;

import main.java.util.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
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
     * @param ignoreNewlines Whether or not to ignore newlines
     * @return The freshly parsed TestNode
     */
    @NotNull
    static TestNode parse(@NotNull TokenList tokens, boolean ignoreNewlines) {
        TestNode ifTrue = parseNoTernary(tokens, ignoreNewlines);
        if (tokens.tokenIs(Keyword.IF)) {
            tokens.nextToken(ignoreNewlines);
            TestNode statement = parse(tokens, ignoreNewlines);
            if (!tokens.tokenIs(Keyword.ELSE)) {
                throw tokens.error("Ternary must have an else");
            }
            tokens.nextToken(ignoreNewlines);
            TestNode ifFalse = parse(tokens, ignoreNewlines);
            return new TernaryNode(ifTrue, statement, ifFalse);
        } else {
            return ifTrue;
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
    private static TestNode parseNoTernary(@NotNull TokenList tokens, boolean ignoreNewlines) {
        LineInfo info = tokens.lineInfo();
        TestNode node = parseExpression(tokens, ignoreNewlines);
        if (tokens.tokenIs(TokenType.ASSIGN, TokenType.AUG_ASSIGN)) {
            throw ParserException.of("Illegal assignment", info);
        }
        if (node instanceof PostDottableNode) {
            return parsePost(tokens, node, ignoreNewlines);
        } else {
            return node;
        }
    }

    class DummyOp implements TestNode {
        public final OperatorTypeNode op;
        public final LineInfo lineInfo;
        public final int precedence;

        @Contract(pure = true)
        public DummyOp(@NotNull OperatorTypeNode op, LineInfo lineInfo) {
            this.op = op;
            this.lineInfo = lineInfo;
            this.precedence = op.precedence;
        }

        @Override
        public LineInfo getLineInfo() {
            return lineInfo;
        }

        @Override
        public String toString() {
            return op.toString();
        }
    }

    /**
     * Parse an expression from a list of tokens.
     *
     * @param tokens The list of tokens to be destructively parsed
     * @param ignoreNewlines Whether or not to ignore newlines
     * @return The freshly parsed expression
     *
     * @implNote This is essentially an RPN calculator
     */
    private static TestNode parseExpression(@NotNull TokenList tokens, boolean ignoreNewlines) {
        Queue<TestNode> queue = new ArrayDeque<>();
        Deque<DummyOp> stack = new ArrayDeque<>();
        boolean parseCurly = true;
        while (true) {
            LineInfo lineInfo = tokens.lineInfo();
            TestNode node = parseNode(tokens, ignoreNewlines, parseCurly);
            if (node == null) {
                break;
            } else if (node instanceof OperatorTypeNode) {
                // Convert - to u- where needed
                OperatorTypeNode operator = (node == OperatorTypeNode.SUBTRACT && parseCurly)
                        ? OperatorTypeNode.U_SUBTRACT
                        : (OperatorTypeNode) node;
                // Operators in a place that they shouldn't be, e.g. 1 + * 2
                if (parseCurly ^ (operator.isUnary() && !operator.isPostfix())) {
                    throw tokens.defaultError();
                }
                // Push all operators that bind more tightly onto the queue
                while (!stack.isEmpty() && operator.precedence >= stack.peek().precedence) {
                    queue.add(stack.pop());
                }
                // Postfix operators don't go on the stack, as they have no
                // arguments left to be parsed
                if (operator.isPostfix()) {
                    queue.add(new DummyOp(operator, lineInfo));
                } else {
                    stack.push(new DummyOp(operator, lineInfo));
                }
                parseCurly = !operator.isPostfix();
            } else {
                if (!parseCurly) {
                    throw tokens.defaultError();
                }
                // Non-operators just get pushed onto the stack
                queue.add(node);
                parseCurly = false;
            }
        }
        while (!stack.isEmpty()) {
            queue.add(stack.pop());
        }
        if (queue.isEmpty()) {
            throw tokens.error("Illegal empty statement");
        }
        return convertQueueToNode(queue);
    }

    private static TestNode convertQueueToNode(@NotNull Queue<TestNode> queue) {
        Deque<TestNode> temp = new ArrayDeque<>();
        for (TestNode t : queue) {
            if (t instanceof DummyOp) {
                LineInfo info = t.getLineInfo();
                OperatorTypeNode op = ((DummyOp) t).op;
                int nodeCount = op.isUnary() ? 1 : 2;
                TestNode[] nodes = new TestNode[nodeCount];
                try {
                    // Nodes need to be reversed, as they get popped backwards
                    for (int i = nodeCount - 1; i >= 0; i--) {
                        nodes[i] = temp.pop();
                    }
                } catch (NoSuchElementException e) {
                    throw ParserException.of("Illegal node combination", t);
                }
                temp.push(new OperatorNode(info, op, nodes));
            } else {
                temp.push(t);
            }
        }
        TestNode node = temp.pop();
        if (!temp.isEmpty()) {
            throw ParserException.of("Invalid node", temp.peek());
        }
        return node;
    }

    @Nullable
    private static TestNode parseNode(@NotNull TokenList tokens, boolean ignoreNewlines, boolean parseCurly) {
        TestNode node = parseInternalNode(tokens, ignoreNewlines, parseCurly);
        if (node instanceof PostDottableNode) {
            return parsePost(tokens, node, ignoreNewlines);
        } else {
            return node;
        }
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
            case ELLIPSIS:
                return VariableNode.parseEllipsis(tokens);
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
            case RAISE:
                return RaiseStatementNode.parse(tokens, ignoreNewlines);
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
        return parsePost(tokens, parseOpenBraceNoDot(tokens), ignoreNewlines);
    }

    static TestNode parsePost(@NotNull TokenList tokens, TestNode pre, boolean ignoreNewlines) {
        TestNode value = parsePostBraces(tokens, pre, ignoreNewlines);
        return DottedVariableNode.parsePostDots(tokens, value, ignoreNewlines);
    }

    @NotNull
    static TestNode parsePostBraces(@NotNull TokenList tokens, TestNode pre, boolean ignoreNewlines) {
        if (ignoreNewlines) {
            tokens.passNewlines();
        }
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
                    return pre;
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
                        throw tokens.errorWithFirst("Unmatched brace: ) does not match");
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
            return Pair.of(preIf, empty());
        }
        tokens.nextToken(ignoreNewlines);
        TestNode postIf = parse(tokens, ignoreNewlines);
        if (tokens.tokenIs(Keyword.ELSE)) {
            tokens.nextToken(ignoreNewlines);
            TestNode ternary = new TernaryNode(preIf, postIf, parse(tokens, ignoreNewlines));
            return Pair.of(ternary, empty());
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

    static String toString(@NotNull Iterable<? extends TestNode> vars) {
        StringJoiner sj = new StringJoiner(", ");
        for (TestNode t : vars) {
            sj.add(t.toString());
        }
        return sj.toString();
    }
}
