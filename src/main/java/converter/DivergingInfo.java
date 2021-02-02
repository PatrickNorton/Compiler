package main.java.converter;

import java.util.HashSet;
import java.util.Set;

public final class DivergingInfo {
    private boolean willReturn;
    private boolean mayReturn;
    private final Set<Integer> willBreak;
    private final Set<Integer> mayBreak;

    public DivergingInfo() {
        willReturn = false;
        mayReturn = false;
        willBreak = new HashSet<>();
        mayBreak = new HashSet<>();
    }

    private DivergingInfo(boolean willReturn, boolean mayReturn, Set<Integer> willBreak, Set<Integer> mayBreak) {
        this.willReturn = willReturn;
        this.mayReturn = mayReturn;
        this.willBreak = willBreak;
        this.mayBreak = mayBreak;
    }

    public void andWith(DivergingInfo other) {
        this.willReturn &= other.willReturn;
        this.mayReturn &= other.mayReturn;
        this.willBreak.retainAll(other.willBreak);
        this.mayBreak.retainAll(other.mayBreak);
    }

    public void orWith(DivergingInfo other) {
        this.willReturn |= other.willReturn;
        this.mayReturn |= other.mayReturn;
        this.willBreak.addAll(other.willBreak);
        this.mayBreak.addAll(other.mayBreak);
    }

    public DivergingInfo knownReturn() {
        this.willReturn = true;
        this.mayReturn = true;
        return this;
    }

    public DivergingInfo possibleReturn() {
        this.mayReturn = true;
        return this;
    }

    public void knownBreak(int level) {
        this.willBreak.add(level);
        this.mayBreak.add(level);
    }

    public void possibleBreak(int level) {
        this.mayBreak.add(level);
    }

    public void makeUncertain() {
        this.willReturn = false;
        this.willBreak.clear();
    }

    public DivergingInfo removeLevel() {
        return new DivergingInfo(willReturn, mayReturn, lessOne(willBreak), lessOne(mayBreak));
    }

    public boolean willReturn() {
        return willReturn;
    }

    public boolean mayReturn() {
        return mayReturn;
    }

    public boolean willDiverge() {
        return willReturn || !willBreak.isEmpty();
    }

    public boolean mayDiverge() {
        return mayReturn || !mayBreak.isEmpty();
    }

    public boolean willBreak() {
        return !willBreak.isEmpty();
    }

    public boolean mayBreak() {
        return !mayBreak.isEmpty();
    }

    private static Set<Integer> lessOne(Set<Integer> values) {
        Set<Integer> result = new HashSet<>(values.size());
        for (var value : values) {
            if (value > 1) {
                result.add(value - 1);
            }
        }
        return result;
    }
}
