package main.java.converter;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class DeadCode {
    private DeadCode() {}

    public static void eliminate(@NotNull GlobalCompilerInfo info) {
        for (var function : info.getFunctions()) {
            var bytes = function.getBytes();
            eliminateJumps(bytes);
            eliminatePostJump(bytes);
        }
    }

    private static void eliminateJumps(@NotNull BytecodeList bytes) {
        var byteIndex = 0;
        for (var pair : bytes.enumerate()) {
            var index = pair.getKey();
            var bytecode = pair.getValue();
            byteIndex += bytecode.size();
            if (isJump(bytecode) && bytes.getOperands(index)[0] == byteIndex) {
                bytes.remove(index);
            }
        }
    }

    @Contract(pure = true)
    private static boolean isJump(@NotNull Bytecode bytecode) {
        return switch (bytecode) {
            case JUMP, JUMP_FALSE, JUMP_TRUE, JUMP_NN, JUMP_NULL -> true;
            default -> false;
        };
    }

    private static void eliminatePostJump(@NotNull BytecodeList bytes) {
        for (var pair : bytes.enumerate()) {
            var index = pair.getKey();
            var bytecode = pair.getValue();
            if (bytecode == Bytecode.JUMP) {
                var nextLabel = bytes.nextLabel(index);
                if (nextLabel == null) {
                    bytes.removeRange(index.next());
                } else {
                    bytes.removeRange(index.next(), nextLabel);
                }
            }
        }
    }
}
