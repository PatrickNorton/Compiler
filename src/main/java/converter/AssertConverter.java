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
        if (info.globalInfo().isDebug()) {
            var converter = TestConverter.of(info, node.getAssertion(), 1);
            var constantRet = converter.constantReturn();
            if (constantRet.isPresent()) {
                if (constantRet.orElseThrow().boolValue().isPresent()) {
                    var constant = constantRet.orElseThrow().boolValue().orElseThrow();
                    var bytes = new BytecodeList();
                    if (constant) {
                        CompilerWarning.warn(
                                "Value in assertion is always true", WarningType.TRIVIAL_VALUE,
                                info, node.getAssertion()
                        );
                    } else {
                        bytes.loadConstant(Builtins.assertionErrorConstant(), info);
                        bytes.addAll(convertMessage());
                        bytes.add(Bytecode.THROW_QUICK, ArgcBytecode.one());
                    }
                    return bytes;
                } else {
                    return convertStandard(converter);
                }
            } else {
                return convertStandard(converter);
            }
        } else {
            return new BytecodeList();
        }
    }

    @NotNull
    private BytecodeList convertStandard(@NotNull TestConverter converter) {
        var bytes = new BytecodeList(converter.convert());
        var jumpTag = info.newJumpLabel();
        bytes.add(Bytecode.JUMP_TRUE, jumpTag);
        bytes.loadConstant(Builtins.assertionErrorConstant(), info);
        bytes.addAll(convertMessage());
        bytes.add(Bytecode.THROW_QUICK, ArgcBytecode.one());
        bytes.addLabel(jumpTag);
        return bytes;
    }

    @NotNull
    private BytecodeList convertMessage() {
        if (node.getAs().isEmpty()) {
            var bytes = new BytecodeList(1);
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
