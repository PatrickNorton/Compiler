package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * The node representing a generic operator definition.
 * <p>
 *     This node is only valid in an interface definition, and nowhere else,
 *     due to its non-initialized nature. There is no parse method for this
 *     reason, it is only parsable from inside an {@link
 *     InterfaceStatementNode}.
 * </p>
 * @author Patrick Norton
 * @see InterfaceStatementNode
 * @see GenericFunctionNode
 */
public class GenericOperatorNode implements GenericDefinitionNode {
    private LineInfo lineInfo;
    private SpecialOpNameNode opCode;
    private TypedArgumentListNode args;
    private TypeLikeNode[] retvals;
    private EnumSet<DescriptorNode> descriptors = DescriptorNode.emptySet();

    @Contract(pure = true)
    public GenericOperatorNode(LineInfo lineInfo, SpecialOpNameNode opCode, TypedArgumentListNode args, TypeLikeNode... retvals) {
        this.lineInfo = lineInfo;
        this.opCode = opCode;
        this.args = args;
        this.retvals = retvals;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public SpecialOpNameNode getOpCode() {
        return opCode;
    }

    public TypedArgumentListNode getArgs() {
        return args;
    }

    public TypeLikeNode[] getRetvals() {
        return retvals;
    }

    public EnumSet<DescriptorNode> getDescriptors() {
        return descriptors;
    }

    @Override
    public void addDescriptor(EnumSet<DescriptorNode> nodes) {
        this.descriptors = nodes;
    }

    /**
     * Whether or not the operator definition at the start of the list of tokens
     * is generic.
     * @param tokens The list of tokens to be destructively parsed
     * @return If the operator is generic
     */
    static boolean isGeneric(@NotNull TokenList tokens) {
        return isGeneric(tokens, 0);
    }

    /**
     * Whether or not the operator definition at the given point in the list of
     * tokens is generic.
     * @param tokens The list of tokens to be destructively parsed
     * @param start The starting point for the parse
     * @return If the operator is generic
     */
    static boolean isGeneric(@NotNull TokenList tokens, int start) {
        assert tokens.tokenIs(start, TokenType.OPERATOR_SP);
        int endPtr = start + 1;
        if (tokens.tokenIs(endPtr, "(")) {
            endPtr = tokens.sizeOfBrace(endPtr);
        }
        if (tokens.tokenIs(endPtr, TokenType.ARROW)) {
            endPtr = tokens.sizeOfReturn(endPtr);
        }
        return !tokens.tokenIs(endPtr, "{");
    }

    /**
     * Parse a GenericOperatorNode from a list of tokens.
     * <p>
     *     The syntax for a generic operator is: <code>OPERATOR_SP [{@link
     *     TypedArgumentListNode}] ["->" {@link TypeNode} *("," {@link
     *     TypeNode}) [","]]</code>. The TokenList passed must begin with a
     *     special operator token.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed GenericOperatorNode
     */
    @NotNull
    @Contract("_ -> new")
    public static GenericOperatorNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(TokenType.OPERATOR_SP);
        LineInfo info = tokens.lineInfo();
        SpecialOpNameNode opCode = SpecialOpNameNode.parse(tokens);
        TypedArgumentListNode args = TypedArgumentListNode.parseOnOpenBrace(tokens);
        TypeLikeNode[] retvals;
        if (tokens.tokenIs(TokenType.ARROW)) {
            retvals = TypeLikeNode.parseRetVal(tokens);
        } else {
            retvals = new TypeLikeNode[0];
        }
        return new GenericOperatorNode(info, opCode, args, retvals);
    }

    @Override
    public String toString() {
        return DescriptorNode.join(descriptors) + "operator " + opCode + " " + args;
    }
}
