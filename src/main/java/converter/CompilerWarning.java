package main.java.converter;

import main.java.parser.LineInfo;
import main.java.parser.Lined;
import org.jetbrains.annotations.NotNull;

/**
 * Class containing methods for warnings.
 * <p>
 *     This is a static class, with no way to construct it.
 * </p>
 * <p>
 *     The methods of this class will print a warning to stderr, formatted in
 *     a way that makes warnings consistent and understandable to the end user.
 *     All compiler warnings should use this class, no other printing should
 *     occur outside of debugging messages (e.g. printing bytecode output) or
 *     the printing that occurs with exceptions.
 * </p>
 *
 * @author Patrick Norton
 */
public final class CompilerWarning {
    private CompilerWarning() {}

    private static void warnIf(String message, WarningType warn, CompilerInfo compilerInfo, LineInfo info) {
        var level = compilerInfo.warningHolder().warningLevel(warn);
        switch (level) {
            case ALLOW:
                return;
            case WARN:
                System.err.printf("Warning - file %s, line %d: %s%n%s%n",
                    info.getPath(), info.getLineNumber(), message, info.infoString());
                return;
            case DENY:
                throw CompilerException.of(message, info);
            default:
                throw CompilerInternalError.format("Unknown warning level %s", info, level);
        }
    }

    /**
     * Emits a compiler warning with a custom message and the relevant
     * information for where the warning occurred.
     *
     * @param message The message to warn
     * @param info The {@link LineInfo} for where the warning occurred
     */
    public static void warn(String message, @NotNull LineInfo info) {
        System.err.printf("Warning - file %s, line %d: %s%n%s%n",
                info.getPath(), info.getLineNumber(), message, info.infoString());
    }

    /**
     * Emits a compiler warning with a custom message, taking a {@link Lined}
     * object containing the {@link LineInfo} for where the warning occurred.
     *
     * @implNote Equivalent to {@code warn(message, node.getLineInfo())}
     * @param message The message to warn
     * @param node The {@link Lined} object to get the location from
     */
    public static void warn(String message, @NotNull Lined node) {
        warn(message, node.getLineInfo());
    }

    /**
     * Emits a compiler warning with a formatted message and the relevant
     * information for where the warning occurred.
     *
     * @param message The message to be formatted
     * @param warn The type of warning
     * @param info The value to get the {@link WarningHolder} for
     * @param lineInfo The {@link LineInfo} of where the warning occurred
     * @param args The args to format with
     */
    public static void warnf(
            String message, WarningType warn, CompilerInfo info, @NotNull LineInfo lineInfo, Object... args
    ) {
        warnIf(String.format(message, args), warn, info, lineInfo);
    }

    /**
     * Emits a compiler warning with a formatted message and the relevant
     * information for where the warning occurred.
     *
     * @param message The message to be formatted
     * @param warn The type of warning
     * @param info The value to get the {@link WarningHolder} for
     * @param node The {@link Lined} object to get the location from
     * @param args The args to format with
     */
    public static void warnf(String message, WarningType warn, CompilerInfo info, Lined node, Object... args) {
        warnIf(String.format(message, args), warn, info, node.getLineInfo());
    }
}
