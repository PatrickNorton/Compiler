package main.java.converter;

import main.java.parser.ArgumentNode;
import main.java.parser.Lined;
import main.java.parser.OperatorTypeNode;
import main.java.parser.VariableNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class IsConverter extends OperatorConverter {
    private final boolean isType;
    private final ArgumentNode[] operands;
    private final Lined lineInfo;
    private final CompilerInfo info;
    private final int retCount;

    public IsConverter(
            boolean isType,
            ArgumentNode[] operands,
            Lined lineInfo,
            CompilerInfo info,
            int retCount
    ) {
        this.isType = isType;
        this.operands = operands;
        this.lineInfo = lineInfo;
        this.info = info;
        this.retCount = retCount;
    }

    @Override
    public Optional<LangConstant> constantReturn() {
        var op = isType ? OperatorTypeNode.IS : OperatorTypeNode.IS_NOT;
        return defaultConstant(op, info, operands);
    }

    @Override
    @NotNull
    public TypeObject[] returnType() {
        return new TypeObject[] {Builtins.bool()};
    }

    @Override
    @NotNull
    public BytecodeList convert() {
        if (retCount > 1) {
            throw CompilerException.format("'is' only returns 1 value, %d expected", lineInfo, retCount);
        }
        switch (operands.length) {
            case 0:
            case 1:
                return convert0();
            case 2:
                return convert2();
            default:
                return convertMany();
        }
    }

    @NotNull
    private BytecodeList convert0() {
        assert operands.length == 0 || operands.length == 1;
        CompilerWarning.warnf(
                "'%s' with < 2 operands will always be %b",
                WarningType.TRIVIAL_VALUE, info,
                lineInfo, isType ? "is" : "is not", isType
        );
        // Have to get side-effects
        var bytes = new BytecodeList(TestConverter.bytes(operands[0].getArgument(), info, 1));
        bytes.add(Bytecode.POP_TOP);
        bytes.add(Bytecode.LOAD_CONST, info.constIndex(LangConstant.of(isType)));
        return bytes;
    }

    private BytecodeList convert2() {
        assert operands.length == 2;
        var constBytes = getConstant();
        if (constBytes != null) return constBytes;
        var bytes = new BytecodeList(TestConverter.bytes(operands[0].getArgument(), info, 1));
        bytes.addAll(TestConverter.bytes(operands[1].getArgument(), info, 1));
        bytes.add(Bytecode.IDENTICAL);
        if (!isType) {
            bytes.add(Bytecode.BOOL_NOT);
        }
        return bytes;
    }

    private BytecodeList convertMany() {
        if (!isType) {
            throw CompilerException.format(
                    "'is not' only works with 2 or fewer operands (got %d)",
                    lineInfo, operands.length
            );
        }
        var constBytes = getConstant();
        if (constBytes != null) return constBytes;
        // Since object identity is transitive, it's much easier if we
        // simply compare everything to the first object given.
        // Since nothing in life is ever simple, we have to do some
        // shenanigans with the stack to ensure everything winds
        // up in the right place.
        var bytes = new BytecodeList();
        bytes.add(Bytecode.LOAD_CONST, info.constIndex(Builtins.TRUE));
        bytes.addAll(TestConverter.bytes(operands[0].getArgument(), info, 1));
        for (int i = 1; i < operands.length - 1; i++) {
            bytes.add(Bytecode.DUP_TOP);
            bytes.addAll(TestConverter.bytes(operands[i].getArgument(), info, 1));
            bytes.add(Bytecode.IDENTICAL);  // Compare the values
            bytes.add(Bytecode.SWAP_3);     // Bring up the next one (below result & operands[0])
            bytes.add(Bytecode.BOOL_AND);   // 'and' them together
            bytes.add(Bytecode.SWAP_2);     // Put operands[0] back on top}
        }
        // Last one is special b/c cleanup...
        // No need to duplicate operands[0], and thus no need to swap to get
        // around it
        bytes.addAll(TestConverter.bytes(operands[operands.length - 1].getArgument(), info, 1));
        bytes.add(Bytecode.IDENTICAL);
        bytes.add(Bytecode.BOOL_AND);
        return bytes;
    }

    @Nullable
    private BytecodeList getConstant() {
        var constant = constantReturn();
        if (constant.isPresent()) {
            return loadConstant(info, constant.orElseThrow());
        }
        return null;
    }

    @Override
    @NotNull
    protected Pair<BytecodeList, TypeObject> convertWithAs() {
        if (operands.length != 2) {
            throw CompilerException.format(
                    "'is' comparison with 'as' clause may only have 2 parameters, not %d%n" +
                            "(only statements of the form 'x is not null' are allowed)",
                    lineInfo, operands.length
            );
        }
        if (isType) {
            throw asException(lineInfo);
        }
        var arg0 = operands[0].getArgument();
        var arg1 = operands[1].getArgument();
        if (!(arg1 instanceof VariableNode) || !((VariableNode) arg1).getName().equals("null")) {
            throw CompilerException.of(
                    "Cannot use 'as' here, 'is not' comparison must be done to null",
                    arg1
            );
        }
        var converter = TestConverter.of(info, arg0, 1);
        var condType = converter.returnType()[0];
        if (!(condType instanceof OptionTypeObject)) {
            CompilerWarning.warn(
                    "Using 'is not null' comparison on non-nullable variable", WarningType.TRIVIAL_VALUE, info, arg0
            );
            var bytes = new BytecodeList(converter.convert());
            bytes.add(Bytecode.LOAD_CONST, info.constIndex(Builtins.TRUE));
            return Pair.of(bytes, condType);
        } else if (condType.equals(Builtins.nullType())) {
            CompilerWarning.warn(
                    "Using 'is not null' comparison on variable that must be null",
                    WarningType.TRIVIAL_VALUE, info, arg0
            );
            var bytes = new BytecodeList(converter.convert());
            bytes.add(Bytecode.LOAD_CONST, info.constIndex(Builtins.FALSE));
            return Pair.of(bytes, condType);
        } else {
            var asType = condType.stripNull();
            var bytes = new BytecodeList(converter.convert());
            bytes.add(Bytecode.DUP_TOP);
            bytes.addAll(TestConverter.bytes(arg1, info, 1));
            bytes.add(Bytecode.IDENTICAL);
            bytes.add(Bytecode.BOOL_NOT);
            return Pair.of(bytes, asType);
        }
    }
}
