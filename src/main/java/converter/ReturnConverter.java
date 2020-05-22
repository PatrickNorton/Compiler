package main.java.converter;

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
        checkReturnTypes();
        if (!node.getCond().isEmpty()) {
            bytes.addAll(TestConverter.bytes(start, node.getCond(), info, 1));
            var returnBytes = TestConverter.bytes(start, node.getReturned().get(0), info, node.getReturned().size());
            int jumpTarget = start + bytes.size() + Bytecode.JUMP_FALSE.size() + returnBytes.size() + Bytecode.RETURN.size();
            bytes.add(Bytecode.JUMP_FALSE.value);
            bytes.addAll(Util.intToBytes(jumpTarget));
            bytes.addAll(returnBytes);
        } else {
            if (node.getReturned().size() != 0) {
                bytes.addAll(TestConverter.bytes(start, node.getReturned().get(0), info, node.getReturned().size()));
            }
        }
        bytes.add(Bytecode.RETURN.value);
        bytes.addAll(Util.shortToBytes((short) node.getReturned().size()));
        return bytes;
    }

    private void checkReturnTypes() {
        var retInfo = info.getFnReturns();
        if (retInfo.notInFunction()) {
            throw CompilerException.of("Cannot return from here", node);
        }
        if (retInfo.isGenerator()) {
            if (node.getReturned().size() != 0) {
                throw CompilerException.of(
                        "Return with arguments invalid in generator",
                        node
                );
            } else {
                return;
            }
        }
        var fnReturns = retInfo.currentFnReturns();
        if (fnReturns.length != node.getReturned().size()) {  // TODO: Multi-returning values
            throw CompilerException.format("Incorrect number of values returned: expected %d, got %d",
                    node, fnReturns.length, node.getReturned().size());
        }
        for (int i = 0; i < fnReturns.length; i++) {
            var retType = TestConverter.returnType(node.getReturned().get(i), info, 1)[0];
            if (!fnReturns[i].isSuperclass(retType)) {
                throw CompilerException.format(
                        "Value returned in position %d, of type '%s', " +
                                "is not a subclass of the required return '%s'",
                        node.getReturned().get(i), i, retType.name(), fnReturns[i].name());
            }
        }
    }
}
