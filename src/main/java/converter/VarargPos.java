package main.java.converter;

import java.util.List;

public final class VarargPos implements ArgPosition {
    private final List<Integer> values;
    private final TypeObject genericType;

    public VarargPos(List<Integer> values, TypeObject genericType) {
        this.values = values;
        this.genericType = genericType;
    }

    public List<Integer> getValues() {
        return values;
    }

    public TypeObject getGenericType() {
        return genericType;
    }
}
