package main.java.converter;

import main.java.parser.ArgumentNode;
import main.java.parser.Lined;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class InstanceConverter extends OperatorConverter {
    private final boolean instanceType;
    private final ArgumentNode[] operands;
    private final Lined lineInfo;
    private final CompilerInfo info;
    private final int retCount;

    public InstanceConverter(
            boolean instanceType, ArgumentNode[] operands, Lined lineInfo, CompilerInfo info, int retCount
    ) {
        this.instanceType = instanceType;
        this.operands = operands;
        this.lineInfo = lineInfo;
        this.info = info;
        this.retCount = retCount;
    }

    @Override
    @NotNull
    public TypeObject[] returnType() {
        return new TypeObject[] {Builtins.bool()};
    }

    @Override
    @NotNull
    public List<Byte> convert(int start) {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public BytecodeList convert() {
        return convertInner(false).getKey();
    }

    @NotNull
    protected Pair<BytecodeList, TypeObject> convertWithAs() {
        return convertInner(true);
    }

    @NotNull
    private Pair<BytecodeList, TypeObject> convertInner(boolean dupFirst) {
        if (operands.length != 2) {
            throw CompilerException.format(
                    "'instanceof' operator requires 2 arguments, not %d",
                    lineInfo, operands.length
            );
        } else if (retCount > 1) {
            throw CompilerException.format("'instanceof' operator returns 1 value, not %d", lineInfo, retCount);
        }
        var arg0 = operands[0].getArgument();
        var arg1 = operands[1].getArgument();
        var converter1 = TestConverter.of(info, arg1, 1);
        var arg1ret = converter1.returnType()[0];
        if (!Builtins.type().isSuperclass(arg1ret)) {
            throw CompilerException.of(
                    "'instanceof' operator requires second argument to be an instance of 'type'", arg1
            );
        }
        var instanceCls = ((TypeTypeObject) arg1ret).representedType();
        var bytes = new BytecodeList(TestConverter.bytes(arg0, info, 1));
        if (dupFirst) {
            bytes.add(Bytecode.DUP_TOP);
        }
        bytes.addAll(converter1.convert());
        bytes.add(Bytecode.INSTANCEOF);
        if (!instanceType) {
            bytes.add(Bytecode.BOOL_NOT);
        }
        return Pair.of(bytes, instanceCls);
    }
}
