package main.java.converter;

import main.java.parser.Lined;
import main.java.parser.TestListNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class ReturnListConverter implements BaseConverter {
    private final TestListNode values;
    private final CompilerInfo info;
    private final TypeObject[] retTypes;
    private final Bytecode value;

    public ReturnListConverter(TestListNode values, CompilerInfo info, TypeObject[] retTypes, Bytecode value) {
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
        } else {
            return convertMultiple(start);
        }
    }

    private List<Byte> convertMultiple(int start) {
        List<Byte> bytes = new ArrayList<>();
        checkReturnTypes();
        if (retTypes.length == 1) {
            bytes.addAll(TestConverter.bytesMaybeOption(start, values.get(0), info, 1, retTypes[0]));
        } else if (!values.isEmpty()) {
            for (var ret : values) {
                if (!ret.getValue().isEmpty()) {
                    throw CompilerTodoError.format("Cannot convert return with varargs yet", ret.getKey());
                }
                bytes.addAll(TestConverter.bytes(start, ret.getKey(), info, 1));
            }
        }
        bytes.add(value.value);
        bytes.addAll(Util.shortToBytes((short) retTypes.length));
        return bytes;
    }

    private List<Byte> convertSingle(int start) {
        assert values.size() == 1;
        var retInfo = info.getFnReturns();
        var fnReturns = retInfo.currentFnReturns();
        var converter = TestConverter.of(info, values.get(0), fnReturns.length);
        var retTypes = converter.returnType();
        checkSingleReturn(retTypes);
        List<Byte> bytes = new ArrayList<>(converter.convert(start));
        for (int i = fnReturns.length - 1; i >= 0; i--) {
            if (OptionTypeObject.needsMakeOption(retTypes[i], fnReturns[i])) {
                int distFromTop = fnReturns.length - i - 1;
                addSwap(bytes, distFromTop);
                bytes.add(Bytecode.MAKE_OPTION.value);
                addSwap(bytes, distFromTop);
            }
        }
        bytes.add(Bytecode.RETURN.value);
        bytes.addAll(Util.shortToBytes((short) fnReturns.length));
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

    private void checkReturnTypes() {
        assert !values.isEmpty();
        if (retTypes.length != values.size()) {
            throw CompilerException.format("Incorrect number of values returned: expected %d, got %d",
                    values, retTypes.length, values.size());
        }
        for (int i = 0; i < retTypes.length; i++) {
            var retType = TestConverter.returnType(values.get(i), info, 1)[0];
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
                    "Value given does not return enough values: expected at least %d, got %d",
                    values.get(0), fnReturns.length, returns.length
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

    private static boolean badType(@NotNull TypeObject fnRet, TypeObject retType) {
        if (fnRet.isSuperclass(retType)) {
            return false;
        } else if (OptionTypeObject.needsMakeOption(fnRet, retType)) {
            return !fnRet.isSuperclass(TypeObject.optional(retType)) && !retType.sameBaseType(Builtins.NULL_TYPE);
        } else {
            return true;
        }
    }

    @NotNull
    private static CompilerException typeError(
            Lined lined, int index, @NotNull TypeObject retType, @NotNull TypeObject fnRet
    ) {
        return CompilerException.format(
                "Value returned in position %d, of type '%s', is not a subclass of the required return '%s'",
                lined, index, retType.name(), fnRet.name()
        );
    }
}
