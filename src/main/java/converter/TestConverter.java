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
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public interface TestConverter extends BaseConverter {
    @NotNull
    TypeObject[] returnType();

    /**
     * Calculates the constant value if returned, or {@link Optional#empty()} if
     * no constant value exists.
     * <p>
     *     As an example: {@code parse("a").constantReturn()} will return {@code
     *     new StringConstant("a")}, but {@code
     *     parse(someFunc()).constantReturn()} will return {@link
     *     Optional#empty()}. (N.B. {@code parse()} is an arbitrary function
     *     that takes the code given and turns it into a parse tree.
     * </p>
     * <p>
     *     This method is guaranteed to return a non-empty value for bare
     *     constants, but what constant-value folding it can calculate is
     *     unspecified and may change at any point.
     * </p>
     *
     * @return The constant value, if it exists.
     */
    default Optional<LangConstant> constantReturn() {
        return Optional.empty();
    }

    @Override
    @NotNull
    default Pair<List<Byte>, DivergingInfo> convertAndReturn(int start) {
        var retTypes = returnType();
        var willThrow = Arrays.asList(retTypes).contains(Builtins.throwsType());
        return Pair.of(convert(start), willThrow ? new DivergingInfo().knownReturn() : new DivergingInfo());
    }

    @NotNull
    static List<Byte> bytes(int start, @NotNull TestNode node, CompilerInfo info, int retCount) {
        return of(info, node, retCount).convert(start);
    }

    @NotNull
    static TypeObject[] returnType(TestNode node, CompilerInfo info, int retCount) {
        return of(info, node, retCount).returnType();
    }

    static Optional<LangConstant> constantReturn(TestNode node, CompilerInfo info, int retCount) {
        return of(info, node, retCount).constantReturn();
    }

    static List<Byte> bytesMaybeOption(TestConverter converter, int start, TypeObject endType) {
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
    static List<Byte> bytesMaybeOption(int start, @NotNull TestNode node, CompilerInfo info,
                                       int retCount, TypeObject endType) {
        var converter = endType instanceof OptionTypeObject
                ? of(info, node, retCount) : of(info, node, retCount, endType);
        return bytesMaybeOption(converter, start, endType);
    }

    @NotNull
    static TestConverter of(CompilerInfo info, TestNode node, int retCount, TypeObject... expectedReturns) {
        if (node instanceof DictLiteralNode) {
            return new DictLiteralConverter(info, (DictLiteralNode) node, retCount, expectedReturns);
        } else if (node instanceof LambdaNode) {
            return new LambdaConverter(info, (LambdaNode) node, retCount, expectedReturns);
        } else if (node instanceof LiteralNode) {
            return new LiteralConverter(info, (LiteralNode) node, retCount, expectedReturns);
        }
        return of(info, node, retCount);
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
            return OperatorConverter.of(info, (OperatorNode) node, retCount);
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
            throw CompilerInternalError.format("Unknown type for TestConverter: %s", node, node.getClass());
        }
    }
}
