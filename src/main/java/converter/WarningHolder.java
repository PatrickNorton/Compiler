package main.java.converter;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class WarningHolder {
    private final List<Set<WarningType>> allowed;
    private final List<Set<WarningType>> denied;

    public enum Level {
        ALLOW,
        WARN,
        DENY,
    }

    public WarningHolder() {
        this.allowed = new ArrayList<>();
        this.denied = new ArrayList<>();
    }

    public Level warningLevel(WarningType type) {
        if (last(allowed).contains(type)) {
            assert !last(denied).contains(type);
            return Level.ALLOW;
        } else if (last(denied).contains(type)) {
            return Level.DENY;
        } else {
            return Level.WARN;
        }
    }

    public void popWarnings() {
        allowed.remove(allowed.size() - 1);
        denied.remove(denied.size() - 1);
    }

    public void allow(WarningType... allowed) {
        this.allowed.add(EnumSet.of(allowed[0], allowed));
        this.denied.add(EnumSet.noneOf(WarningType.class));
    }

    public void deny(WarningType... denied) {
        this.allowed.add(EnumSet.noneOf(WarningType.class));
        this.denied.add(EnumSet.of(denied[0], denied));
    }

    public void allowAll() {
        allowed.add(EnumSet.allOf(WarningType.class));
        denied.add(EnumSet.noneOf(WarningType.class));
    }

    public void denyAll() {
        allowed.add(EnumSet.noneOf(WarningType.class));
        denied.add(EnumSet.allOf(WarningType.class));
    }

    private static <T> T last(List<T> values) {
        return values.get(values.size() - 1);
    }
}
