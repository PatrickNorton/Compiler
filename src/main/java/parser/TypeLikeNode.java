package main.java.parser;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

public interface TypeLikeNode extends AtomicNode {
    TypeLikeNode[] getSubtypes();
    boolean isVararg();
    boolean isOptional();
    String strName();
    void setMutability(DescriptorNode node);
    Optional<DescriptorNode> getMutability();
    default boolean isDecided() {
        return true;
    }

    static TypeLikeNode parse(TokenList tokens) {
        return parse(tokens, false);
    }

    static TypeLikeNode parse(@NotNull TokenList tokens, boolean ignoreNewlines) {
        if (tokens.tokenIs(TokenType.DESCRIPTOR)) {
            var descriptor = DescriptorNode.parse(tokens);
            if (DescriptorNode.MUT_NODES.contains(descriptor)) {
                var node = parseNoMut(tokens, ignoreNewlines);
                node.setMutability(descriptor);
                return node;
            } else {
                throw tokens.error("Invalid descriptor for type");
            }
        } else {
            return parseNoMut(tokens, ignoreNewlines);
        }
    }

    static TypeLikeNode parseNoMut(@NotNull TokenList tokens, boolean ignoreNewlines) {
        if (tokens.tokenIs("(")) {
            tokens.nextToken(true);
            TypeLikeNode type = parseNoMut(tokens, true);
            if (!tokens.tokenIs(")")) {
                throw new ParserException("Unexpected )");
            }
            tokens.nextToken(ignoreNewlines);
            return type;
        }
        assert tokens.tokenIs(TokenType.NAME, Keyword.VAR);
        TypeLikeNode type = TypeNode.parse(tokens, ignoreNewlines);
        if (tokens.tokenIs("|")) {
            return TypeUnionNode.fromType(tokens, type, ignoreNewlines);
        } else if (tokens.tokenIs("&")) {
            return TypewiseAndNode.fromType(tokens, type, ignoreNewlines);
        } else {
            return type;
        }
    }

    @NotNull
    static TypeLikeNode[] parseList(@NotNull TokenList tokens) {
        return parseList(tokens, false);
    }

    @NotNull
    static TypeLikeNode[] parseList(@NotNull TokenList tokens, boolean ignoreNewlines) {
        List<TypeLikeNode> types = new ArrayList<>();
        while (tokens.tokenIs(TokenType.NAME, Keyword.VAR) || tokens.tokenIs(TokenType.DESCRIPTOR)) {
            types.add(parse(tokens, ignoreNewlines));
            if (!tokens.tokenIs(TokenType.COMMA)) {
                break;
            }
            tokens.nextToken(ignoreNewlines);
        }
        return types.toArray(new TypeLikeNode[0]);
    }

    @NotNull
    static TypeLikeNode[] parseListOnToken(@NotNull TokenList tokens, TokenType sentinel, boolean ignoreNewlines) {
        if (tokens.tokenIs(sentinel)) {
            tokens.nextToken(ignoreNewlines);
            return parseList(tokens);
        } else {
            return new TypeNode[0];
        }
    }

    @NotNull
    static TypeLikeNode[] parseListOnToken(@NotNull TokenList tokens, Keyword sentinel) {
        if (tokens.tokenIs(sentinel)) {
            tokens.nextToken();
            return parseList(tokens);
        } else {
            return new TypeNode[0];
        }
    }

    /**
     * Parse the return value of some function.
     * <p>
     *     The syntax for a return value indicator is: <code>"->" {@link
     *     TypeNode} *("," {@link TypeNode}) [","]</code>.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The list of return types for the function being parsed
     */
    @NotNull
    static TypeLikeNode[] parseRetVal(@NotNull TokenList tokens) {
        return parseListOnToken(tokens, TokenType.ARROW, false);
    }

    @NotNull
    static TypeLikeNode[] parseRetVal(TokenList tokens, boolean ignoreNewlines) {
        return parseListOnToken(tokens, TokenType.ARROW, ignoreNewlines);
    }

    static String returnString(@NotNull TypeLikeNode... values) {
        if (values.length == 0) {
            return "";
        }
        StringJoiner sj = new StringJoiner(", ", " -> ", "");
        for (TypeLikeNode t : values) {
            sj.add(t.toString());
        }
        return sj.toString();
    }

    static int sizeOfType(TokenList tokens) {
        return sizeOfType(tokens, 0);
    }

    static int sizeOfType(@NotNull TokenList tokens, int start) {
        if (tokens.tokenIs(Keyword.VAR)) {
            return start + 1;
        }
        int netBraces = 0;
        Token previous = Token.Epsilon(LineInfo.empty());
        for (int i = start;; i++) {
            Token token = tokens.getToken(i);
            switch (token.token) {
                case OPEN_BRACE:
                    if (token.is("(")) {
                        if (!previous.is(TokenType.COMMA, TokenType.EPSILON,
                                TokenType.OPERATOR, TokenType.OPEN_BRACE)) {
                            return 0;
                        }
                    } else if (token.is("{")) {
                        return netBraces == 0 ? Math.max(i - 1, 0) : 0;
                    } else if (!previous.is(TokenType.NAME, TokenType.EPSILON)) {
                        return 0;
                    }
                    netBraces++;
                    break;
                case CLOSE_BRACE:
                    netBraces--;
                    break;
                case NAME:
                    if (previous.is(TokenType.NAME, TokenType.CLOSE_BRACE) || previous.is("?")) {
                        return i;
                    }
                    break;
                case DOT:
                    if (!previous.is(TokenType.NAME)) {
                        return 0;
                    }
                    break;
                case OPERATOR:
                    if (!token.is("?", "*", "|", "&")) {
                        return 0;
                    } else if (token.is("*") && !previous.is(TokenType.COMMA, TokenType.OPEN_BRACE)) {
                        return 0;
                    }
                    break;
                case COMMA:
                case NEWLINE:
                    if (netBraces == 0) {
                        return i;
                    }
                    break;
                default:
                    return netBraces == 0 ? i : 0;
            }
            previous = token.is(TokenType.NEWLINE) ? previous : token;
        }
    }
}
