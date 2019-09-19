package Parser;

import org.jetbrains.annotations.Contract;

import java.util.StringJoiner;

/**
 * The class representing a function call.
 * <p>
 *     This class is for function calls within code, not function definitions
 *     or anything else that might look like it could be parsed using this.
 *     This class does not have any sort of parse method, that is all handled
 *     by {@link NameNode#parse}.
 * </p>
 */
public class FunctionCallNode implements NameNode, EnumKeywordNode {
    private TestNode caller;
    private ArgumentNode[] parameters;

    @Contract(pure = true)
    public FunctionCallNode(TestNode caller, ArgumentNode[] parameters) {
        this.caller = caller;
        this.parameters = parameters;
    }

    public TestNode getCaller() {
        return caller;
    }

    public ArgumentNode[] getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", ");
        for (ArgumentNode a : parameters) {
            sj.add(a.toString());
        }
        return caller + "(" + sj + ")";
    }
}
