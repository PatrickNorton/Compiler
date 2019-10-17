package Parser;

import org.jetbrains.annotations.Contract;

import java.util.EnumSet;
import java.util.regex.Pattern;

/**
 * The abstract class representing a StringNode or a FormattedStringNode.
 *
 * @author Patrick Norton
 */
public abstract class StringLikeNode implements AtomicNode {
    private LineInfo lineInfo;

    @Contract(pure = true)
    public StringLikeNode(LineInfo lineInfo) {
        this.lineInfo = lineInfo;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public abstract EnumSet<StringPrefix> getPrefixes();
    static final Pattern prefixPattern = Pattern.compile("^[refb]*");
    static final Pattern contentPattern = Pattern.compile("(^[refb]*\")|(\"$)");
}
