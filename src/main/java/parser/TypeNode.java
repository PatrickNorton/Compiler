package main.java.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// TODO?? Replace with simple DottedVariableNode
public class TypeNode implements TypeLikeNode {
    private static final Set<String> TYPE_NODE_POSSIBLE = Set.of("[", "*");

    private LineInfo lineInfo;
    private DottedVariableNode name;
    private TypeLikeNode[] subtypes;
    private boolean isVararg;
    private boolean optional;
    private DescriptorNode mutablility;

    public TypeNode(DottedVariableNode name, boolean optional) {
        this(name, new TypeNode[0], false, optional);
    }

    public TypeNode(DottedVariableNode name, TypeLikeNode[] subtypes, boolean isVararg, boolean optional) {
        this(name.getLineInfo(), name, subtypes, isVararg, optional);
    }

    public TypeNode(LineInfo lineInfo, DottedVariableNode name, TypeLikeNode[] subtypes, boolean isVararg, boolean optional) {
        this.lineInfo = lineInfo;
        this.name = name;
        this.subtypes = subtypes;
        this.isVararg = isVararg;
        this.optional = optional;
        this.mutablility = null;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public DottedVariableNode getName() {
        return name;
    }

    @Override
    public String strName() {
        return ((VariableNode) name.getPreDot()).getName();
    }

    @Override
    public void setMutability(DescriptorNode node) {
        mutablility = node;
    }

    @Override
    public Optional<DescriptorNode> getMutability() {
        return Optional.ofNullable(mutablility);
    }

    @Override
    public TypeLikeNode[] getSubtypes() {
        return subtypes;
    }

    @Override
    public boolean isVararg() {
        return isVararg;
    }

    public boolean isOptional() {
        return optional;
    }

    public boolean isDecided() {
        return true;
    }

    @Override
    public boolean isEmpty() {
        return name.isEmpty() && subtypes.length == 0;
    }

    static TypeNode empty() {
        return new TypeNode(DottedVariableNode.empty(), false);
    }

    /**
     * Parse a TypeNode from a list of tokens.
     * <p>
     *     The syntax for a TypeNode is: <code>{@link DottedVariableNode} ["["
     *     [(["*"] {@link TypeNode} | "[" [["*"] {@link TypeNode}]
     *     *("," ["*"] {@link TypeNode}) [","]] "]") *(","
     *     (["*"] {@link TypeNode} | "[" [["*"] {@link TypeNode}]
     *     *("," ["*"] {@link TypeNode}) [","] "]") [","] "]"]</code>.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The parsed TypeNode
     */

    static TypeNode parse(TokenList tokens) {
        return parse(tokens, false, false, false);
    }

    static TypeNode parse(TokenList tokens, boolean ignoreNewlines) {
        return parse(tokens, false, false, ignoreNewlines);
    }

    /**
     * Parse a TypeNode from a list of tokens.
     * @param tokens The list of tokens to destructively parse.
     * @param allowEmpty Whether or not empty varargs are allowed
     * @param isVararg Whether or not the type is a vararg.
     * @return The freshly parsed TypeNode
     */

    private static TypeNode parse(TokenList tokens, boolean allowEmpty, boolean isVararg, boolean ignoreNewlines) {
        if (tokens.tokenIs(Keyword.VAR)) {
            return parseVar(tokens);
        }
        DottedVariableNode main;
        if (!tokens.tokenIs(TokenType.NAME)) {
            if (allowEmpty && tokens.tokenIs("[")) {
                main = DottedVariableNode.empty();
            } else {
                throw tokens.errorExpected("type name");
            }
        } else {
            main = DottedVariableNode.parseNamesOnly(tokens, ignoreNewlines);
            if (tokens.tokenIs("?")) {
                tokens.nextToken(ignoreNewlines);
                return new TypeNode(main, true);
            }
        }
        if (!tokens.tokenIs("[")) {
            return new TypeNode(main, false);
        }
        tokens.nextToken(true);
        List<TypeLikeNode> subtypes = new ArrayList<>();
        while (!tokens.tokenIs("]")) {
            boolean subclassIsVararg;
            if (tokens.tokenIs("*", "**")) {
                subclassIsVararg = true;
                tokens.nextToken(true);
            } else {
                subclassIsVararg = false;
            }
            TypeLikeNode subType = parse(tokens, true, subclassIsVararg, true);
            subtypes.add(subType);
            if (tokens.tokenIs(TokenType.COMMA)) {
                tokens.nextToken(true);
                continue;
            }
            tokens.passNewlines();
            if (!tokens.tokenIs("]")) {
                throw tokens.error("Comma must separate subtypes");
            }
        }
        tokens.nextToken(ignoreNewlines);
        boolean optional;
        if (tokens.tokenIs("?")) {
            optional = true;
            tokens.nextToken(ignoreNewlines);
        } else {
            optional = false;
        }
        return new TypeNode(main, subtypes.toArray(new TypeLikeNode[0]), isVararg, optional);
    }

    private static TypeNode parseVar(TokenList tokens) {
        assert tokens.tokenIs(Keyword.VAR);
        tokens.nextToken();
        return var();
    }

    static boolean nextIsType(TokenList tokens) {
        return tokens.tokenIs(TokenType.NAME, Keyword.VAR) || TYPE_NODE_POSSIBLE.contains(tokens.tokenSequence())
                || (tokens.tokenIs(TokenType.DESCRIPTOR) &&
                        DescriptorNode.MUT_NODES.contains(DescriptorNode.find(tokens.getFirst().sequence)));
    }

    /**
     * Return a new TypeNode representing the keyword {@link Keyword#VAR var}.
     *
     * @return The new TypeNode
     */

    public static TypeNode var() {
        return new TypeNode(DottedVariableNode.empty(), false) {
            @Override
            public boolean isDecided() {
                return false;
            }

            @Override
            public String toString() {
                return "var";
            }
        };
    }

    @Override
    public String toString() {
        if (name.isEmpty() && subtypes.length == 0) {
            return isVararg ? "*[]" : "[]";
        }
        if (subtypes.length > 0) {
            String subtypes = TestNode.toString(this.subtypes);
            return (isVararg ? "*" : "") + name + "[" + subtypes + "]" + (optional ? "?" : "");
        }
        return (isVararg ? "*" : "") + name + (optional ? "?" : "");
    }
}
