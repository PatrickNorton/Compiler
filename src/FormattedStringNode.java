import org.jetbrains.annotations.Contract;

/**
 * The class representing a formatted string.
 * <p>
 *     This is separate intentionally from the normal {@link StringNode},
 *     because it has a list of tests which need to be performed in order to be
 *     turned into a proper string. There is no parse method for this (yet?), as
 *     all parsing is done instead by the normal {@link StringNode#parse}.
 * </p>
 * @author Patrick Norton
 * @see StringNode
 */
public class FormattedStringNode implements AtomicNode {
    private String[] strs;
    private TestNode[] tests;

    /**
     * Construct a new FormattedStringNode.
     * @param strs The intermittent string literals
     * @param tests The non-string-literals which are interpolated
     */
    @Contract(pure = true)
    public FormattedStringNode(String[] strs, TestNode[] tests) {  // FIXME? Add string flags
        this.strs = strs;
        this.tests = tests;
    }

    public String[] getStrs() {
        return strs;
    }

    public TestNode[] getTests() {
        return tests;
    }
}
