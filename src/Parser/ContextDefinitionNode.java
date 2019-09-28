package Parser;
// TODO: Non-local variables in context definition

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.LinkedList;

/**
 * The class representing a context statement.
 * @author Patrick Norton
 */
public class ContextDefinitionNode implements DefinitionNode, ClassStatementNode {
    private VariableNode name;
    private TypedArgumentListNode args;
    private StatementBodyNode enter;
    private StatementBodyNode exit;
    private ArgumentNode[] exitArgs;
    private ClassBodyNode others;
    private EnumSet<DescriptorNode> descriptors = DescriptorNode.emptySet();
    private NameNode[] annotations = new NameNode[0];
    private NameNode[] decorators = new NameNode[0];

    @Contract(pure = true)
    public ContextDefinitionNode(StatementBodyNode enter, StatementBodyNode exit) {
        this(VariableNode.empty(), new TypedArgumentListNode(), enter, exit, new ArgumentNode[0], new ClassBodyNode());
    }

    @Contract(pure = true)
    public ContextDefinitionNode(VariableNode name, TypedArgumentListNode args, StatementBodyNode enter,
                                 StatementBodyNode exit, ArgumentNode[] exitArgs, ClassBodyNode others) {
        this.name = name;
        this.args = args;
        this.enter = enter;
        this.exit = exit;
        this.exitArgs = exitArgs;
        this.others = others;
    }

    @Override
    public NameNode getName() {
        return name;
    }

    public TypedArgumentListNode getArgs() {
        return args;
    }

    public StatementBodyNode getEnter() {
        return enter;
    }

    public StatementBodyNode getExit() {
        return exit;
    }

    @Override
    public StatementBodyNode getBody() {
        return enter;
    }

    public ArgumentNode[] getExitArgs() {
        return exitArgs;
    }

    public ClassBodyNode getOthers() {
        return others;
    }

    @Override
    public void addDescriptor(EnumSet<DescriptorNode> nodes) {
        this.descriptors = nodes;
    }

    @Override
    public EnumSet<DescriptorNode> getDescriptors() {
        return descriptors;
    }

    @Override
    public NameNode[] getAnnotations() {
        return annotations;
    }

    @Override
    public void addAnnotations(NameNode... annotations) {
        this.annotations = annotations;
    }

    @Override
    public NameNode[] getDecorators() {
        return decorators;
    }

    @Override
    public void addDecorators(NameNode... decorators) {
        this.decorators = decorators;
    }

    /**
     * Parse a context definition from a list of tokens.
     * <p>
     *     The syntax for a context definition is as follows: <code>"context"
     *     [{@link VariableNode}] "{" ["enter" {@link StatementBodyNode}]
     *     ["exit" {@link StatementBodyNode}] "}"</code>. The
     *     Parser.ContextDefinitionNode must start with "context", passing a Parser.TokenList
     *     without that will result in an error.
     * </p>
     * @param tokens The list of tokens to be parsed destructively
     * @return The new Parser.ContextDefinitionNode
     */
    @NotNull
    @Contract("_ -> new")
    static ContextDefinitionNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.CONTEXT);
        tokens.nextToken();
        VariableNode name = VariableNode.parseOnToken(tokens, TokenType.NAME);
        TypedArgumentListNode args = TypedArgumentListNode.parseOnToken(tokens, "(");
        if (!tokens.tokenIs("{")) {
            throw new ParserException("Context managers must be followed by a curly brace");
        }
        tokens.nextToken(true);
        StatementBodyNode enter = new StatementBodyNode();
        StatementBodyNode exit = new StatementBodyNode();
        ArgumentNode[] exitArgs = new ArgumentNode[0];
        LinkedList<ClassStatementNode> others = new LinkedList<>();
        while (!tokens.tokenIs("}")) {
            if (tokens.tokenIs(Keyword.ENTER)) {
                tokens.nextToken();
                if (enter.isEmpty()) {
                    enter = StatementBodyNode.parse(tokens);
                } else {
                    throw new ParserException("Cannot have multiple definitions of enter");
                }
            } else if (tokens.tokenIs(Keyword.EXIT)) {
                tokens.nextToken();
                if (tokens.tokenIs("(")) {
                    exitArgs = ArgumentNode.parseList(tokens);
                }
                if (exit.isEmpty()) {
                    exit = StatementBodyNode.parse(tokens);
                } else {
                    throw new ParserException("Exit cannot be defined multiple times");
                }
            } else {
                IndependentNode stmt = IndependentNode.parse(tokens);
                tokens.Newline();
                if (stmt instanceof ClassStatementNode) {
                    others.add((ClassStatementNode) stmt);
                } else {
                    throw new ParserException("Illegal statement");
                }
            }
            tokens.passNewlines();
        }
        if (!tokens.tokenIs("}")) {
            throw new ParserException("Context manager must end with close curly brace");
        }
        tokens.nextToken();
        return new ContextDefinitionNode(name, args, enter, exit, exitArgs, ClassBodyNode.fromList(others));
    }

    @Override
    public String toString() {
        if (name.isEmpty()) {
            return "context";
        } else if (args.isEmpty()) {
            return "context " + name;
        } else {
            return "context " + name + args;
        }
    }
}
