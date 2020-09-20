package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Types which can be generified into another object.
 *
 * @param <T> The return type of the generification
 * @author Patrick Norton
 */
public interface Template<T> {
    T generify(TypeObject... generics);

    static boolean addGenericsToMap(@NotNull Map<Integer, TypeObject> toAdd, Map<Integer, TypeObject> result) {
        for (var pair : toAdd.entrySet()) {
            int index = pair.getKey();
            var obj = pair.getValue();
            var resultType = result.get(index);
            if (resultType == null) {
                result.put(index, obj);
            } else {
                if (obj.isSuperclass(resultType)) {
                    result.put(index, obj);
                } else if (!resultType.isSuperclass(obj)) {
                    return false;
                }
            }
        }
        return true;
    }
}
