package main.java.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * An implementation of {@link Counter} using a {@link HashMap} as its base.
 *
 * @param <T> The type to be counted, as specified in {@link Counter}
 */
public class HashCounter<T> implements Counter<T> {
    private final Map<T, Integer> map;

    public HashCounter() {
        this.map = new HashMap<>();
    }

    /**
     * {@inheritDoc}
     *
     * @param key The key to check
     * @return If the value is contained in the map and > 0
     */
    @Override
    public boolean contains(T key) {
        return map.containsKey(key);
    }

    /**
     * {@inheritDoc}
     *
     * @param key The key to count
     * @return The count of that key
     */
    @Override
    public int count(T key) {
        return map.get(key);
    }

    /**
     * {@inheritDoc}
     *
     * @param key The key to increase
     */
    @Override
    public void increment(T key) {
        map.put(key, map.getOrDefault(key, 0) + 1);
    }

    /**
     * {@inheritDoc}
     *
     * @param key The key to decrease
     */
    @Override
    public void decrement(T key) {
        map.put(key, map.get(key) - 1);
        if (map.get(key) == 0) {
            map.remove(key);
        }
    }

    /**
     * Returns an iterator over all keys of the counter
     *
     * @return The iterator over all keys
     */

    @Override
    public Iterator<T> iterator() {
        return map.keySet().iterator();
    }
}
