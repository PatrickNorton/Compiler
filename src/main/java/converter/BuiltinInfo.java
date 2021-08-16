package main.java.converter;

/**
 * Information derived from the {@code $builtin} annotation.
 *
 * @author Patrick Norton
 */
public final class BuiltinInfo {
    private final String name;
    private final int index;
    private final boolean hidden;

    public BuiltinInfo(String name, int index, boolean hidden) {
        this.name = name;
        this.index = index;
        this.hidden = hidden;
    }

    public BuiltinInfo(String name, int index) {
        this(name, index, false);
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public boolean isHidden() {
        return hidden;
    }
}
