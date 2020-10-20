package main.java.converter;

import main.java.parser.Lined;
import main.java.parser.ReturnStatementNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class ReturnConverter implements BaseConverter {
    private final ReturnStatementNode node;
    private final CompilerInfo info;

    public ReturnConverter(CompilerInfo info, ReturnStatementNode node) {
        this.info = info;
        this.node = node;
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        List<Byte> bytes = new ArrayList<>();
        int jumpPos;
        if (!node.getCond().isEmpty()) {
            bytes.addAll(TestConverter.bytes(start, node.getCond(), info, 1));
            bytes.add(Bytecode.JUMP_FALSE.value);
            jumpPos = bytes.size();
            bytes.addAll(Util.zeroToBytes());
        } else {
            jumpPos = -1;
        }
        var retInfo = info.getFnReturns();
        if (retInfo.notInFunction()) {
            throw CompilerException.of("Cannot return from here", node);
        } else if (retInfo.isGenerator() && !node.getReturned().isEmpty()) {
            throw CompilerException.of("Return with arguments invalid in generator", node);
        }
        var fnReturns = retInfo.currentFnReturns();
        if (fnReturns.length == 0 || retInfo.isGenerator()) {  // Zero-returning functions are easy to deal with
            if (!node.getReturned().isEmpty()) {
                throw CompilerException.of(
                        "Non-empty 'return' statement invalid in function with no return types", node
                );
            } else {
                bytes.add(Bytecode.RETURN.value);
                bytes.addAll(Util.zeroToBytes());
            }
        } else if (fnReturns.length > 1 && node.getReturned().size() == 1) {
            convertSingle(start, bytes);
        } else {
            convertMultiple(start, bytes);
        }
        if (jumpPos != -1) {
            Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpPos);
        }
        return bytes;
    }

    private void convertMultiple(int start, @NotNull List<Byte> bytes) {
        if (node.getReturned().isEmpty()) {
            throw CompilerException.of("Empty return is invalid in non-void function", node);
        }
        checkReturnTypes();
        var fnReturns = info.getFnReturns();
        var currentReturns = fnReturns.currentFnReturns();
        if (currentReturns.length == 1) {
            bytes.addAll(TestConverter.bytesMaybeOption(start, node.getReturned().get(0), info, 1, currentReturns[0]));
        } else if (!node.getReturned().isEmpty()) {
            for (var ret : node.getReturned()) {
                if (!ret.getValue().isEmpty()) {
                    throw CompilerTodoError.format("Cannot convert return with varargs yet", ret.getKey());
                }
                bytes.addAll(TestConverter.bytes(start, ret.getKey(), info, 1));
            }
        }
        bytes.add(Bytecode.RETURN.value);
        bytes.addAll(Util.shortToBytes((short) currentReturns.length));
    }

    private void convertSingle(int start, @NotNull List<Byte> bytes) {
        assert node.getReturned().size() == 1;
        var retInfo = info.getFnReturns();
        var fnReturns = retInfo.currentFnReturns();
        var converter = TestConverter.of(info, node.getReturned().get(0), fnReturns.length);
        var retTypes = converter.returnType();
        checkSingleReturn(retTypes);
        bytes.addAll(converter.convert(start + bytes.size()));
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
        assert !node.getReturned().isEmpty();
        var retInfo = info.getFnReturns();
        assert !retInfo.notInFunction();
        var fnReturns = retInfo.currentFnReturns();
        if (fnReturns.length != node.getReturned().size()) {  // TODO: Multi-returning values
            throw CompilerException.format("Incorrect number of values returned: expected %d, got %d",
                    node, fnReturns.length, node.getReturned().size());
        }
        for (int i = 0; i < fnReturns.length; i++) {
            var retType = TestConverter.returnType(node.getReturned().get(i), info, 1)[0];
            if (badType(fnReturns[i], retType)) {
                throw typeError(node.getReturned().get(i), i, retType, fnReturns[i]);
            }
        }
    }

    private void checkSingleReturn(@NotNull TypeObject[] returns) {
        assert node.getReturned().size() == 1;
        var retInfo = info.getFnReturns();
        var fnReturns = retInfo.currentFnReturns();
        if (returns.length < fnReturns.length) {
            throw CompilerException.format(
                    "Value given does not return enough values: expected at least %d, got %d",
                    node, fnReturns.length, returns.length
            );
        }
        for (int i = 0; i < fnReturns.length; i++) {
            var fnRet = fnReturns[i];
            var retType = returns[i];
            if (badType(fnRet, retType)) {
                throw typeError(node.getReturned().get(0), i, retType, fnRet);
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
