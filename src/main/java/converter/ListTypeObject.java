package main.java.converter;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;

public final class ListTypeObject extends TypeObject implements Iterable<TypeObject> {
    private final List<TypeObject> values;
    private final String typedefName;

    public ListTypeObject(TypeObject... values) {
        this.values = List.of(values);
        this.typedefName = "";
    }

    @Contract(pure = true)
    private ListTypeObject(@NotNull ListTypeObject other, String typedefName) {
        this.values = other.values;
        this.typedefName = typedefName;
    }

    public TypeObject get(int i) {
        return values.get(i);
    }

    @Override
    public boolean isSuperclass(@NotNull TypeObject other) {
        throw new UnsupportedOperationException("Should not be instancing list types");
    }

    protected boolean isSubclass(@NotNull TypeObject other) {
        throw new UnsupportedOperationException("Should not be instancing list types");
    }

    @Override
    public String name() {
        if (!typedefName.isEmpty()) {
            return typedefName;
        }
        var sj = new StringJoiner(", ", "[", "]");
        for (var value : values) {
            sj.add(value.name());
        }
        return sj.toString();
    }

    @Contract(value = "_ -> new", pure = true)
    @Override
    @NotNull
    public TypeObject typedefAs(String name) {
        return new ListTypeObject(this, name);
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
