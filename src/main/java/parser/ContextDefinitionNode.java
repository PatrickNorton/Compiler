package main.java.parser;
// TODO: Non-local variables in context definition

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * The class representing a context statement.
 * @author Patrick Norton
 */
public class ContextDefinitionNode implements DefinitionNode, ClassStatementNode {
    private LineInfo lineInfo;
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
    public ContextDefinitionNode(LineInfo info, VariableNode name, TypedArgumentListNode args, StatementBodyNode enter,
                                 StatementBodyNode exit, ArgumentNode[] exitArgs, ClassBodyNode others) {
        this.lineInfo = info;
        this.name = name;
        this.args = args;
        this.enter = enter;
        this.exit = exit;
        this.exitArgs = exitArgs;
        this.others = others;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
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

    @Override
    public Set<DescriptorNode> validDescriptors() {
        return DescriptorNode.CONTEXT_VALID;
    }

    /**
     * Parse a context definition from a list of tokens.
     * <p>
     *     The syntax for a context definition is as follows: <code>"context"
     *     [{@link VariableNode}] "{" ["enter" {@link StatementBodyNode}]
     *     ["exit" {@link StatementBodyNode}] "}"</code>. The
     *     ContextDefinitionNode must start with "context", passing a TokenList
     *     without that will result in an error.
     * </p>
     * @param tokens The list of tokens to be parsed destructively
     * @return The new ContextDefinitionNode
     */
    @NotNull
    @Contract("_ -> new")
    static ContextDefinitionNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.CONTEXT);
        LineInfo info = tokens.lineInfo();
        tokens.nextToken();
        VariableNode name = VariableNode.parseOnName(tokens);
        TypedArgumentListNode args = TypedArgumentListNode.parseOnOpenBrace(tokens);
        if (!tokens.tokenIs("{")) {
            throw tokens.error("Context managers must be followed by a curly brace");
        }
        tokens.nextToken(true);
        StatementBodyNode enter = StatementBodyNode.empty();
        StatementBodyNode exit = StatementBodyNode.empty();
        ArgumentNode[] exitArgs = new ArgumentNode[0];
        LinkedList<ClassStatementNode> others = new LinkedList<>();
        while (!tokens.tokenIs("}")) {
            if (tokens.tokenIs(Keyword.ENTER)) {
                tokens.nextToken();
                if (enter.isEmpty()) {
                    enter = StatementBodyNode.parse(tokens);
                } else {
                    throw tokens.error("Cannot have multiple definitions of enter");
                }
            } else if (tokens.tokenIs(Keyword.EXIT)) {
                tokens.nextToken();
                if (tokens.tokenIs("(")) {
                    exitArgs = ArgumentNode.parseList(tokens);
                }
                if (exit.isEmpty()) {
                    exit = StatementBodyNode.parse(tokens);
                } else {
                    throw tokens.error("Exit cannot be defined multiple times");
                }
            } else {
                IndependentNode stmt = IndependentNode.parse(tokens);
                tokens.Newline();
                if (stmt instanceof ClassStatementNode) {
                    others.add((ClassStatementNode) stmt);
                } else {
                    throw ParserException.of("Illegal statement for context definition", stmt);
                }
            }
            tokens.passNewlines();
        }
        if (!tokens.tokenIs("}")) {
            throw tokens.error("Context manager must end with close curly brace");
        }
        tokens.nextToken();
        return new ContextDefinitionNode(info, name, args, enter, exit, exitArgs, ClassBodyNode.fromList(others));
    }

    @Override
    public String toString() {
        String descriptors = DescriptorNode.join(this.descriptors);
        if (name.isEmpty()) {
            return descriptors + "context";
        } else if (args.isEmpty()) {
            return descriptors + "context " + name;
        } else {
            return descriptors + "context " + name + args;
        }
    }
}
