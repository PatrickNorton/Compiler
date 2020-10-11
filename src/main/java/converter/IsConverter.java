package main.java.converter;

import main.java.parser.ArgumentNode;
import main.java.parser.Lined;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class IsConverter implements TestConverter {
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
        if (operands.length < 2) {
            return Optional.of(LangConstant.of(isType));
        } else {
            return Optional.empty();
        }
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
            case 1: {
                CompilerWarning.warnf("'%s' with < 2 operands will always be %b", lineInfo, isType ? "is" : "is not", isType);
                List<Byte> bytes = new ArrayList<>(Bytecode.LOAD_CONST.size());
                bytes.add(Bytecode.LOAD_CONST.value);
                bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(isType))));
                return bytes;
            }
            case 2: {
                List<Byte> bytes = new ArrayList<>(TestConverter.bytes(start, operands[0].getArgument(), info, 1));
                bytes.addAll(TestConverter.bytes(start, operands[1].getArgument(), info, 1));
                bytes.add(Bytecode.IDENTICAL.value);
                if (!isType) {
                    bytes.add(Bytecode.BOOL_NOT.value);
                }
                return bytes;
            }
            default: {
                if (!isType) {
                    throw CompilerException.format(
                            "'is not' only works with 2 or fewer operands (got %d)",
                            lineInfo, operands.length
                    );
                }
                // Since object identity is transitive, it's much easier if we
                // simply compare everything to the first object given.
                // Since nothing in life is ever simple, we have to do some
                // shenanigans with the stack to ensure everything winds
                // up in the right place.
                List<Byte> bytes = new ArrayList<>(TestConverter.bytes(start, operands[0].getArgument(), info, 1));
                for (int i = 1; i < operands.length; i++) {
                    if (i != operands.length - 1) {
                        bytes.add(Bytecode.DUP_TOP.value);
                    }
                    bytes.addAll(TestConverter.bytes(start, operands[i].getArgument(), info, 1));
                    bytes.add(Bytecode.IDENTICAL.value);  // Compare the values
                    bytes.add(Bytecode.SWAP_3.value);     // Bring up the next one (below result & operands[0])
                    bytes.add(Bytecode.BOOL_AND.value);   // 'and' them together
                    bytes.add(Bytecode.SWAP_2.value);     // Put operands[0] back on top
                }
                return bytes;
            }
        }
    }
}
