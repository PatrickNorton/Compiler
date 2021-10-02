package main.java.converter;

import main.java.converter.bytecode.ArgcBytecode;
import main.java.parser.AssertStatementNode;
import org.jetbrains.annotations.NotNull;

public final class AssertConverter implements BaseConverter {
    private final AssertStatementNode node;
    private final CompilerInfo info;

    public AssertConverter(CompilerInfo info, AssertStatementNode node) {
        this.info = info;
        this.node = node;
    }

    @Override
    @NotNull
    public BytecodeList convert() {
        // TODO: Debug vs release
        var bytes = new BytecodeList(TestConverter.bytes(node.getAssertion(), info, 1));
        var jumpTag = info.newJumpLabel();
        bytes.add(Bytecode.JUMP_TRUE, jumpTag);
        bytes.loadConstant(Builtins.assertionErrorConstant(), info);
        bytes.addAll(convertMessage());
        bytes.add(Bytecode.THROW_QUICK, ArgcBytecode.zero());
        bytes.addLabel(jumpTag);
        return bytes;
    }

    @NotNull
    private BytecodeList convertMessage() {
        if (node.getAs().isEmpty()) {
            var bytes = new BytecodeList();
            bytes.loadConstant(LangConstant.of("Assertion failed"), info);
            return bytes;
        } else {
            var converter = TestConverter.of(info, node.getAs(), 1);
            var retType = converter.returnType()[0];
            if (!Builtins.str().isSuperclass(retType)) {
                throw CompilerException.format(
                        "'as' clause in an assert statement must return a 'str', not '%s'",
                        node.getAs(), retType.name()
                );
            }
            return converter.convert();
        }
    }
}
