package main.java.converter;

import main.java.parser.OpSpTypeNode;

import java.util.List;
import java.util.Map;

public final class ClassInfo {
    private StdTypeObject type;
    private Map<OpSpTypeNode, List<Byte>> operatorDefs;
    private Map<String, List<Byte>> methodDefs;

    public ClassInfo(StdTypeObject type, Map<OpSpTypeNode, List<Byte>> operatorDefs,
                     Map<String, List<Byte>> methodDefs) {
        this.type = type;
        this.operatorDefs = operatorDefs;
        this.methodDefs = methodDefs;
    }

    public StdTypeObject getType() {
        return type;
    }

    public Map<OpSpTypeNode, List<Byte>> getOperatorDefs() {
        return operatorDefs;
    }

    public Map<String, List<Byte>> getMethodDefs() {
        return methodDefs;
    }
}
