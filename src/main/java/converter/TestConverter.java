package main.java.converter;

import main.java.parser.FunctionCallNode;
import main.java.parser.NumberNode;
import main.java.parser.StringNode;
import main.java.parser.SwitchStatementNode;
import main.java.parser.TestNode;
import main.java.parser.VariableNode;
import org.jetbrains.annotations.NotNull;

public interface TestConverter extends BaseConverter {
    TypeObject returnType();

    @NotNull
    static TestConverter of(CompilerInfo info, @NotNull TestNode node) {
        if (node instanceof FunctionCallNode) {
            return new FunctionCallConverter(info, (FunctionCallNode) node);
        } else if (node instanceof NumberNode) {
            return new NumberConverter(info, (NumberNode) node);
        } else if (node instanceof StringNode) {
            return new StringConverter(info, (StringNode) node);
        } else if (node instanceof SwitchStatementNode) {
            return new SwitchConverter(info, (SwitchStatementNode) node);
        } else if (node instanceof VariableNode) {
            return new VariableConverter(info, (VariableNode) node);
        } else {
            throw new UnsupportedOperationException("Unknown type: " + node.getClass());
        }
    }
}
