package main.java.converter;

import main.java.parser.ComprehensionNode;
import main.java.parser.DictComprehensionNode;
import main.java.parser.DictLiteralNode;
import main.java.parser.DottedVariableNode;
import main.java.parser.FormattedStringNode;
import main.java.parser.FunctionCallNode;
import main.java.parser.IndexNode;
import main.java.parser.LambdaNode;
import main.java.parser.LiteralNode;
import main.java.parser.NumberNode;
import main.java.parser.OperatorNode;
import main.java.parser.RaiseStatementNode;
import main.java.parser.RangeLiteralNode;
import main.java.parser.StringNode;
import main.java.parser.SwitchStatementNode;
import main.java.parser.TernaryNode;
import main.java.parser.TestNode;
import main.java.parser.VariableNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public interface TestConverter extends BaseConverter {
    @NotNull
    TypeObject[] returnType();

    @NotNull
    static List<Byte> bytes(int start, @NotNull TestNode node, CompilerInfo info, int retCount) {
        return of(info, node, retCount).convert(start);
    }

    @NotNull
    static TypeObject[] returnType(TestNode node, CompilerInfo info, int retCount) {
        return of(info, node, retCount).returnType();
    }

    @NotNull
    static List<Byte> bytesMaybeOption(int start, @NotNull TestNode node, CompilerInfo info,
                                       int retCount, TypeObject endType) {
        var converter = of(info, node, retCount);
        var retType = converter.returnType()[0];
        if (endType instanceof OptionTypeObject && !(retType instanceof OptionTypeObject)) {
            List<Byte> bytes = new ArrayList<>(converter.convert(start));
            bytes.add(Bytecode.MAKE_OPTION.value);
            return bytes;
        } else {
            return converter.convert(start);
        }
    }

    @NotNull
    static TestConverter of(CompilerInfo info, @NotNull TestNode node, int retCount) {
        if (node instanceof ComprehensionNode) {
            return new ComprehensionConverter(info, (ComprehensionNode) node, retCount);
        } else if (node instanceof DictComprehensionNode) {
            return new DictCompConverter(info, (DictComprehensionNode) node, retCount);
        } else if (node instanceof DictLiteralNode) {
            return new DictLiteralConverter(info, (DictLiteralNode) node, retCount);
        } else if (node instanceof DottedVariableNode) {
            return new DotConverter(info, (DottedVariableNode) node, retCount);
        } else if (node instanceof FormattedStringNode) {
            return new FormattedStringConverter(info, (FormattedStringNode) node, retCount);
        } else if (node instanceof FunctionCallNode) {
            return new FunctionCallConverter(info, (FunctionCallNode) node, retCount);
        } else if (node instanceof IndexNode) {
            return new IndexConverter(info, (IndexNode) node, retCount);
        } else if (node instanceof LambdaNode) {
            return new LambdaConverter(info, (LambdaNode) node, retCount);
        } else if (node instanceof LiteralNode) {
            return new LiteralConverter(info, (LiteralNode) node, retCount);
        } else if (node instanceof NumberNode) {
            return new NumberConverter(info, (NumberNode) node, retCount);
        } else if (node instanceof OperatorNode) {
            return new OperatorConverter(info, (OperatorNode) node, retCount);
        } else if (node instanceof RaiseStatementNode) {
            return new RaiseConverter(info, (RaiseStatementNode) node, retCount);
        } else if (node instanceof RangeLiteralNode) {
            return new RangeConverter(info, (RangeLiteralNode) node, retCount);
        } else if (node instanceof StringNode) {
            return new StringConverter(info, (StringNode) node, retCount);
        } else if (node instanceof SwitchStatementNode) {
            return new SwitchConverter(info, (SwitchStatementNode) node, retCount);
        } else if (node instanceof TernaryNode) {
            return new TernaryConverter(info, (TernaryNode) node, retCount);
        } else if (node instanceof VariableNode) {
            return new VariableConverter(info, (VariableNode) node, retCount);
        } else if (node instanceof VariantCreationNode) {
            return new VariantConverter(info, (VariantCreationNode) node, retCount);
        } else {
            throw new UnsupportedOperationException("Unknown type: " + node.getClass());
        }
    }
}
