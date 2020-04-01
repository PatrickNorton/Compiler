package main.java.converter;

import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.NotNull;

public class GenerifiedTypeTypeObject extends TypeObject {
    private TypeObject type;
    private String typedefName;

    public GenerifiedTypeTypeObject(TypeObject type) {
        this.type = type;
        this.typedefName = "";
    }

    private GenerifiedTypeTypeObject(TypeObject other, String typedefName) {
        this.type = other;
        this.typedefName = typedefName;
    }

    @Override
    public boolean isSuperclass(TypeObject other) {
        return other instanceof GenerifiedTypeTypeObject && ((GenerifiedTypeTypeObject) other).type == this.type;
    }

    @Override
    public boolean isSubclass(@NotNull TypeObject other) {
        return false;
    }

    @Override
    public String name() {
        return typedefName.isEmpty() ? String.format("type[%s]", type.name()) : typedefName;
    }

    @Override
    public TypeObject typedefAs(String name) {
        return new GenerifiedTypeTypeObject(type, name);
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
