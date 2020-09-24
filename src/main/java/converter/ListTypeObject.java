package main.java.converter;

import main.java.util.Zipper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    public TypeObject[] getValues() {
        return values.toArray(new TypeObject[0]);
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

    @Contract(pure = true)
    @Override
    @NotNull
    public String baseName() {
        return "";
    }

    @Override
    public boolean sameBaseType(TypeObject other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int baseHash() {
        throw new UnsupportedOperationException();
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

    @Override
    public Optional<Map<Integer, TypeObject>> generifyAs(TypeObject parent, TypeObject other) {
        if (!(other instanceof ListTypeObject)) {
            throw new UnsupportedOperationException();
        }
        Map<Integer, TypeObject> result = new HashMap<>();
        for (var pair : Zipper.of(this, (ListTypeObject) other)) {
            var map = pair.getKey().generifyAs(parent, pair.getValue());
            if (map.isEmpty() || !TypeObject.addGenericsToMap(map.orElseThrow(), result)) {
                return Optional.empty();
            }
        }
        return Optional.of(result);
    }

    @Contract("_, _ -> new")
    @Override
    @NotNull
    public TypeObject generifyWith(TypeObject parent, List<TypeObject> values) {
        if (this.values.size() == 1
                && this.values.get(0) instanceof TemplateParam
                && ((TemplateParam) this.values.get(0)).isVararg()) {
            return this.values.get(0).generifyWith(parent, values);
        } else {
            TypeObject[] result = new TypeObject[this.values.size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = this.values.get(i).generifyWith(parent, values);
            }
            return new ListTypeObject(result);
        }
    }

    public TypeObject[] toArray() {
        return values.toArray(new TypeObject[0]);
    }
}
