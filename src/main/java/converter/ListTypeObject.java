package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;

public class ListTypeObject implements TypeObject, Iterable<TypeObject> {
    private List<TypeObject> values;

    public ListTypeObject(TypeObject... values) {
        this.values = List.of(values);
    }

    public TypeObject get(int i) {
        return values.get(i);
    }

    @Override
    public boolean isSuperclass(TypeObject other) {
        throw new UnsupportedOperationException("Should not be instancing list types");
    }

    @Override
    public String name() {
        var sj = new StringJoiner(", ", "[", "]");
        for (var value : values) {
            sj.add(value.name());
        }
        return sj.toString();
    }

    @NotNull
    @Override
    public Iterator<TypeObject> iterator() {
        return values.iterator();
    }

    public TypeObject[] toArray() {
        return values.toArray(new TypeObject[0]);
    }
}
