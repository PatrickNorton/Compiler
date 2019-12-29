package main.java.converter;

public interface TypeObject {
    boolean isSubclass(TypeObject other);
    String name();
}
