package main.java.converter;

import main.java.parser.OpSpTypeNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ClassInfo {
    private StdTypeObject type;
    private List<Short> superConstants;
    private Map<String, Short> variables;
    private Map<String, Short> staticVariables;
    private Map<OpSpTypeNode, List<Byte>> operatorDefs;
    private Map<OpSpTypeNode, List<Byte>> staticOperators;
    private Map<String, List<Byte>> methodDefs;
    private Map<String, List<Byte>> staticMethods;
    private Map<String, Pair<List<Byte>, List<Byte>>> properties;

    private ClassInfo(StdTypeObject type, List<Short> superConstants,
                      Map<String, Short> variables, Map<String, Short> staticVariables,
                      Map<OpSpTypeNode, List<Byte>> operatorDefs, Map<OpSpTypeNode, List<Byte>> staticOperators,
                      Map<String, List<Byte>> methodDefs, Map<String, List<Byte>> staticMethods,
                      Map<String, Pair<List<Byte>, List<Byte>>> properties) {
        this.type = type;
        this.superConstants = superConstants;
        this.variables = variables;
        this.staticVariables = staticVariables;
        this.operatorDefs = operatorDefs;
        this.staticOperators = staticOperators;
        this.staticMethods = staticMethods;
        this.methodDefs = methodDefs;
        this.properties = properties;
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

    public Map<String, List<Byte>> getStaticMethods() {
        return staticMethods;
    }

    public Map<String, Pair<List<Byte>, List<Byte>>> getProperties() {
        return properties;
    }

    public String name() {
        return type.name();
    }

    @NotNull
    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>(StringConstant.strBytes(type.name()));
        bytes.addAll(Util.intToBytes(superConstants.size()));
        for (var superType : superConstants) {
            bytes.addAll(Util.intToBytes(superType));
        }
        bytes.addAll(Util.shortToBytes((short) type.getGenericInfo().size()));
        addVariables(bytes, variables);
        addVariables(bytes, staticVariables);
        addOperators(bytes, operatorDefs);
        addOperators(bytes, staticOperators);
        addMethods(bytes, methodDefs);
        addMethods(bytes, staticMethods);
        addProperties(bytes, properties);
        return bytes;
    }

    private static void addVariables(@NotNull List<Byte> bytes, @NotNull Map<String, Short> byteMap) {
        bytes.addAll(Util.intToBytes(byteMap.size()));
        for (var pair : byteMap.entrySet()) {
            bytes.addAll(StringConstant.strBytes(pair.getKey()));
            bytes.addAll(Util.shortToBytes(pair.getValue()));
        }
    }

    private static void addOperators(@NotNull List<Byte> bytes, @NotNull Map<OpSpTypeNode, List<Byte>> byteMap) {
        bytes.addAll(Util.intToBytes(byteMap.size()));
        for (var pair : byteMap.entrySet()) {
            bytes.add((byte) pair.getKey().ordinal());
            bytes.addAll(Util.intToBytes(pair.getValue().size()));
            bytes.addAll(pair.getValue());
        }
    }

    private static void addMethods(@NotNull List<Byte> bytes, @NotNull Map<String, List<Byte>> byteMap) {
        bytes.addAll(Util.intToBytes(byteMap.size()));
        for (var pair : byteMap.entrySet()) {
            bytes.addAll(StringConstant.strBytes(pair.getKey()));
            bytes.addAll(Util.intToBytes(pair.getValue().size()));
            bytes.addAll(pair.getValue());
        }
    }

    private static void addProperties(@NotNull List<Byte> bytes, @NotNull Map<String, Pair<List<Byte>, List<Byte>>> properties) {
        bytes.addAll(Util.intToBytes(properties.size()));
        for (var pair : properties.entrySet()) {
            bytes.addAll(StringConstant.strBytes(pair.getKey()));
            bytes.addAll(Util.intToBytes(pair.getValue().getKey().size()));
            bytes.addAll(pair.getValue().getKey());
            bytes.addAll(Util.intToBytes(pair.getValue().getValue().size()));
            bytes.addAll(pair.getValue().getValue());
        }
    }

    private static void addSet(@NotNull List<Byte> bytes, @NotNull Set<String> set) {
        bytes.addAll(Util.intToBytes(set.size()));
        for (var str : set) {
            bytes.addAll(StringConstant.strBytes(str));
        }
    }

    public static class Factory {
        private StdTypeObject type;
        private List<Short> superConstants;
        private Map<String, Short> variables;
        private Map<String, Short> staticVariables;
        private Map<OpSpTypeNode, List<Byte>> operatorDefs;
        private Map<OpSpTypeNode, List<Byte>> staticOperators;
        private Map<String, List<Byte>> methodDefs;
        private Map<String, List<Byte>> staticMethods;
        private Map<String, Pair<List<Byte>, List<Byte>>> properties;

        public Factory setType(StdTypeObject type) {
            assert this.type == null;
            this.type = type;
            return this;
        }

        public Factory setSuperConstants(List<Short> superConstants) {
            assert this.superConstants == null;
            this.superConstants = superConstants;
            return this;
        }

        public Factory setVariables(Map<String, Short> variables) {
            assert this.variables == null;
            this.variables = variables;
            return this;
        }

        public Factory setStaticVariables(Map<String, Short> staticVariables) {
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

        public Factory setProperties(Map<String, Pair<List<Byte>, List<Byte>>> properties) {
            assert this.properties == null;
            this.properties = properties;
            return this;
        }

        public ClassInfo create() {
            return new ClassInfo(type, superConstants, variables, staticVariables,
                    operatorDefs, staticOperators, methodDefs, staticMethods, properties);
        }
    }
}
