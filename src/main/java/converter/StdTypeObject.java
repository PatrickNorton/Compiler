package main.java.converter;

import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class StdTypeObject extends NameableType {
    private final Info info;
    private final String typedefName;

    public StdTypeObject(String name) {
        this(name, Collections.emptyList());
    }

    public StdTypeObject(String name, List<TypeObject> supers) {
        this.info = new Info(name, supers);
        this.typedefName = "";
    }

    public StdTypeObject(String name, GenericInfo info) {
        this(name, Collections.emptyList(), info);
    }

    public StdTypeObject(String name, List<TypeObject> supers, GenericInfo info) {
        this.info = new Info(name, supers, info);
        this.typedefName = "";
    }

    private StdTypeObject(@NotNull StdTypeObject other, String typedefName) {
        this.info = other.info;
        this.typedefName = typedefName;
    }

    @Override
    public boolean isSuperclass(TypeObject other) {
        if (this.equals(other)) {
            return true;
        } else {
            return other.isSubclass(this);
        }
    }

    @Override
    public boolean superWillRecurse() {
        return true;
    }

    public boolean isSubclass(@NotNull TypeObject other) {
        if (this.equals(other)) {
            return true;
        } else if (other instanceof AbstractDefaultInterface) {
            return other.isSuperclass(this);
        }
        for (var sup : info.supers) {
            if (sup.isSubclass(other)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String name() {
        return typedefName.isEmpty() ? info.name : typedefName;
    }

    @Override
    public TypeObject typedefAs(String name) {
        return new StdTypeObject(this, name);
    }

    @Override
    public TypeObject generify(@NotNull TypeObject... args) {
        if (args.length != info.info.getParams().size()) {
            throw new UnsupportedOperationException("Cannot generify object in this manner");
        } else {
            return new GenerifiedTypeObject(this, List.of(args));
        }
    }

    public GenericInfo getGenericInfo() {
        return info.info;
    }

    public void setOperators(Map<OpSpTypeNode, FunctionInfo> args) {
        assert info.operators.isEmpty();
        info.operators = args;
    }

    @Override
    public FunctionInfo operatorInfo(OpSpTypeNode o) {
        return trueOperatorInfo(o).boundify();
    }

    public FunctionInfo trueOperatorInfo(OpSpTypeNode o) {
        return info.operators.get(o);
    }

    @Override
    public TypeObject[] operatorReturnType(OpSpTypeNode o) {
        var types = operatorReturnTypeWithGenerics(o);
        if (types == null) return null;
        TypeObject[] result = new TypeObject[types.length];
        for (int i = 0; i < types.length; i++) {
            var type = types[i];
            result[i] = type instanceof TemplateParam ? ((TemplateParam) type).getBound() : type;
        }
        return result;
    }

    @Nullable
    public TypeObject[] operatorReturnTypeWithGenerics(OpSpTypeNode o) {
        return info.operatorReturnTypeWithGenerics(o);
    }

    @Override
    public TypeObject[] staticOperatorReturnType(OpSpTypeNode o) {
        return info.staticOperatorReturnType(o);
    }

    public List<TypeObject> getSupers() {
        return info.supers;
    }

    @Override
    public TypeObject attrType(String value) {
        var type = attrTypeWithGenerics(value);
        return type instanceof TemplateParam ? ((TemplateParam) type).getBound() : type;
    }

    public TypeObject attrTypeWithGenerics(String value) {
        return info.attributes.get(value);
    }

    public void setAttributes(Map<String, TypeObject> attributes) {
        assert info.attributes == null;
        info.attributes = attributes;
    }

    public void setStaticAttributes(Map<String, TypeObject> attributes) {
        assert info.staticAttributes == null;
        info.staticAttributes = attributes;
    }

    @Override
    public TypeObject staticAttrType(String value) {
        return info.staticAttributes.get(value);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof StdTypeObject && info.equals(((StdTypeObject) o).info);
    }

    @Override
    public int hashCode() {
        return Objects.hash(info);
    }

    private static final class Info {
        private final String name;
        private final List<TypeObject> supers;
        private Map<OpSpTypeNode, FunctionInfo> operators;
        private Map<OpSpTypeNode, FunctionInfo> staticOperators;
        private final GenericInfo info;
        private Map<String, TypeObject> attributes;
        private Map<String, TypeObject> staticAttributes;

        public Info(String name, List<TypeObject> supers) {
            this.name = name;
            this.supers = Collections.unmodifiableList(supers);
            this.operators = new EnumMap<>(OpSpTypeNode.class);
            this.staticOperators = new EnumMap<>(OpSpTypeNode.class);
            this.info = GenericInfo.empty();
        }

        public Info(String name, List<TypeObject> supers, GenericInfo info) {
            this.name = name;
            this.supers = Collections.unmodifiableList(supers);
            this.operators = new EnumMap<>(OpSpTypeNode.class);
            this.staticOperators = new EnumMap<>(OpSpTypeNode.class);
            this.info = info;
        }

        @Nullable
        public TypeObject[] operatorReturnTypeWithGenerics(OpSpTypeNode o) {
            if (operators.containsKey(o)) {
                return operators.get(o).getReturns();
            }
            for (var sup : supers) {
                var opRet = sup.operatorReturnType(o);
                if (opRet != null) {
                    return opRet;
                }
            }
            return null;
        }

        @Nullable
        public TypeObject[] staticOperatorReturnType(OpSpTypeNode o) {
            if (staticOperators.containsKey(o)) {
                return staticOperators.get(o).getReturns();
            }
            for (var sup : supers) {
                var opRet = sup.staticOperatorReturnType(o);
                if (opRet != null) {
                    return opRet;
                }
            }
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Info that = (Info) o;
            return Objects.equals(name, that.name) &&
                    Objects.equals(supers, that.supers) &&
                    Objects.equals(operators, that.operators);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, supers, operators);
        }
    }
}
