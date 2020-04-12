package main.java.util;

/**
 * An interface designed to hold counts for a number of items.
 *
 * @param <T> The type of the item to be counted
 */
public interface Counter<T> extends Iterable<T> {
    /**
     * Whether or not the counter contains the specified object.
     *
     * @param key The key to check
     * @return If the value is contained in the map and > 0
     */
    boolean contains(T key);

    /**
     * The count of the stored item.
     *
     * @param key The key to count
     * @return The count of that key
     */
    int count(T key);

    /**
     * Increase the count of the stored item by one.
     *
     * @param key The key to increase
     */
    void increment(T key);

    /**
     * Decrease the count of the stored item by one.
     *
     * @param key The key to decrease
     */
    void decrement(T key);
}
