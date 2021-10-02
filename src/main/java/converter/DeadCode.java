package main.java.converter;

import main.java.converter.bytecode.LocationBytecode;
import org.jetbrains.annotations.NotNull;

/**
 * The class containing functions for eliminating dead code.
 * <p>
 *     This contains the method used when {@code -fdce} is passed as a
 *     parameter.
 * </p>
 *
 * @author Patrick Norton
 */
public final class DeadCode {
    private DeadCode() {}

    /**
     * The function that implements {@code -fdce}.
     * <p>
     *     This currently does two passes of optimization: The first eliminates
     *     all jumps to the next instruction; the second eliminates all code
     *     between any unconditional jump instruction and the next label.
     * </p>
     *
     * @param info The global information to operate on
     */
    public static void eliminate(@NotNull GlobalCompilerInfo info) {
        for (var function : info.getFunctions()) {
            var bytes = function.getBytes();
            eliminateJumps(bytes);
            eliminatePostJump(bytes);
        }
    }

    /**
     * Eliminates all jumps that jump to the next instruction.
     * <p>
     *     Any code of the form
     *     <pre><code>
     *     JUMP*    label
     * label:
     *     more_code
     *     </code></pre>
     *     is unnecessary, as no matter what the condition is, code execution
     *     will flow to the next statement.
     * </p>
     *
     * @param bytes The byte list to optimize
     */
    private static void eliminateJumps(@NotNull BytecodeList bytes) {
        boolean changed;
        do {
            var byteIndex = 0;
            bytes.setLabels();
            changed = false;
            for (var pair : bytes.enumerate()) {
                var index = pair.getKey();
                var bytecode = pair.getValue();
                byteIndex += bytecode.size();
                var operands = bytes.getOperands(index);
                if (operands.length > 0 && operands[0] instanceof LocationBytecode l) {
                    assert l.getLabel().getValue() != -1;
                    if (l.getLabel().getValue() == byteIndex) {
                        bytes.remove(index);
                        changed = true;
                    }
                }
            }
        } while (changed);
    }

    /**
     * Eliminates all code after a unconditional jump statement.
     * <p>
     *     Any code of the form
     *     <pre><code>
     *     JUMP     label1
     *     (some code goes here)
     * label2:
     *     </code></pre>
     *     can have the code eliminated. This is because all jump statements
     *     must point to a label. If there is no label after a jump, it
     *     therefore is impossible for any code to reach it. It is not
     *     necessary for the jump statement to point to the next label reached.
     *     As such, the above code snippet can be optimized to
     *     <pre><code>
     *     JUMP     label1
     * label2:
     *     </code></pre>
     * </p>
     *
     * @param bytes The byte list to optimize
     */
    private static void eliminatePostJump(@NotNull BytecodeList bytes) {
        for (var pair : bytes.enumerate()) {
            var index = pair.getKey();
            var bytecode = pair.getValue();
            if (bytecode == Bytecode.JUMP || bytecode == Bytecode.RETURN) {
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
