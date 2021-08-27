package main.java.converter;

import main.java.parser.ArgumentNode;
import main.java.parser.Lined;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

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
    protected Pair<BytecodeList, TypeObject> convertWithAs() {
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
    public BytecodeList convert() {
        if (args.length != 2) {
            throw argsException();
        } else if (retCount == 0) {
            CompilerWarning.warn("Cast is useless when not assigned to a value", WarningType.UNUSED, info, lineInfo);
            return TestConverter.bytes(args[0].getArgument(), info, 0);
        } else if (retCount != 1) {
            throw retException();
        } else {
            return convertNormal();
        }
    }

    @NotNull
    private BytecodeList convertNormal() {
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
                    WarningType.TRIVIAL_VALUE, info,
                    lineInfo, argType.name(), representedType.name()
            );
            return argConverter.convert();
        }
        var bytes = new BytecodeList(argConverter.convert());
        bytes.add(Bytecode.DUP_TOP);
        bytes.addAll(typeConverter.convert());
        bytes.add(Bytecode.INSTANCEOF);
        var jumpLbl = info.newJumpLabel();
        bytes.add(Bytecode.JUMP, jumpLbl);
        var message = String.format("Cannot cast to type %s", representedType.name());
        bytes.add(Bytecode.LOAD_CONST, info.constIndex(LangConstant.of(message)));
        bytes.add(Bytecode.THROW_QUICK, 1);
        bytes.addLabel(jumpLbl);
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
