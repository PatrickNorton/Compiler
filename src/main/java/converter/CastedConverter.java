package main.java.converter;

import main.java.parser.ArgumentNode;
import main.java.parser.Lined;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class CastedConverter extends OperatorConverter {
    private final ArgumentNode[] args;
    private final Lined lineInfo;
    private final CompilerInfo info;
    private final int retCount;

    public CastedConverter(ArgumentNode[] args, Lined lineInfo, CompilerInfo info, int retCount) {
        this.args = args;
        this.lineInfo = lineInfo;
        this.info = info;
        this.retCount = retCount;
    }

    @Override
    @NotNull
    protected Pair<List<Byte>, TypeObject> convertWithAs(int start) {
         throw asException(lineInfo);
    }

    @Override
    @NotNull
    public TypeObject[] returnType() {
        if (args.length != 2) {
            throw argsException();
        } else if (retCount != 1) {
            throw retException();
        }
        var retType = TestConverter.returnType(args[1].getArgument(), info, 1)[0];
        if (retType instanceof TypeTypeObject) {
            return new TypeObject[] {((TypeTypeObject) retType).representedType()};
        } else {
            throw typeException(retType);
        }
    }

    @Override
    @NotNull
    public List<Byte> convert(int start) {
        if (args.length != 2) {
            throw argsException();
        } else if (retCount == 0) {
            CompilerWarning.warn("Cast is useless when not assigned to a value", lineInfo);
            return TestConverter.bytes(start, args[0].getArgument(), info, 0);
        } else if (retCount != 1) {
            throw retException();
        } else {
            return convertNormal(start);
        }
    }

    private List<Byte> convertNormal(int start) {
        var argConverter = TestConverter.of(info, args[0].getArgument(), 1);
        var typeConverter = TestConverter.of(info, args[1].getArgument(), 1);
        var retType = typeConverter.returnType()[0];
        if (!(retType instanceof TypeTypeObject)) {
            throw typeException(retType);
        }
        var representedType = ((TypeTypeObject) retType).representedType();
        var argType = argConverter.returnType()[0];
        if (representedType.isSuperclass(argType)) {
            CompilerWarning.warnf(
                    "Useless cast: %s is already a subclass of %s",
                    lineInfo, argType.name(), representedType.name()
            );
            return argConverter.convert(start);
        }
        List<Byte> bytes = new ArrayList<>(argConverter.convert(start));
        bytes.add(Bytecode.DUP_TOP.value);
        bytes.addAll(typeConverter.convert(start + bytes.size()));
        bytes.add(Bytecode.INSTANCEOF.value);
        bytes.add(Bytecode.JUMP.value);
        int jumpPos = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        bytes.add(Bytecode.LOAD_CONST.value);
        // FIXME: Better message
        var message = String.format("Cannot cast to type %s", representedType.name());
        bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(message))));
        bytes.add(Bytecode.THROW_QUICK.value);
        bytes.addAll(Util.shortToBytes((short) 1));
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpPos);
        return bytes;
    }

    private CompilerException retException() {
        return CompilerException.format(
                "casted operator only returns one value, not %d",
                lineInfo, retCount
        );
    }

    private CompilerException argsException() {
        return CompilerException.format(
                "casted operator only takes two operands, got %d",
                lineInfo, args.length
        );
    }

    private CompilerException typeException(TypeObject t) {
        return CompilerException.format(
                "Second argument of casted operator must be a type, not %s",
                lineInfo, t.name()
        );
    }
}
