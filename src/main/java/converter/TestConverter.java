package main.java.converter;

import main.java.parser.FunctionCallNode;
import main.java.parser.TestNode;
import org.jetbrains.annotations.NotNull;

public interface TestConverter extends BaseConverter {
    TypeObject returnType();

    @NotNull
    static TestConverter of(CompilerInfo info, @NotNull TestNode node) {
        if (node instanceof FunctionCallNode) {
            return new FunctionCallConverter(info, (FunctionCallNode) node);
        } else {
            throw new UnsupportedOperationException("Unknown type: " + node.getClass());
        }
    }
}
