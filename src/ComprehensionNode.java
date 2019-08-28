import java.util.LinkedList;

public class ComprehensionNode implements SubTestNode {
    private String brace_type;
    private TypedVariableNode[] variables;
    private TestNode builder;
    private TestNode[] looped;

    public ComprehensionNode(String brace_type, TypedVariableNode[] variables, TestNode builder, TestNode[] looped) {
        this.brace_type = brace_type;
        this.variables = variables;
        this.builder = builder;
        this.looped = looped;
    }

    public String getBrace_type() {
        return brace_type;
    }

    public TypedVariableNode[] getVariables() {
        return variables;
    }

    public TestNode getBuilder() {
        return builder;
    }

    public TestNode[] getLooped() {
        return looped;
    }

    public boolean hasBraces() {
        return !brace_type.isEmpty();
    }

    static ComprehensionNode parse(TokenList tokens) {
        String brace_type;
        if (!tokens.tokenIs(TokenType.OPEN_BRACE)) {  // Comprehensions in function calls
            brace_type = "";
        } else {
            brace_type = tokens.getFirst().sequence;
            tokens.nextToken(true);
        }
        TestNode builder = TestNode.parse(tokens, true);
        if (!tokens.tokenIs("for")) {
            throw new ParserException("Invalid start to comprehension");
        }
        tokens.nextToken(true);
        TypedVariableNode[] variables = TypedVariableNode.parseList(tokens);
        if (!tokens.tokenIs("in")) {
            throw new ParserException("Comprehension body must have in after variable list");
        }
        tokens.nextToken(true);
        LinkedList<TestNode> looped = new LinkedList<>();
        while (true) {
            if (tokens.tokenIs(brace_type)) {
                break;
            }
            looped.add(TestNode.parse(tokens, true));
            if (tokens.tokenIs(",")) {
                tokens.nextToken(true);
            } else {
                break;
            }
        }
        if (!brace_type.isEmpty() && !tokens.tokenIs(TokenList.matchingBrace(brace_type))) {
            throw new ParserException("Expected close brace");
        }
        tokens.nextToken();
        TestNode[] looped_array = looped.toArray(new TestNode[0]);
        return new ComprehensionNode(brace_type, variables, builder, looped_array);
    }
}
