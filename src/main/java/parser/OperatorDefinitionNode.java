package main.java.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * The class representing an operator definition.
 * @author Patrick Norton
 */
public class OperatorDefinitionNode implements DefinitionNode, ClassStatementNode, GeneralizableNode {
    private LineInfo lineInfo;
    private SpecialOpNameNode opCode;
    private TypeLikeNode[] retType;
    private TypedArgumentListNode args;
    private StatementBodyNode body;
    private boolean isEqStmt;
    private EnumSet<DescriptorNode> descriptors = DescriptorNode.emptySet();
    private NameNode[] annotations = new NameNode[0];
    private NameNode[] decorators = new NameNode[0];
    private TypeLikeNode[] generics = new TypeLikeNode[0];

    public OperatorDefinitionNode(@NotNull SpecialOpNameNode opCode, @NotNull TypeLikeNode[] retType,
                                  @NotNull TypedArgumentListNode args, @NotNull StatementBodyNode body,
                                  boolean isEqStmt) {
        this(opCode.getLineInfo(), opCode, retType, args, body, isEqStmt);
    }
    /**
     * Construct a new instance of OperatorDefinitionNode.
     * @param opCode The code of the operator definition
     * @param retType The return type of the operator
     * @param args The arguments the operator takes
     * @param body The body of the operator definition
     */
    @Contract(pure = true)
    public OperatorDefinitionNode(LineInfo lineInfo, @NotNull SpecialOpNameNode opCode, @NotNull TypeLikeNode[] retType,
                                  @NotNull TypedArgumentListNode args, @NotNull StatementBodyNode body,
                                  boolean isEqStmt) {
        this.lineInfo = lineInfo;
        this.opCode = opCode;
        this.retType = retType;
        this.args = args;
        this.body = body;
        this.isEqStmt = isEqStmt;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    @Override
    public void addDescriptor(EnumSet<DescriptorNode> nodes) {
        this.descriptors = nodes;
    }

    public SpecialOpNameNode getOpCode() {
        return opCode;
    }

    public TypeLikeNode[] getRetType() {
        return retType;
    }

    public TypedArgumentListNode getArgs() {
        return args;
    }

    public boolean isEqStmt() {
        return isEqStmt;
    }

    @Override
    public StatementBodyNode getBody() {
        return body;
    }

    @Override
    public EnumSet<DescriptorNode> getDescriptors() {
        return descriptors;
    }

    @Override
    public VariableNode getName() {
        return new VariableNode(LineInfo.empty(), opCode.getOperator().name);
    }

    @Override
    public void addAnnotations(NameNode... annotations) {
        this.annotations = annotations;
    }

    @Override
    public NameNode[] getAnnotations() {
        return annotations;
    }

    @Override
    public void addDecorators(NameNode... decorators) {
        this.decorators = decorators;
    }

    @Override
    public NameNode[] getDecorators() {
        return decorators;
    }

    @Override
    public TypeLikeNode[] getGenerics() {
        return generics;
    }

    @Override
    public void addGenerics(TypeLikeNode... types) {
        this.generics = types;
    }

    /**
     * Parse a new operator definition from a list of tokens.
     * <p>
     *     The syntax for an operator definition is <code>*{@link
     *     DescriptorNode} SPECIAL_OP [{@link TypedArgumentListNode}] ["->"
     *     {@link TypeNode} *("," {@link TypeNode}) [","]] {@link
     *     StatementBodyNode}</code>. Descriptors are parsed separately, and
     *     therefore the list of tokens must begin with a special operator.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly-parsed OperatorDefinitionNode
     */
    @NotNull
    @Contract("_ -> new")
    static OperatorDefinitionNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(TokenType.OPERATOR_SP);
        SpecialOpNameNode opCode = SpecialOpNameNode.parse(tokens);
        TypedArgumentListNode args = TypedArgumentListNode.parseOnOpenBrace(tokens);
        TypeLikeNode[] retval;
        if (tokens.tokenIs(TokenType.ARROW)) {
            retval = TypeLikeNode.parseRetVal(tokens);
        } else {
            retval = new TypeNode[0];
        }
        boolean isEqStmt = tokens.tokenIs("=");
        StatementBodyNode body = parseBody(tokens, isEqStmt);
        return new OperatorDefinitionNode(opCode, retval, args, body, isEqStmt);
    }

    @NotNull
    @Contract("_, _ -> new")
    static OperatorDefinitionNode fromGeneric(@NotNull TokenList tokens, @NotNull GenericOperatorNode op) {
        assert tokens.tokenIs("{");
        boolean isEqStmt = tokens.tokenIs("=");
        StatementBodyNode body = parseBody(tokens, isEqStmt);
        return new OperatorDefinitionNode(op.getOpCode(), op.getRetvals(), op.getArgs(), body, isEqStmt);
    }

    @NotNull
    private static StatementBodyNode parseBody(@NotNull TokenList tokens, boolean isEqStmt) {
        assert isEqStmt == tokens.tokenIs("=");
        if (isEqStmt) {
            tokens.nextToken();
            var value = TestNode.parse(tokens, false);
            return new StatementBodyNode(value);
        } else {
            return StatementBodyNode.parse(tokens);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(GeneralizableNode.toString(generics));
        sb.append(DescriptorNode.join(descriptors));
        sb.append(opCode);
        if (!args.isEmpty()) {
            sb.append(' ');
            sb.append(args);
        }
        return sb + " " + body;
    }
}
