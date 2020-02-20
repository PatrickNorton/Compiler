package main.java.converter;

import main.java.parser.OpSpTypeNode;

public class GenerifiedTypeTypeObject implements TypeObject {
    private TypeObject type;

    public GenerifiedTypeTypeObject(TypeObject type) {
        this.type = type;
    }

    @Override
    public boolean isSubclass(TypeObject other) {
        return other instanceof GenerifiedTypeTypeObject && ((GenerifiedTypeTypeObject) other).type == this.type;
    }

    @Override
    public String name() {
        return String.format("type[%s]", type);
    }

    public TypeObject operatorReturnType(OpSpTypeNode o) {
        if (o == OpSpTypeNode.CALL) {
            return type;
        }
        throw new UnsupportedOperationException("Cannot get type");
    }
}
