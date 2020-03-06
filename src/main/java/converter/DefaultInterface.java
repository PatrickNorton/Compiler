package main.java.converter;

import main.java.parser.OpSpTypeNode;

import java.util.EnumSet;
import java.util.Set;

public class DefaultInterface implements TypeObject {
    private String name;
    private Set<OpSpTypeNode> operators;

    public DefaultInterface(String name, OpSpTypeNode... operators) {
        this.name = name;
        this.operators = EnumSet.of(operators[0], operators);
    }

    @Override
    public boolean isSuperclass(TypeObject other) {
        if (other instanceof DefaultInterface) {
            return ((DefaultInterface) other).operators.containsAll(operators);
        }
        for (var op : operators) {
            if (other.operatorReturnType(op) == null) {  // TODO: Ensure types match
                return false;
            }
        }
        return true;
    }

    @Override
    public String name() {
        return name;
    }
}
