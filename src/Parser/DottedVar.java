package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DottedVar implements BaseNode {
    private LineInfo lineInfo;
    private boolean isNullDot;
    private NameNode postDot;

    @Contract(pure = true)
    public DottedVar(LineInfo info, boolean isNullDot, NameNode postDot) {
        this.lineInfo = info;
        this.isNullDot = isNullDot;
        this.postDot = postDot;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public boolean isNullDot() {
        return isNullDot;
    }

    public NameNode getPostDot() {
        return postDot;
    }

    @NotNull
    @Contract("_, _, _ -> new")
    public static DottedVar parse(@NotNull TokenList tokens, boolean namesOnly, boolean ignoreNewlines) {
        assert tokens.tokenIs(TokenType.DOT);
        LineInfo info = tokens.lineInfo();
        boolean isNullDot = tokens.tokenIs("?.");
        tokens.nextToken(ignoreNewlines);
        NameNode postDot;
        if (tokens.tokenIs(TokenType.OPERATOR_SP)) {
            if (namesOnly) {
                throw tokens.error("Unexpected " + tokens.getFirst());
            }
            postDot = SpecialOpNameNode.parse(tokens);
        } else {
            postDot = VariableNode.parse(tokens);
        }
        if (!namesOnly) {
            postDot = NameNode.parsePostBraces(tokens, postDot);
        }
        return new DottedVar(info, isNullDot, postDot);
    }

    @NotNull
    public static DottedVar[] parseAll(@NotNull TokenList tokens, boolean ignoreNewlines) {
        List<DottedVar> vars = new ArrayList<>();
        while (tokens.tokenIs(TokenType.DOT)) {
            vars.add(parse(tokens, false, ignoreNewlines));
        }
        return vars.toArray(new DottedVar[0]);
    }

    @Override
    public String toString() {
        return (isNullDot ? "?." : ".") + postDot;
    }
}
