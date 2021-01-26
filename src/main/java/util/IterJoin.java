package main.java.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public final class IterJoin<T> implements Iterator<T> {
    private final List<Iterator<T>> values;
    private int i = 0;

    private IterJoin(List<Iterator<T>> values) {
        this.values = values;
    }

    @Override
    public boolean hasNext() {
        while (i < values.size() && !values.get(i).hasNext()) {
            i++;
        }
        return i < values.size();
    }

    @Override
    public T next() {
        while (i < values.size() && !values.get(i).hasNext()) {
            i++;
        }
        if (i < values.size()) {
            return values.get(i).next();
        } else {
            throw new NoSuchElementException();
        }
    }

    @SafeVarargs
    public static <T> IterJoin<T> from(Iterable<T>... values) {
        List<Iterator<T>> result = new ArrayList<>(values.length);
        for (var value : values) {
            result.add(value.iterator());
        }
        return new IterJoin<>(result);
    }
}
