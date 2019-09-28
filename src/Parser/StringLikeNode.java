package Parser;

import java.util.EnumSet;
import java.util.regex.Pattern;

public abstract class StringLikeNode implements AtomicNode {
    public abstract EnumSet<StringPrefix> getPrefixes();
    static final Pattern prefixPattern = Pattern.compile("^[refb]*");
    static final Pattern contentPattern = Pattern.compile("(^[refb]*\")|(?<!\\\\)\"");
}
