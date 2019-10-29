package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SwitchStatementNode implements StatementNode, EmptiableNode, TestNode {
    private LineInfo lineInfo;
    private TestNode switched;
    private CaseStatementNode[] cases;

    @Contract(pure = true)
    public SwitchStatementNode(LineInfo lineInfo, TestNode switched, CaseStatementNode[] cases) {
        this.lineInfo = lineInfo;
        this.switched = switched;
        this.cases = cases;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public TestNode getSwitched() {
        return switched;
    }

    public CaseStatementNode[] getCases() {
        return cases;
    }

    @NotNull
    @Contract("_ -> new")
    public static SwitchStatementNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.SWITCH);
        LineInfo info = tokens.lineInfo();
        tokens.nextToken();
        TestNode switched = TestNode.parse(tokens);
        if (!tokens.tokenIs("{")) {
            throw tokens.error("Unexpected " + tokens.getFirst());
        }
        tokens.nextToken(true);
        List<CaseStatementNode> cases = new ArrayList<>();
        while (tokens.tokenIs(Keyword.CASE, Keyword.DEFAULT)) {
            cases.add(CaseStatementNode.parse(tokens));
            tokens.passNewlines();
        }
        if (!tokens.tokenIs("}")) {
            throw tokens.error("Unexpected " + tokens.getFirst());
        }
        tokens.nextToken();
        return new SwitchStatementNode(info, switched, cases.toArray(new CaseStatementNode[0]));
    }

    @Override
    public String toString() {
        return String.format("switch %s %s", switched, cases.length == 0 ? "{}" : "{...}");
    }
}
