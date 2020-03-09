package main.java.converter;

import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class DefaultInterface implements TypeObject {
    private String name;
    private GenericInfo info;
    private Map<OpSpTypeNode, TypeObject> operators;

    public DefaultInterface(String name, GenericInfo info, Map<OpSpTypeNode, TypeObject> operators) {
        this.name = name;
        this.info = info;
        this.operators = operators;
    }

    public DefaultInterface(String name, Map<OpSpTypeNode, TypeObject> operators) {
        this.name = name;
        this.info = GenericInfo.empty();
        this.operators = operators;
    }

    @Override
    public boolean isSuperclass(TypeObject other) {
        for (var pair : operators.entrySet()) {
            var otherRet = other.operatorReturnType(pair.getKey());
            if (otherRet != null) {  // TODO: Ensure types match
                return false;
            }
        }
        return true;
    }

    @Override
    public TypeObject generify(@NotNull TypeObject... args) {
        assert args.length == info.size();
        return new GenerifiedDefaultInterface(this, List.of(args));
    }

    @Override
    public TypeObject[] operatorReturnType(OpSpTypeNode o) {
        return sanitizeType(internalReturnType(o));
    }

    public TypeObject[] sanitizeType(TypeObject result) {
        return result instanceof ListTypeObject
                ? ((ListTypeObject) result).getValues().toArray(new TypeObject[0])
                : new TypeObject[] {result};
    }

    private TypeObject internalReturnType(OpSpTypeNode o) {
        var result = operators.get(o);
        return result instanceof TemplateParam ? ((TemplateParam) result).getBound() : result;
    }

    TypeObject opRetType(OpSpTypeNode o) {
        return operators.get(o);
    }

    public Map<OpSpTypeNode, TypeObject> getOperators() {
        return operators;
    }

    @Override
    public String name() {
        return name;
    }
}
