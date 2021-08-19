package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * The class for managing loop-like statements and their {@code break} and
 * {@code continue} statements.
 *
 * @author Patrick Norton
 */
public final class LoopManager {
    private final List<LoopEntry> entries = new ArrayList<>();

    /**
     * Enter another loop, implying another level of break/continue statements.
     */
    public void enterLoop(@NotNull CompilerInfo info, boolean hasContinue) {
        var breakLabel = info.newJumpLabel();
        var continueLabel = hasContinue ? info.newJumpLabel() : -1;
        entries.add(new LoopEntry(hasContinue, breakLabel, continueLabel));
    }

    /**
     * Exit a loop, and set all dangling pointers to the end of the loop.
     *
     * @param bytes The list of bytes
     */
    public void exitLoop(@NotNull BytecodeList bytes) {
        var entry = entries.remove(entries.size() - 1);
        bytes.addLabel(entry.getBreakLabel());
    }

    /**
     * The label where a `continue` statement should jump to.
     *
     * <p>
     *     Care should be taken that this never be added to the bytecode list.
     * </p>
     */
    public int breakLabel(int levels) {
        return entries.get(entries.size() - levels + 1).getBreakLabel();
    }

    /**
     * The label where a `continue` statement should jump to.
     *
     * <p>
     *     Care should be taken that this only be added to the bytecode list
     *     *once* per loop.
     * </p>
     */
    public int continueLabel() {
        for (int i = entries.size() - 1; i >= 0; i--) {
            var entry = entries.get(i);
            if (entry.hasContinue()) {
                return entry.getContinueLabel();
            }
        }
        return -1;
    }

    private static final class LoopEntry {
        private final boolean hasContinue;
        private final int breakLabel;
        private final int continueLabel;

        public LoopEntry(boolean hasContinue, int breakLabel, int continueLabel) {
            this.hasContinue = hasContinue;
            this.breakLabel = breakLabel;
            this.continueLabel = continueLabel;
        }

        public boolean hasContinue() {
            return hasContinue;
        }

        public int getBreakLabel() {
            return breakLabel;
        }

        public int getContinueLabel() {
            return continueLabel;
        }
    }
}
