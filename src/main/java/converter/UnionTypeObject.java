package main.java.converter;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Objects;
import java.util.SortedSet;
import java.util.StringJoiner;
import java.util.TreeSet;

public final class UnionTypeObject extends TypeObject {
    private final SortedSet<TypeObject> types;
    private final String typedefName;

    public UnionTypeObject(SortedSet<TypeObject> types) {
        this.types = Collections.unmodifiableSortedSet(types);
        this.typedefName = "";
    }

    private UnionTypeObject(SortedSet<TypeObject> types, String typedefName) {
        this.types = types;
        this.typedefName = typedefName;
    }

    @Override
    public String name() {
        if (!typedefName.isEmpty()) {
            return typedefName;
        }
        var sj = new StringJoiner("|");
        for (var type : types) {
            sj.add(type.name());
        }
        return sj.toString();
    }

    @Contract("_ -> new")
    @Override
    @NotNull
    public TypeObject typedefAs(String name) {
        return new UnionTypeObject(types, name);
    }

    @Override
    public boolean isSuperclass(TypeObject other) {
        for (var subtype : types) {
            if (subtype.isSuperclass(other)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSubclass(@NotNull TypeObject other) {
        for (var subtype : types) {
            if (!subtype.isSubclass(other)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public TypeObject stripNull() {
        SortedSet<TypeObject> newTypes = new TreeSet<>();
        for (var t : types) {
            if (!t.equals(Builtins.NULL_TYPE)) {
                newTypes.add(t.stripNull());
            }
        }
        return new UnionTypeObject(newTypes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnionTypeObject that = (UnionTypeObject) o;
        return Objects.equals(types, that.types);
    }

    @Override
    public int hashCode() {
        return Objects.hash(types);
    }

    public SortedSet<TypeObject> subTypes() {
        return types;
    }

    @Override
    public TypeObject makeMut() {
        SortedSet<TypeObject> newTypes = new TreeSet<>();
        for (var obj : types) {
            newTypes.add(obj.makeMut());
        }
        return new UnionTypeObject(Collections.unmodifiableSortedSet(newTypes), typedefName);
    }

    @Override
    public TypeObject makeConst() {
        SortedSet<TypeObject> newTypes = new TreeSet<>();
        for (var obj : types) {
            newTypes.add(obj.makeConst());
        }
        return new UnionTypeObject(Collections.unmodifiableSortedSet(newTypes), typedefName);
    }
}
