package main.java.util;

import java.util.HashMap;
import java.util.Map;

public interface BidirectionalMap<K, V> extends Map<K, V> {
    BidirectionalMap<V, K> inverse();

    @SafeVarargs
    static <K, V> BidirectionalMap<K, V> ofEntries(Map.Entry<K, V>... args) {
        var normal = new HashMap<K, V>(args.length);
        var reversed = new HashMap<V, K>(args.length);
        for (var entry : args) {
            var key = entry.getKey();
            var val = entry.getValue();
            if (normal.containsKey(key) || reversed.containsKey(val)) {
                throw new UnsupportedOperationException();
            } else {
                normal.put(key, val);
                reversed.put(val, key);
            }
        }
        return new BiHashMap<>(normal, reversed);
    }
}
