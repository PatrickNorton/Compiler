package main.java.converter;

import main.java.parser.ArgumentNode;
import main.java.parser.Lined;
import main.java.parser.OperatorTypeNode;
import main.java.parser.VariableNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
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
        return new TypeObject[] {Builtins.BOOL};
    }

    @Override
    @NotNull
    public List<Byte> convert(int start) {
        if (retCount > 1) {
            throw CompilerException.format("'is' only returns 1 value, %d expected", lineInfo, retCount);
        }
        switch (operands.length) {
            case 0:
            case 1:
                return convert0(start);
            case 2:
                return convert2(start);
            default:
                return convertMany(start);
        }
    }

    private List<Byte> convert0(int start) {
        assert operands.length == 0 || operands.length == 1;
        CompilerWarning.warnf("'%s' with < 2 operands will always be %b", lineInfo, isType ? "is" : "is not", isType);
        // Have to get side-effects
        List<Byte> bytes = new ArrayList<>(TestConverter.bytes(start, operands[0].getArgument(), info, 1));
        bytes.add(Bytecode.POP_TOP.value);
        bytes.add(Bytecode.LOAD_CONST.value);
        bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(isType))));
        return bytes;
    }

    private List<Byte> convert2(int start) {
        assert operands.length == 2;
        List<Byte> constBytes = getConstant();
        if (constBytes != null) return constBytes;
        List<Byte> bytes = new ArrayList<>(TestConverter.bytes(start, operands[0].getArgument(), info, 1));
        bytes.addAll(TestConverter.bytes(start, operands[1].getArgument(), info, 1));
        bytes.add(Bytecode.IDENTICAL.value);
        if (!isType) {
            bytes.add(Bytecode.BOOL_NOT.value);
        }
        return bytes;
    }

    private List<Byte> convertMany(int start) {
        if (!isType) {
            throw CompilerException.format(
                    "'is not' only works with 2 or fewer operands (got %d)",
                    lineInfo, operands.length
            );
        }
        List<Byte> constBytes = getConstant();
        if (constBytes != null) return constBytes;
        // Since object identity is transitive, it's much easier if we
        // simply compare everything to the first object given.
        // Since nothing in life is ever simple, we have to do some
        // shenanigans with the stack to ensure everything winds
        // up in the right place.
        List<Byte> bytes = new ArrayList<>();
        bytes.add(Bytecode.LOAD_CONST.value);
        bytes.addAll(Util.shortToBytes(info.constIndex(Builtins.TRUE)));
        bytes.addAll(TestConverter.bytes(start, operands[0].getArgument(), info, 1));
        for (int i = 1; i < operands.length - 1; i++) {
            bytes.add(Bytecode.DUP_TOP.value);
            bytes.addAll(TestConverter.bytes(start, operands[i].getArgument(), info, 1));
            bytes.add(Bytecode.IDENTICAL.value);  // Compare the values
            bytes.add(Bytecode.SWAP_3.value);     // Bring up the next one (below result & operands[0])
            bytes.add(Bytecode.BOOL_AND.value);   // 'and' them together
            bytes.add(Bytecode.SWAP_2.value);     // Put operands[0] back on top}
        }
        // Last one is special b/c cleanup...
        bytes.addAll(TestConverter.bytes(start, operands[operands.length - 1].getArgument(), info, 1));
        bytes.add(Bytecode.IDENTICAL.value);
        bytes.add(Bytecode.BOOL_AND.value);
        return bytes;
    }

    @Nullable
    private List<Byte> getConstant() {
        var constant = constantReturn();
        if (constant.isPresent()) {
            return loadConstant(info, constant.orElseThrow());
        }
        return null;
    }

    @Override
    @NotNull
    protected Pair<List<Byte>, TypeObject> convertWithAs(int start) {
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
        var condType = TestConverter.returnType(arg0, info, 1)[0];
        if (!(condType instanceof OptionTypeObject)) {
            CompilerWarning.warn("Using 'is not null' comparison on non-nullable variable", arg0);
        } else if (condType.equals(Builtins.NULL_TYPE)) {
            CompilerWarning.warn("Using 'is not null' comparison on variable that must be null", arg0);
        }
        var asType = condType.stripNull();
        var bytes = new ArrayList<>(TestConverter.bytes(start, arg0, info, 1));
        bytes.add(Bytecode.DUP_TOP.value);
        bytes.addAll(TestConverter.bytes(start + bytes.size(), arg1, info, 1));
        bytes.add(Bytecode.IDENTICAL.value);
        bytes.add(Bytecode.BOOL_NOT.value);
        return Pair.of(bytes, asType);
    }
}
