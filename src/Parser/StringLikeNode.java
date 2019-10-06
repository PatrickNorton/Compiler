package Parser;

import java.util.EnumSet;
import java.util.regex.Pattern;

/**
 * The abstract class representing a StringNode or a FormattedStringNode.
 *
 * @author Patrick Norton
 */
public abstract class StringLikeNode implements AtomicNode {
    public abstract EnumSet<StringPrefix> getPrefixes();
    static final Pattern prefixPattern = Pattern.compile("^[refb]*");
    static final Pattern contentPattern = Pattern.compile("(^[refb]*\")|(\"$)");
}
