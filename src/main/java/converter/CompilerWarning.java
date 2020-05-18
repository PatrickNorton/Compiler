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
     * @param info The {@link LineInfo} of where the warning occurred
     * @param args The args to format with
     */
    public static void warnf(String message, @NotNull LineInfo info, Object... args) {
        warn(String.format(message, args), info);
    }

    /**
     * Emits a compiler warning with a formatted message and the relevant
     * information for where the warning occurred.
     *
     * @param message The message to be formatted
     * @param node The {@link Lined} object to get the location from
     * @param args The args to format with
     */
    public static void warnf(String message, @NotNull Lined node, Object... args) {
        warn(String.format(message, args), node.getLineInfo());
    }
}
