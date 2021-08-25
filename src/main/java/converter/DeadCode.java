package main.java.converter;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class DeadCode {
    private DeadCode() {}

    public static void eliminateJumps(@NotNull GlobalCompilerInfo info) {
        for (var function : info.getFunctions()) {
            var bytes = function.getBytes();
            eliminateJumps(bytes);
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
        switch (bytecode) {
            case JUMP:
            case JUMP_FALSE:
            case JUMP_TRUE:
            case JUMP_NN:
            case JUMP_NULL:
                return true;
            default:
                return false;
        }
    }
}
