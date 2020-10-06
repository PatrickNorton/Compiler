package main.java.converter;

/**
 * Shim existing in order to ensure that types of the same base are treated
 * as equal in hashed collections.
 *
 * @author Patrick Norton
 * @see TypeObject
 */
public final class BaseType {
    private final TypeObject value;

    public BaseType(TypeObject value) {
        this.value = value;
    }

    public TypeObject getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseType baseType = (BaseType) o;
        return value.sameBaseType(baseType.value);
    }

    @Override
    public int hashCode() {
        return value.baseHash();
    }
}
