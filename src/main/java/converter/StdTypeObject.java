package main.java.converter;

import main.java.parser.DescriptorNode;
import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class StdTypeObject extends NameableType {
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

    @Nullable
    @Override
    public FunctionInfo operatorInfo(OpSpTypeNode o, DescriptorNode access) {
        var trueInfo = trueOperatorInfo(o, access);
        return trueInfo == null ? null : trueInfo.boundify();
    }

    public FunctionInfo trueOperatorInfo(OpSpTypeNode o, DescriptorNode access) {
        // TODO: Check access bounds
        return info.operators.get(o);
    }

    @Nullable
    @Override
    public TypeObject[] operatorReturnType(OpSpTypeNode o, DescriptorNode access) {
        var types = info.operatorReturnTypeWithGenerics(o, access);
        if (types == null) return null;
        TypeObject[] result = new TypeObject[types.length];
        for (int i = 0; i < types.length; i++) {
            var type = types[i];
            result[i] = type instanceof TemplateParam ? ((TemplateParam) type).getBound() : type;
        }
        return result;
    }

    @Nullable
    public TypeObject[] operatorReturnTypeWithGenerics(OpSpTypeNode o, DescriptorNode access) {
        return info.operatorReturnTypeWithGenerics(o, access);
    }

    @Override
    public TypeObject[] staticOperatorReturnType(OpSpTypeNode o) {
        return info.staticOperatorReturnType(o);
    }

    public List<TypeObject> getSupers() {
        return info.supers;
    }

    @Override
    public TypeObject attrType(String value, DescriptorNode access) {
        var type = attrTypeWithGenerics(value, access);
        return type instanceof TemplateParam ? ((TemplateParam) type).getBound() : type;
    }

    @Nullable
    public TypeObject attrTypeWithGenerics(String value, DescriptorNode access) {
        var attr = info.attributes.get(value);
        if (attr == null) return null;
        return DescriptorNode.canAccess(attr.getDescriptors(), access) ? attr.getType() : null;
    }

    public void setAttributes(Map<String, AttributeInfo> attributes) {
        assert info.attributes == null;
        info.attributes = attributes;
    }

    public void setStaticAttributes(Map<String, AttributeInfo> attributes) {
        assert info.staticAttributes == null;
        info.staticAttributes = attributes;
    }

    @Override
    public TypeObject staticAttrType(String value) {
        return info.staticAttributes.get(value).getType();
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
        private Map<String, AttributeInfo> attributes;
        private Map<String, AttributeInfo> staticAttributes;

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
        public TypeObject[] operatorReturnTypeWithGenerics(OpSpTypeNode o, DescriptorNode access) {
            if (operators.containsKey(o)) {  // TODO: Bounds-check
                return operators.get(o).getReturns();
            }
            for (var sup : supers) {
                var opRet = sup.operatorReturnType(o, access);
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
