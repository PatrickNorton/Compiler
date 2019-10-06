package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * The class representing a generic function.
 * <p>
 *     This class is only valid within an interface definition, and thus has no
 *     parse method, as it is not meant to be constructed separately.
 * </p>
 * @author Patrick Norton
 * @see InterfaceStatementNode
 * @see GenericOperatorNode
 */
public class GenericFunctionNode implements GenericDefinitionNode {
    private VariableNode name;
    private TypedArgumentListNode args;
    private TypeNode[] retvals;
    private EnumSet<DescriptorNode> descriptors = DescriptorNode.emptySet();

    @Contract(pure = true)
    public GenericFunctionNode(VariableNode name, TypedArgumentListNode args, TypeNode... retvals) {
        this.name = name;
        this.args = args;
        this.retvals = retvals;
    }

    public VariableNode getName() {
        return name;
    }

    public TypedArgumentListNode getArgs() {
        return args;
    }

    public TypeNode[] getRetvals() {
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
     * Whether or not the method at the start of the list of tokens is generic.
     * @param tokens The list of tokens to be destructively parsed
     * @return If the method is generic
     */
    static boolean isGeneric(@NotNull TokenList tokens) {
        return isGeneric(tokens, 0);
    }

    /**
     * Whether or not the method at the given point in the list of tokens is
     * generic.
     * @param tokens The list of tokens to be destructively parsed
     * @param start The starting point for the parse
     * @return If the method is generic
     */
    static boolean isGeneric(@NotNull TokenList tokens, int start) {
        assert tokens.tokenIs(start, Keyword.METHOD);
        int endPtr = start + 1;
        assert tokens.tokenIs(endPtr, TokenType.NAME);
        // NOTE: sizeOfVariable also consumes the open-paren in the argument list,
        // so there is no need to add size to that separately.
        endPtr = tokens.sizeOfVariable(endPtr);
        if (tokens.tokenIs(endPtr, TokenType.ARROW)) {
            endPtr = tokens.sizeOfReturn(endPtr);
        }
        return !tokens.tokenIs(endPtr, "{");
    }

    /**
     * Parse a GenericFunctionNode from a list of tokens.
     * <p>
     *     The syntax for a generic function is: <code>"method" {@link
     *     VariableNode} {@link TypedArgumentListNode} ["->" {@link TypeNode}
     *     *("," {@link TypeNode}) [","]]</code>. The TokenList passed must
     *     begin with "method".
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed GenericFunctionNode
     */
    @NotNull
    @Contract("_ -> new")
    public static GenericFunctionNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.METHOD);
        tokens.nextToken();
        VariableNode name = VariableNode.parse(tokens);
        TypedArgumentListNode args = TypedArgumentListNode.parse(tokens);
        TypeNode[] retval = TypeNode.parseRetVal(tokens);
        return new GenericFunctionNode(name, args, retval);
    }

    @Override
    public String toString() {
        return "func " + name + args;
    }
}
