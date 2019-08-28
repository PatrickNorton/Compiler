import java.util.ArrayList;

public class TypedArgumentListNode implements BaseNode {
    private TypedArgumentNode[] positionArgs;
    private TypedArgumentNode[] normalArgs;
    private TypedArgumentNode[] nameArgs;

    public TypedArgumentListNode(TypedArgumentNode... args) {
        this.normalArgs = args;
    }

    public TypedArgumentListNode(TypedArgumentNode[] positionArgs, TypedArgumentNode[] normalArgs, TypedArgumentNode[] nameArgs) {
        this.normalArgs = normalArgs;
        this.positionArgs = positionArgs;
        this.nameArgs = nameArgs;
    }

    public TypedArgumentNode[] getPositionArgs() {
        return positionArgs;
    }

    public TypedArgumentNode[] getArgs() {
        return normalArgs;
    }

    public TypedArgumentNode[] getNameArgs() {
        return nameArgs;
    }

    public TypedArgumentNode get(int index) {
        return normalArgs[index];
    }

    static TypedArgumentListNode parse(TokenList tokens) {
        assert tokens.tokenIs("(");
        boolean has_posArgs = tokens.braceContains("/");
        if (!tokens.tokenIs("(")) {
            throw new ParserException("Argument lists must start with an open-paren");
        }
        tokens.nextToken(true);
        ArrayList<TypedArgumentNode> posArgs = new ArrayList<>();
        ArrayList<TypedArgumentNode> args = new ArrayList<>();
        ArrayList<TypedArgumentNode> kwArgs = new ArrayList<>();
        if (has_posArgs) {
            while (!tokens.tokenIs("/")) {
                posArgs.add(TypedArgumentNode.parse(tokens));
                if (tokens.tokenIs(TokenType.COMMA)) {
                    tokens.nextToken(true);
                }
            }
            tokens.nextToken();
            if (tokens.tokenIs(TokenType.COMMA)) {
                tokens.nextToken(true);
            } else if (!tokens.tokenIs(TokenType.CLOSE_BRACE)) {
                throw new ParserException("Unexpected " + tokens.getFirst());
            }
        }
        ArrayList<TypedArgumentNode> which_args = args;
        while (!tokens.tokenIs(")")) {
            if (tokens.tokenIs("*") && tokens.getToken(1).is(",", ")")) {
                which_args = kwArgs;
                tokens.nextToken(true);
                tokens.nextToken(true);
                continue;
            }
            which_args.add(TypedArgumentNode.parse(tokens));
            tokens.passNewlines();
            if (tokens.tokenIs(TokenType.COMMA)) {
                tokens.nextToken(true);
                continue;
            }
            if (!tokens.tokenIs(")")) {
                throw new ParserException("Comma must separate arguments");
            }
        }
        tokens.nextToken();
        return new TypedArgumentListNode(posArgs.toArray(new TypedArgumentNode[0]), args.toArray(new TypedArgumentNode[0]),
                kwArgs.toArray(new TypedArgumentNode[0]));
    }
}
