package main.java.converter;

import main.java.parser.FunctionCallNode;
import main.java.parser.Lined;
import main.java.parser.TestListNode;
import main.java.parser.TestNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ReturnListConverter implements BaseConverter {
    private final TestListNode values;
    private final CompilerInfo info;
    private final TypeObject[] retTypes;
    private final Bytecode value;

    public ReturnListConverter(TestListNode values, CompilerInfo info, TypeObject[] retTypes, Bytecode value) {
        assert value == Bytecode.RETURN || value == Bytecode.YIELD : String.format("Invalid bytecode value %s", value);
        this.values = values;
        this.info = info;
        this.retTypes = retTypes;
        this.value = value;
    }

    @Override
    @NotNull
    public List<Byte> convert(int start) {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public BytecodeList convert() {
        if (retTypes.length > 1 && values.size() == 1) {
            return convertSingle();
        } else if (canTailCall()) {
            return convertTailCall();
        } else {
            return convertMultiple();
        }
    }

    private boolean canTailCall() {
        return value == Bytecode.RETURN && retTypes.length == 1 && values.size() == 1
                && values.getVararg(0).isEmpty()
                && values.get(0) instanceof FunctionCallNode
                && !firstNeedsOption();
    }

    private boolean firstNeedsOption() {
        return OptionTypeObject.needsMakeOption(
                retTypes[0], TestConverter.returnType(values.get(0), info, 1)[0]
        );
    }

    @NotNull
    private BytecodeList convertMultiple() {
        var bytes = new BytecodeList();
        checkReturnTypes();
        if (retTypes.length == 1) {
            bytes.addAll(TestConverter.bytesMaybeOption(values.get(0), info, 1, retTypes[0]));
        } else if (!values.isEmpty()) {
            for (int i = 0; i < values.size(); i++) {
                var retType = retTypes[i];
                bytes.addAll(convertInner(values.get(i), values.getVararg(i), retType));
            }
        }
        bytes.add(value, retTypes.length);
        return bytes;
    }

    @NotNull
    private BytecodeList convertInner(TestNode stmt, @NotNull String vararg, TypeObject retType) {
        switch (vararg) {
            case "":
                return TestConverter.bytesMaybeOption(stmt, info, 1, retType);
            case "*":
                var returnType = TestConverter.returnType(stmt, info, 1)[0];
                if (returnType.sameBaseType(Builtins.tuple())) {
                    throw CompilerTodoError.format("Cannot convert %s with varargs yet", stmt, returnName());
                } else if (Builtins.iterable().isSuperclass(returnType)) {
                    throw CompilerException.format("Cannot unpack iterable in %s statement", stmt, returnName());
                } else {
                    throw CompilerException.format(
                            "Can only unpack tuples in %s statement, not '%s'", stmt, returnName(), returnType.name()
                    );
                }
            case "**":
                throw CompilerException.format("Cannot unpack dictionaries in %s statement", stmt, returnName());
            default:
                throw CompilerInternalError.format("Unknown splat type '%s'", stmt, vararg);
        }
    }

    @NotNull
    private BytecodeList convertSingle() {
        assert values.size() == 1;
        var converter = TestConverter.of(info, values.get(0), retTypes.length);
        var retTypes = converter.returnType();
        checkSingleReturn(retTypes);
        BytecodeList bytes = new BytecodeList(converter.convert());
        for (int i = retTypes.length - 1; i >= 0; i--) {
            if (OptionTypeObject.needsMakeOption(retTypes[i], retTypes[i])) {
                int distFromTop = retTypes.length - i - 1;
                addSwap(bytes, distFromTop);
                bytes.add(Bytecode.MAKE_OPTION);
                addSwap(bytes, distFromTop);
            }
        }
        bytes.add(value, retTypes.length);
        return bytes;
    }

    private void addSwap(BytecodeList bytes, int distFromTop) {
        switch (distFromTop) {
            case 0:
                return;
            case 1:
                bytes.add(Bytecode.SWAP_2);
                return;
            default:
                bytes.add(Bytecode.SWAP_STACK, 0, distFromTop);
        }
    }

    @NotNull
    private BytecodeList convertTailCall() {
        assert canTailCall();
        var node = (FunctionCallNode) values.get(0);
        var converter = new FunctionCallConverter(info, node, 1);
        var retType = converter.returnType()[0];
        if (!retTypes[0].isSuperclass(retType)) {
            throw typeError(values.get(0), 0, retTypes[0], retType);
        }
        BytecodeList bytes = new BytecodeList(converter.convertTail());
        bytes.add(Bytecode.RETURN, 1);  // Necessary b/c tail-call may delegate to normal call at runtime
        return bytes;
    }

    private void checkReturnTypes() {
        assert !values.isEmpty();
        if (retTypes.length != values.size()) {
            throw CompilerException.format("Incorrect number of values %sed: expected %d, got %d",
                    values.get(0), returnName(), retTypes.length, values.size());
        }
        for (int i = 0; i < retTypes.length; i++) {
            var converter = TestConverter.of(info, values.get(i), 1, retTypes[i]);
            var retType = converter.returnType()[0];
            if (badType(retTypes[i], retType)) {
                throw typeError(values.get(i), i, retType, retTypes[i]);
            }
        }
    }

    private void checkSingleReturn(@NotNull TypeObject[] returns) {
        assert values.size() == 1;
        var retInfo = info.getFnReturns();
        var fnReturns = retInfo.currentFnReturns();
        if (returns.length < fnReturns.length) {
            throw CompilerException.format(
                    "Value given does not %s enough values: expected at least %d, got %d",
                    values.get(0), returnName(), fnReturns.length, returns.length
            );
        }
        for (int i = 0; i < fnReturns.length; i++) {
            var fnRet = fnReturns[i];
            var retType = returns[i];
            if (badType(fnRet, retType)) {
                throw typeError(values.get(0), i, retType, fnRet);
            }
        }
    }

    private String returnName() {
        switch (value) {
            case RETURN:
                return "return";
            case YIELD:
                return "yield";
            default:
                throw CompilerInternalError.format(
                        "Unknown bytecode value for ReturnListConverter: %s", values, value
                );
        }
    }

    private static boolean badType(@NotNull TypeObject fnRet, TypeObject retType) {
        if (fnRet.isSuperclass(retType)) {
            return false;
        } else if (OptionTypeObject.needsMakeOption(fnRet, retType)) {
            return !OptionTypeObject.superWithOption(fnRet, retType);
        } else {
            return true;
        }
    }

    @NotNull
    private CompilerException typeError(
            Lined lined, int index, @NotNull TypeObject retType, @NotNull TypeObject fnRet
    ) {
        return CompilerException.format(
                "Value %sed in position %d, of type '%s', is not a subclass of the required return '%s'",
                lined, returnName(), index, retType.name(), fnRet.name()
        );
    }
}
