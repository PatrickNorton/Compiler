package main.java.converter;

import main.java.parser.OpSpTypeNode;

public class GenerifiedTypeTypeObject implements TypeObject {
    private TypeObject type;

    public GenerifiedTypeTypeObject(TypeObject type) {
        this.type = type;
    }

    @Override
    public boolean isSuperclass(TypeObject other) {
        return other instanceof GenerifiedTypeTypeObject && ((GenerifiedTypeTypeObject) other).type == this.type;
    }

    @Override
    public String name() {
        return String.format("type[%s]", type.name());
    }

    @Override
    public TypeObject[] operatorReturnType(OpSpTypeNode o) {
        if (o == OpSpTypeNode.CALL) {
            return new TypeObject[] {type};
        }
        return type.staticOperatorReturnType(o);
    }

    @Override
    public TypeObject attrType(String value) {
        return type.staticAttrType(value);
    }

    @Override
    public FunctionInfo operatorInfo(OpSpTypeNode o) {
        if (o == OpSpTypeNode.CALL) {
            return new FunctionInfo(type.operatorInfo(OpSpTypeNode.NEW).getArgs(), type);
        } else {
            return null;
        }
    }
}
