package main.java.converter;

import main.java.parser.LineInfo;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class FunctionInliner {
    private static final int INLINE_THRESHOLD = 32;

    public static void inline(BytecodeList.Index index, @NotNull BytecodeList first, @NotNull BytecodeList toInline) {
        // FIXME: Remove return bytecodes from byte list
        first.remove(index);
        first.addAll(index, toInline.copyLabels());
    }

    public static void inlineAll(CompilerInfo info, @NotNull BytecodeList value) {
        CompilerWarning.warn(
                "Inlining is a work in progress and may not produce executable bytecodes",
                WarningType.TODO, info, LineInfo.empty()
        );
        for (var pair : value.enumerate()) {
            var index = pair.getKey();
            var bytecode = pair.getValue();
            if (bytecode == Bytecode.CALL_FN) {
                var operands = value.getOperands(index);
                assert operands.length == 2;
                var functionNo = operands[0];
                var function = info.getFunctions().get(functionNo);
                if (shouldInline(functionNo, function)) {
                    inline(index, value, function.getBytes());
                }
            }
        }
    }

    private static boolean shouldInline(int fnIndex, @NotNull Function fn) {
        // FIXME: Deeper-than-single mutual recursion will hang
        var bytes = fn.getBytes();
        var bytecodeCount = bytes.bytecodeCount();
        if (hasSwitchTable(bytes)) {
            // Temporary hack b/c switch tables need to be copied
            return false;
        }
        return bytecodeCount < INLINE_THRESHOLD && !callsFunction(fnIndex, bytes);
    }

    private static boolean callsFunction(int fnIndex, @NotNull BytecodeList value) {
        for (var pair : value.enumerate()) {
            var index = pair.getKey();
            var bytecode = pair.getValue();
            if (bytecode == Bytecode.CALL_FN) {
                var operands = value.getOperands(index);
                assert operands.length == 2;
                var functionNo = operands[0];
                if (functionNo == fnIndex) {
                    return true;
                }
            }
        }
        return false;
    }

    @Contract(pure = true)
    private static boolean hasSwitchTable(@NotNull BytecodeList value) {
        for (var bytecode : value.bytecodes()) {
            if (bytecode == Bytecode.SWITCH_TABLE) {
                return true;
            }
        }
        return false;
    }
}
