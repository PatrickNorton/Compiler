package main.java.converter;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class WarningHolder {
    private final List<Set<WarningType>> allowed;
    private final List<Set<WarningType>> denied;
    private final List<Set<WarningType>> forbidden;
    private final ErrorCounter counter;

    public enum Level {
        ALLOW,
        WARN,
        DENY,
    }

    public WarningHolder(ErrorCounter counter) {
        this.counter = counter;
        this.allowed = new ArrayList<>();
        this.denied = new ArrayList<>();
        this.forbidden = new ArrayList<>();
    }

    public Level warningLevel(WarningType type) {
        assert allowed.size() == denied.size();
        if (allowed.isEmpty()) {
            return Level.WARN;
        }
        for (int i = allowed.size() - 1; i >= 0; i--) {
            if (allowed.get(i).contains(type)) {
                assert !denied.get(i).contains(type);
                return Level.ALLOW;
            } else if (denied.get(i).contains(type)) {
                return Level.DENY;
            }
        }
        return Level.WARN;
    }

    public void popWarnings() {
        allowed.remove(allowed.size() - 1);
        denied.remove(denied.size() - 1);
    }

    public void allow(WarningType... allowed) {
        this.allowed.add(EnumSet.of(allowed[0], allowed));
        this.denied.add(EnumSet.noneOf(WarningType.class));
        this.forbidden.add(EnumSet.noneOf(WarningType.class));
    }

    public void deny(WarningType... denied) {
        this.allowed.add(EnumSet.noneOf(WarningType.class));
        this.denied.add(EnumSet.of(denied[0], denied));
        this.forbidden.add(EnumSet.noneOf(WarningType.class));
    }

    public void forbid(WarningType... forbidden) {
        this.allowed.add(EnumSet.noneOf(WarningType.class));
        this.denied.add(EnumSet.of(forbidden[0], forbidden));
        this.forbidden.add(EnumSet.of(forbidden[0], forbidden));
    }

    public void allowAll() {
        allowed.add(EnumSet.allOf(WarningType.class));
        denied.add(EnumSet.noneOf(WarningType.class));
        forbidden.add(EnumSet.noneOf(WarningType.class));
    }

    public void denyAll() {
        allowed.add(EnumSet.noneOf(WarningType.class));
        denied.add(EnumSet.allOf(WarningType.class));
        forbidden.add(EnumSet.noneOf(WarningType.class));
    }

    public void forbidAll() {
        allowed.add(EnumSet.noneOf(WarningType.class));
        denied.add(EnumSet.allOf(WarningType.class));
        forbidden.add(EnumSet.allOf(WarningType.class));
    }

    public boolean isForbidden(WarningType warning) {
        for (var set : forbidden) {
            if (set.contains(warning)) {
                return true;
            }
        }
        return false;
    }

    public void addWarning() {
        counter.addWarning();
    }

    public void addError() {
        counter.addError();
    }
}
