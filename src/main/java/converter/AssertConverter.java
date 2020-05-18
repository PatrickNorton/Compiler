package main.java.converter;

import main.java.parser.AssertStatementNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class AssertConverter implements BaseConverter {
    private final AssertStatementNode node;
    private final CompilerInfo info;

    public AssertConverter(CompilerInfo info, AssertStatementNode node) {
        this.info = info;
        this.node = node;
    }

    @Override
    @NotNull
    public List<Byte> convert(int start) {
        // TODO: Debug vs release
        var bytes = new ArrayList<>(TestConverter.bytes(start, node.getAssertion(), info, 1));
        bytes.add(Bytecode.JUMP_FALSE.value);
        int jumpLoc = bytes.size();
        bytes.addAll(Util.intToBytes(0));
        bytes.addAll(convertMessage(start + bytes.size()));
        bytes.add(Bytecode.LOAD_CONST.value);
        bytes.addAll(Util.shortToBytes(info.constIndex(Builtins.constantOf("str"))));
        bytes.add(Bytecode.THROW_QUICK.value);
        bytes.addAll(Util.shortZeroBytes());
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpLoc);
        return bytes;
    }

    private @NotNull List<Byte> convertMessage(int start) {
        if (node.getAs().isEmpty()) {
            List<Byte> bytes = new ArrayList<>();
            bytes.add(Bytecode.LOAD_CONST.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(""))));
            return bytes;
        } else {
            var converter = TestConverter.of(info, node.getAs(), 1);
            var retType = converter.returnType()[0];
            if (!Builtins.STR.isSuperclass(retType)) {
                throw CompilerException.format(
                        "'as' clause in an assert statement must return a 'str', not '%s'",
                        node.getAs(), retType.name()
                );
            }
            return converter.convert(start);
        }
    }
}
