package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Objects;
import java.util.SortedSet;
import java.util.StringJoiner;
import java.util.TreeSet;

public class IntersectionTypeObject extends TypeObject {
    private final SortedSet<TypeObject> types;
    private final String typedefName;

    public IntersectionTypeObject(SortedSet<TypeObject> types) {
        this.types = Collections.unmodifiableSortedSet(types);
        this.typedefName = "";
    }

    private IntersectionTypeObject(SortedSet<TypeObject> types, String typedefName) {
        this.types = types;
        this.typedefName = typedefName;
    }

    @Override
    public String name() {
        var sj = new StringJoiner("&");
        for (var type : types) {
            sj.add(type.name());
        }
        return sj.toString();
    }

    @Override
    public TypeObject typedefAs(String name) {
        return new IntersectionTypeObject(types, name);
    }

    @Override
    public boolean isSuperclass(TypeObject other) {
        for (var subtype : types) {
            if (!subtype.isSuperclass(other)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isSubclass(@NotNull TypeObject other) {
        for (var subtype : types) {
            if (other.isSubclass(subtype)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntersectionTypeObject that = (IntersectionTypeObject) o;
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
        return new IntersectionTypeObject(Collections.unmodifiableSortedSet(newTypes), typedefName);
    }

    @Override
    public TypeObject makeConst() {
        SortedSet<TypeObject> newTypes = new TreeSet<>();
        for (var obj : types) {
            newTypes.add(obj.makeConst());
        }
        return new IntersectionTypeObject(Collections.unmodifiableSortedSet(newTypes), typedefName);
    }
}
