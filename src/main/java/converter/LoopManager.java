package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LoopManager {
    private final Deque<Boolean> loopLevel = new ArrayDeque<>();
    private final Map<Integer, Set<Integer>> breakPointers = new HashMap<>();
    private final Map<Integer, Set<Integer>> continuePointers = new HashMap<>();
    private final Deque<Integer> continueLocations = new ArrayDeque<>();

    /**
     * Enter another loop, implying another level of break/continue statements.
     */
    public void enterLoop(boolean hasContinue) {
        loopLevel.push(hasContinue);
        continueLocations.push(-1);
        assert continueLocations.size() == loopLevel.size();
    }

    /**
     * Exit a loop, and set all dangling pointers to the end of the loop.
     *
     * @param listStart The index of the beginning of the list relative to the
     *                  start of the function
     * @param bytes The list of bytes
     */
    public void exitLoop(int listStart, @NotNull List<Byte> bytes) {
        int level = loopLevel.size();
        boolean hasContinue = loopLevel.pop();
        int endLoop = listStart + bytes.size();
        for (int i : breakPointers.getOrDefault(level, Collections.emptySet())) {
            Util.emplace(bytes, Util.intToBytes(endLoop), i - listStart);
        }
        breakPointers.remove(level);
        int continueLocation = continueLocations.pop();
        if (hasContinue) {
            assert continueLocation != -1 : "Continue location not defined";
            for (int i : continuePointers.getOrDefault(level, Collections.emptySet())) {
                Util.emplace(bytes, Util.intToBytes(continueLocation), i - listStart);
            }
            continuePointers.remove(level);
        } else {
            assert continueLocation == -1 : "Continue location erroneously set";
        }
    }

    /**
     * Add a break statement to the pool of un-linked statements.
     *
     * @param levels The number of levels to break
     * @param location The location (absolute, by start of function)
     */
    public void addBreak(int levels, int location) {
        var pointerSet = breakPointers.computeIfAbsent(loopLevel.size() - levels + 1, k -> new HashSet<>());
        pointerSet.add(location);
    }

    /**
     * Add a continue statement's pointer to the list.
     *
     * @param location The location (absolute, by start of function)
     */
    public void addContinue(int location) {
        var pointerSet = continuePointers.computeIfAbsent(loopLevel.size(), k -> new HashSet<>());
        pointerSet.add(location);
    }

    /**
     * Set the point where a {@code continue} statement should jump to.
     *
     * @param location The location (absolute, by start of function)
     */
    public void setContinuePoint(int location) {
        assert continueLocations.size() == loopLevel.size() : "Mismatch in continue levels";
        int oldValue = continueLocations.pop();
        assert oldValue == -1 : "Defined multiple continue points for loop";
        continueLocations.push(location);
    }
}
