package main.java.converter;

import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public abstract class AbstractDefaultInterface implements TypeObject {
    private String name;
    protected Map<OpSpTypeNode, TypeObject> operators;

    protected AbstractDefaultInterface(String name, Map<OpSpTypeNode, TypeObject> operators) {
        this.name = name;
        this.operators = operators;
    }

    @Override
    public boolean isSuperclass(TypeObject other) {
        for (var pair : operators.entrySet()) {
            var otherRet = other.operatorReturnType(pair.getKey());
            if (otherRet == null) {
                return false;
            } else {
                var selfRet = operatorReturnType(pair.getKey());
                if (selfRet.length > otherRet.length) {
                    return false;
                }
                for (int i = 0; i < selfRet.length; i++) {  // otherRet.length will always be >= selfRet.length
                    if (!selfRet[i].isSuperclass(otherRet[i])) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public String name() {
        return name;
    }

    @NotNull
    @Contract("null -> new")
    protected static TypeObject[] sanitizeType(TypeObject result) {
        return result instanceof ListTypeObject
                ? ((ListTypeObject) result).getValues().toArray(new TypeObject[0])
                : new TypeObject[] {result};
    }
}
