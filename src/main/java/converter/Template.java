package main.java.converter;

/**
 * Types which can be generified into another object.
 *
 * @param <T> The return type of the generification
 * @author Patrick Norton
 */
public interface Template<T> {
    T generify(TypeObject... generics);
}
