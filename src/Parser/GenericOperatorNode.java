package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.StringJoiner;

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
    private SpecialOpNameNode op_code;
    private TypedArgumentListNode args;
    private TypeNode[] retvals;
    private DescriptorNode[] descriptors = new DescriptorNode[0];

    @Contract(pure = true)
    public GenericOperatorNode(SpecialOpNameNode op_code, TypedArgumentListNode args, TypeNode... retvals) {
        this.op_code = op_code;
        this.args = args;
        this.retvals = retvals;
    }

    public SpecialOpNameNode getOp_code() {
        return op_code;
    }

    public TypedArgumentListNode getArgs() {
        return args;
    }

    public TypeNode[] getRetvals() {
        return retvals;
    }

    public DescriptorNode[] getDescriptors() {
        return descriptors;
    }

    @Override
    public void addDescriptor(DescriptorNode[] nodes) {
        this.descriptors = nodes;
    }

    static boolean isGeneric(@NotNull TokenList tokens) {
        return isGeneric(tokens, 0);
    }

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

    @NotNull
    @Contract("_ -> new")
    public static GenericOperatorNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(TokenType.OPERATOR_SP);
        SpecialOpNameNode op_code = SpecialOpNameNode.parse(tokens);
        TypedArgumentListNode args = TypedArgumentListNode.parseOnToken(tokens, "(");
        TypeNode[] retvals;
        if (tokens.tokenIs(TokenType.ARROW)) {
            retvals = TypeNode.parseRetVal(tokens);
        } else {
            retvals = new TypeNode[0];
        }
        return new GenericOperatorNode(op_code, args, retvals);
    }

    @Override
    public String toString() {
        String ops;
        if (descriptors.length > 0) {
            StringJoiner sj = new StringJoiner(" ");
            for (DescriptorNode d : descriptors) {
                sj.add(d.toString());
            }
            ops = sj + " ";
        } else {
            ops = "";
        }
        return ops + "operator " + op_code + " " + args;
    }
}
