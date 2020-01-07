package main.java.converter;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class StdTypeObject implements TypeObject {
    private final String name;
    private final List<TypeObject> supers;
    private final List<TypeObject> declaredGenerics;
    private final List<String> generics;

    public StdTypeObject(String name) {
        this(name, Collections.emptyList(), Collections.emptyList());
    }

    public StdTypeObject(String name, List<TypeObject> supers, List<TypeObject> declaredGenerics) {
        this.name = name;
        this.supers = Collections.unmodifiableList(supers);
        this.declaredGenerics = Collections.unmodifiableList(declaredGenerics);
        this.generics = Collections.emptyList();
    }

    public StdTypeObject(String name, List<StdTypeObject> supers, List<String> generics, Object sentinel) {
        this.name = name;
        this.supers = Collections.unmodifiableList(supers);
        this.generics = Collections.unmodifiableList(generics);
        this.declaredGenerics = Collections.emptyList();
        assert sentinel == null;
    }

    public boolean isSubclass(TypeObject other) {
        if (this.equals(other)) {
            return true;
        }
        for (var sup : supers) {
            if (sup.isSubclass(other)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StdTypeObject that = (StdTypeObject) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(declaredGenerics, that.declaredGenerics) &&
                Objects.equals(generics, that.generics);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, declaredGenerics, generics);
    }
}
