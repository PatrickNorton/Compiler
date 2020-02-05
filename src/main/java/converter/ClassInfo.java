package main.java.converter;

import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ClassInfo {
    private StdTypeObject type;
    private List<Integer> superConstants;
    private Set<String> variables;
    private Set<String> staticVariables;
    private Map<OpSpTypeNode, List<Byte>> operatorDefs;
    private Map<OpSpTypeNode, List<Byte>> staticOperators;
    private Map<String, List<Byte>> methodDefs;
    private Map<String, List<Byte>> staticMethods;

    private ClassInfo(StdTypeObject type, List<Integer> superConstants,
                      Set<String> variables, Set<String> staticVariables,
                      Map<OpSpTypeNode, List<Byte>> operatorDefs, Map<OpSpTypeNode, List<Byte>> staticOperators,
                      Map<String, List<Byte>> methodDefs, Map<String, List<Byte>> staticMethods) {
        this.type = type;
        this.superConstants = superConstants;
        this.variables = variables;
        this.staticVariables = staticVariables;
        this.operatorDefs = operatorDefs;
        this.staticOperators = staticOperators;
        this.staticMethods = staticMethods;
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

    @NotNull
    public List<Byte> toBytes() {
        var name = Util.strBytes(type.name());
        List<Byte> bytes = new ArrayList<>(Util.intToBytes(name.size()));
        bytes.addAll(name);
        bytes.addAll(Util.intToBytes(superConstants.size()));
        for (var superType : superConstants) {
            bytes.addAll(Util.intToBytes(superType));
        }
        addSet(bytes, variables);
        addSet(bytes, staticVariables);
        addMap(bytes, operatorDefs);
        addMap(bytes, staticOperators);
        addMap(bytes, methodDefs);
        addMap(bytes, staticMethods);

        return bytes;
    }

    private static void addMap(@NotNull List<Byte> bytes, @NotNull Map<?, List<Byte>> byteMap) {
        bytes.addAll(Util.intToBytes(byteMap.size()));
        for (var pair : byteMap.entrySet()) {
            bytes.addAll(pair.getValue());
        }
    }

    private static void addSet(@NotNull List<Byte> bytes, @NotNull Set<String> set) {
        bytes.addAll(Util.intToBytes(set.size()));
        for (var str : set) {
            var strBytes = Util.strBytes(str);
            bytes.addAll(Util.intToBytes(strBytes.size()));
            bytes.addAll(strBytes);
        }
    }

    public static class Factory {
        private StdTypeObject type;
        private List<Integer> superConstants;
        private Set<String> variables;
        private Set<String> staticVariables;
        private Map<OpSpTypeNode, List<Byte>> operatorDefs;
        private Map<OpSpTypeNode, List<Byte>> staticOperators;
        private Map<String, List<Byte>> methodDefs;
        private Map<String, List<Byte>> staticMethods;

        public Factory setType(StdTypeObject type) {
            assert this.type == null;
            this.type = type;
            return this;
        }

        public Factory setSuperConstants(List<Integer> superConstants) {
            assert this.superConstants == null;
            this.superConstants = superConstants;
            return this;
        }

        public Factory setVariables(Set<String> variables) {
            assert this.variables == null;
            this.variables = variables;
            return this;
        }

        public Factory setStaticVariables(Set<String> staticVariables) {
            assert this.staticVariables == null;
            this.staticVariables = staticVariables;
            return this;
        }

        public Factory setOperatorDefs(Map<OpSpTypeNode, List<Byte>> operatorDefs) {
            assert this.operatorDefs == null;
            this.operatorDefs = operatorDefs;
            return this;
        }

        public Factory setStaticOperators(Map<OpSpTypeNode, List<Byte>> staticOperators) {
            assert this.staticOperators == null;
            this.staticOperators = staticOperators;
            return this;
        }

        public Factory setMethodDefs(Map<String, List<Byte>> methodDefs) {
            assert this.methodDefs == null;
            this.methodDefs = methodDefs;
            return this;
        }

        public Factory setStaticMethods(Map<String, List<Byte>> staticMethods) {
            assert this.staticMethods == null;
            this.staticMethods = staticMethods;
            return this;
        }

        public ClassInfo create() {
            return new ClassInfo(type, superConstants, variables, staticVariables,
                    operatorDefs, staticOperators, methodDefs, staticMethods);
        }
    }
}
