package Parser;

import java.util.regex.Pattern;

public abstract class StringLikeNode implements AtomicNode {
    static final Pattern prefixPattern = Pattern.compile("^[refb]*");
    static final Pattern contentPattern = Pattern.compile("(^[refb]*\")|(?<!\\\\)\"");
}
