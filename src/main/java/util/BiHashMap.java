package main.java.util;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Set;

public final class BiHashMap<K, V> extends AbstractMap<K, V> implements BidirectionalMap<K, V> {
    private final HashMap<K, V> normal;
    private final HashMap<V, K> reversed;

    BiHashMap(HashMap<K, V> normal, HashMap<V, K> reversed) {
        assert normal.size() == reversed.size();
        this.normal = normal;
        this.reversed = reversed;
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return normal.entrySet();
    }

    @Override
    public BidirectionalMap<V, K> inverse() {
        return new BiHashMap<>(reversed, normal);
    }

    @Override
    public boolean isEmpty() {
        return normal.isEmpty();
    }

    @Override
    public void clear() {
        normal.clear();
        reversed.clear();
    }

    @Override
    public V get(Object key) {
        return normal.get(key);
    }

    @Override
    public boolean containsKey(Object key) {
        return normal.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        //noinspection SuspiciousMethodCalls
        return reversed.containsKey(value);
    }

    @Override
    public V put(K key, V value) {
        if (normal.containsKey(key)) {
            var old = normal.get(key);
            assert reversed.containsKey(old);
            if (old != value) {
                throw new UnsupportedOperationException("Mismatched keys");
            } else {
                normal.put(key, value);
                reversed.put(value, key);
                return old;
            }
        } else {
            if (reversed.containsKey(value)) {
                throw new UnsupportedOperationException("Mismatched keys");
            } else {
                normal.put(key, value);
                reversed.put(value, key);
                return null;
            }
        }
    }
}
