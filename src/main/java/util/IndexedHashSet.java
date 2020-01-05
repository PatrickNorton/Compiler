package main.java.util;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class IndexedHashSet<E> extends AbstractSet<E> implements IndexedSet<E> {
    private Set<E> values;
    private List<E> valueList;

    public IndexedHashSet() {
        this.valueList = new ArrayList<>();
        this.values = new HashSet<>();
    }

    public IndexedHashSet(Collection<? extends E> values) {
        this.values = new HashSet<>(values);
        this.valueList = new ArrayList<>(values);
    }

    @NotNull
    public Iterator<E> iterator() {
        return new IHSIterator();
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public E get(int index) {
        return valueList.get(index);
    }

    @Override
    public void set(int index, E value) {
        valueList.set(index, value);
        if (index < size()) {
            values.remove(value);
        }
        values.add(value);
    }

    @Override
    public E remove(int index) {
        E val = valueList.remove(index);
        values.remove(val);
        return val;
    }

    @Override
    public boolean remove(Object o) {
        if (values.remove(o)) {
            valueList.remove(o);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean contains(Object o) {
        return values.contains(o);
    }

    @Override
    public boolean add(E e) {
        boolean modified = values.add(e);
        if (modified) valueList.add(e);
        return modified;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        boolean modified = false;
        for (var value : c) {
            if (contains(value)) {
                remove(value);
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public int indexOf(E object) {
        return valueList.indexOf(object);
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return valueList.toArray();
    }

    private class IHSIterator implements Iterator<E> {
        private Iterator<E> iterator = values.iterator();

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public E next() {
            return iterator.next();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        IndexedHashSet<?> that = (IndexedHashSet<?>) o;
        return Objects.equals(values, that.values) &&
                Objects.equals(valueList, that.valueList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), values, valueList);
    }
}
