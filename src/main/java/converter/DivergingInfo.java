package main.java.converter;

import java.util.HashSet;
import java.util.Set;

/**
 * The class that encapsulates control-flow information.
 * <p>
 *     The current information being tracked is {@code return}, {@code break},
 *     and {@code continue} statements. {@code yield} and {@code throw} are not
 *     tracked.
 * </p>
 * <p>
 *     The information within this class comes in three states: "will", "may",
 *     and "will not". As an example, if {@link #willReturn()} is {@code true},
 *     then all possible code paths will result in a {@code return} statement
 *     being executed. The exception to this is if an error is thrown. If
 *     {@link #mayReturn()} is {@code true}, then there exist possible code
 *     paths which will result in a {@code return} statement being executed.
 *     If neither are {@code true}, then there are no normal code paths that
 *     result in {@code return} happening.
 * </p>
 * <p>
 *     Examples:
 *     {@link #willReturn()} {@code == true}
 *     <pre><code>
 * if x() {
 *     return y
 * } else {
 *     return z
 * }
 *     </code></pre>
 *     {@link #mayReturn()} {@code == true}
 *     <pre><code>
 * if x() {
 *     return y
 * } else {}
 *     </code></pre>
 *     {@link #mayReturn()} {@code == false}
 *     <pre><code>
 * if x() {
 *     bar()
 * } else {
 *     baz()
 * }
 *     </code></pre>
 * </p>
 *
 * @author Patrick Norton
 */
public final class DivergingInfo {
    private boolean willReturn;
    private boolean mayReturn;
    private final Set<Integer> willBreak;
    private final Set<Integer> mayBreak;
    private boolean willContinue;
    private boolean mayContinue;

    public DivergingInfo() {
        willReturn = false;
        mayReturn = false;
        willBreak = new HashSet<>();
        mayBreak = new HashSet<>();
        willContinue = false;
        mayContinue = false;
    }

    private DivergingInfo(
            boolean willReturn, boolean mayReturn, Set<Integer> willBreak,
            Set<Integer> mayBreak, boolean willContinue, boolean mayContinue
    ) {
        this.willReturn = willReturn;
        this.mayReturn = mayReturn;
        this.willBreak = willBreak;
        this.mayBreak = mayBreak;
        this.willContinue = willContinue;
        this.mayContinue = mayContinue;
    }

    /**
     * Sets this to the logical 'and' of this {@link DivergingInfo} and
     * another.
     * <p>
     *     This is useful for branched statements, where control flow
     *     operations are only guaranteed to happen if all branches have it.
     *     One example of this is {@code if}-statements.
     * </p>
     * @param other The other information to "and" this with
     */
    public void andWith(DivergingInfo other) {
        this.willReturn &= other.willReturn;
        this.mayReturn |= other.mayReturn;
        this.willBreak.retainAll(other.willBreak);
        this.mayBreak.addAll(other.mayBreak);
        this.willContinue &= other.willContinue;
        this.mayContinue |= other.mayContinue;
    }

    /**
     * Sets this to the logical 'or' of this {@link DivergingInfo} and
     * another.
     * <p>
     *     This is useful for consecutive statements, where any one that is
     *     guaranteed to return means all following it will not execute. One
     *     example of this is the statement in a block.
     * </p>
     * @param other The other information to "and" this with
     */
    public void orWith(DivergingInfo other) {
        this.willReturn |= other.willReturn;
        this.mayReturn |= other.mayReturn;
        this.willBreak.addAll(other.willBreak);
        this.mayBreak.addAll(other.mayBreak);
        this.willContinue |= other.willContinue;
        this.mayContinue |= other.mayContinue;
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

    public void knownContinue() {
        this.willContinue = true;
        this.mayContinue = true;
    }

    public void possibleContinue() {
        this.mayContinue = true;
    }

    public void makeUncertain() {
        this.willReturn = false;
        this.willBreak.clear();
        this.willContinue = false;
    }

    public DivergingInfo removeLevel() {
        return new DivergingInfo(willReturn, mayReturn, lessOne(willBreak), lessOne(mayBreak), false, false);
    }

    public boolean willReturn() {
        return willReturn;
    }

    public boolean mayReturn() {
        return mayReturn;
    }

    public boolean willDiverge() {
        return willReturn || willContinue || !willBreak.isEmpty();
    }

    public boolean mayDiverge() {
        return mayReturn || mayContinue || !mayBreak.isEmpty();
    }

    public boolean willBreak() {
        return !willBreak.isEmpty();
    }

    public boolean mayBreak() {
        return !mayBreak.isEmpty();
    }

    public boolean willContinue() {
        return willContinue;
    }

    public boolean mayContinue() {
        return mayContinue;
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
