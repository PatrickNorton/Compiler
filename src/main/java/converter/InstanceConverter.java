package main.java.converter;

import main.java.parser.ArgumentNode;
import main.java.parser.Lined;
import main.java.util.Pair;

import java.util.ArrayList;
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

    public TypeObject[] returnType() {
        return new TypeObject[] {Builtins.BOOL};
    }

    @Override

    public List<Byte> convert(int start) {
        return convertInner(start, false).getKey();
    }

    protected Pair<List<Byte>, TypeObject> convertWithAs(int start) {
        return convertInner(start, true);
    }

    private Pair<List<Byte>, TypeObject> convertInner(int start, boolean dupFirst) {
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
        if (!Builtins.TYPE.isSuperclass(arg1ret)) {
            throw CompilerException.of(
                    "'instanceof' operator requires second argument to be an instance of 'type'", arg1
            );
        }
        var instanceCls = ((TypeTypeObject) arg1ret).representedType();
        var bytes = new ArrayList<>(TestConverter.bytes(start, arg0, info, 1));
        if (dupFirst) {
            bytes.add(Bytecode.DUP_TOP.value);
        }
        bytes.addAll(converter1.convert(start + bytes.size()));
        bytes.add(Bytecode.INSTANCEOF.value);
        if (!instanceType) {
            bytes.add(Bytecode.BOOL_NOT.value);
        }
        return Pair.of(bytes, instanceCls);
    }
}
