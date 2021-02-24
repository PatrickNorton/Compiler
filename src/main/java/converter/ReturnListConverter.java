package main.java.converter;

import main.java.parser.FunctionCallNode;
import main.java.parser.Lined;
import main.java.parser.TestListNode;
import main.java.parser.TestNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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
        if (retTypes.length > 1 && values.size() == 1) {
            return convertSingle(start);
        } else if (canTailCall()) {
            return convertTailCall(start);
        } else {
            return convertMultiple(start);
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

    private List<Byte> convertMultiple(int start) {
        List<Byte> bytes = new ArrayList<>();
        checkReturnTypes();
        if (retTypes.length == 1) {
            bytes.addAll(TestConverter.bytesMaybeOption(start, values.get(0), info, 1, retTypes[0]));
        } else if (!values.isEmpty()) {
            for (var ret : values) {
                bytes.addAll(convertInner(start, ret.getKey(), ret.getValue()));
            }
        }
        bytes.add(value.value);
        bytes.addAll(Util.shortToBytes((short) retTypes.length));
        return bytes;
    }

    private List<Byte> convertInner(int start, TestNode stmt, String vararg) {
        switch (vararg) {
            case "":
                return TestConverter.bytes(start, stmt, info, 1);
            case "*":
                var retType = TestConverter.returnType(stmt, info, 1)[0];
                if (retType.sameBaseType(Builtins.tuple())) {
                    throw CompilerTodoError.format("Cannot convert %s with varargs yet", stmt, returnName());
                } else if (Builtins.iterable().isSuperclass(retType)) {
                    throw CompilerException.format("Cannot unpack iterable in %s statement", stmt, returnName());
                } else {
                    throw CompilerException.format(
                            "Can only unpack tuples in %s statement, not '%s'", stmt, returnName(), retType.name()
                    );
                }
            case "**":
                throw CompilerException.format("Cannot unpack dictionaries in %s statement", stmt, returnName());
            default:
                throw CompilerInternalError.format("Unknown splat type '%s'", stmt, vararg);
        }
    }

    private List<Byte> convertSingle(int start) {
        assert values.size() == 1;
        var converter = TestConverter.of(info, values.get(0), retTypes.length);
        var retTypes = converter.returnType();
        checkSingleReturn(retTypes);
        List<Byte> bytes = new ArrayList<>(converter.convert(start));
        for (int i = retTypes.length - 1; i >= 0; i--) {
            if (OptionTypeObject.needsMakeOption(retTypes[i], retTypes[i])) {
                int distFromTop = retTypes.length - i - 1;
                addSwap(bytes, distFromTop);
                bytes.add(Bytecode.MAKE_OPTION.value);
                addSwap(bytes, distFromTop);
            }
        }
        bytes.add(value.value);
        bytes.addAll(Util.shortToBytes((short) retTypes.length));
        return bytes;
    }

    private void addSwap(List<Byte> bytes, int distFromTop) {
        switch (distFromTop) {
            case 0:
                return;
            case 1:
                bytes.add(Bytecode.SWAP_2.value);
                return;
            default:
                bytes.add(Bytecode.SWAP_STACK.value);
                bytes.addAll(Util.shortZeroBytes());
                bytes.addAll(Util.shortToBytes((short) distFromTop));
        }
    }

    private List<Byte> convertTailCall(int start) {
        assert canTailCall();
        var node = (FunctionCallNode) values.get(0);
        var converter = new FunctionCallConverter(info, node, 1);
        var retType = converter.returnType()[0];
        if (!retTypes[0].isSuperclass(retType)) {
            throw typeError(values.get(0), 0, retTypes[0], retType);
        }
        List<Byte> bytes = new ArrayList<>(converter.convertTail(start));
        bytes.add(Bytecode.RETURN.value);  // Necessary b/c tail-call may delegate to normal call at runtime
        bytes.addAll(Util.shortToBytes((short) 1));
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
