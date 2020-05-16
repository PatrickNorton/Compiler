package main.java.converter;

import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class InterfaceType extends UserType {
    private final Info info;
    private final String typedefName;
    private final boolean isConst;

    public InterfaceType(String name) {
        this(name, Collections.emptyList());
    }

    public InterfaceType(String name, List<TypeObject> supers) {
        this.info = new Info(name, supers);
        this.typedefName = "";
        this.isConst = true;
    }

    private InterfaceType(@NotNull InterfaceType other, String typedefName) {
        this.info = other.info;
        this.isConst = other.isConst;
        this.typedefName = typedefName;
    }

    private InterfaceType(@NotNull InterfaceType other, boolean isConst) {
        this.info = other.info;
        this.typedefName = other.typedefName;
        this.isConst = isConst;
    }

    @Override
    public boolean isSuperclass(TypeObject other) {
        return false;
    }

    @Override
    public boolean isSubclass(@NotNull TypeObject other) {
        return false;
    }

    @Override
    public String name() {
        return typedefName.isEmpty() ? info.name : typedefName;
    }

    @Override
    @NotNull
    public TypeObject typedefAs(String name) {
        return new InterfaceType(this, name);
    }

    public boolean isFinal() {
        return false;
    }

    @Override
    public List<TypeObject> getSupers() {
        return info.supers;
    }

    @Override
    public GenericInfo getGenericInfo() {
        return info.info;
    }

    public void setAttributes(@NotNull Map<String, AttributeInfo> attributes, Set<String> generics) {
        assert !info.isSealed && info.attributes == null;
        Map<String, InterfaceAttrInfo> result = new HashMap<>();
        for (var pair : attributes.entrySet()) {
            result.put(pair.getKey(), new InterfaceAttrInfo(pair.getValue(), generics.contains(pair.getKey())));
        }
        info.attributes = result;
    }

    public void setStaticAttributes(@NotNull Map<String, AttributeInfo> attributes, Set<String> generics) {
        assert !info.isSealed && info.staticAttributes == null;
        Map<String, InterfaceAttrInfo> result = new HashMap<>();
        for (var pair : attributes.entrySet()) {
            result.put(pair.getKey(), new InterfaceAttrInfo(pair.getValue(), generics.contains(pair.getKey())));
        }
        info.staticAttributes = result;
    }

    public void setOperators(@NotNull Map<OpSpTypeNode, FunctionInfo> args, Set<OpSpTypeNode> generics) {
        assert !info.isSealed && info.operators.isEmpty();
        Map<OpSpTypeNode, InterfaceFnInfo> result = new HashMap<>();
        for (var pair : args.entrySet()) {
            result.put(pair.getKey(), new InterfaceFnInfo(pair.getValue(), generics.contains(pair.getKey())));
        }
        info.operators = result;
    }

    @Override
    public TypeObject makeConst() {
        return isConst ? this : new InterfaceType(this, true);
    }

    @Override
    public TypeObject makeMut() {
        return isConst ? new InterfaceType(this, false) : this;
    }


    private static final class Info {
        private final String name;
        private final List<TypeObject> supers;
        private Map<OpSpTypeNode, InterfaceFnInfo> operators;
        private Map<OpSpTypeNode, InterfaceFnInfo> staticOperators;
        private final GenericInfo info;
        private Map<String, InterfaceAttrInfo> attributes;
        private Map<String, InterfaceAttrInfo> staticAttributes;
        private boolean isConstClass;
        private boolean isSealed;

        public Info(String name, List<TypeObject> supers) {
            this.name = name;
            this.supers = Collections.unmodifiableList(supers);
            this.operators = new EnumMap<>(OpSpTypeNode.class);
            this.staticOperators = new EnumMap<>(OpSpTypeNode.class);
            this.info = GenericInfo.empty();
            this.isConstClass = false;
        }

        public Info(String name, List<TypeObject> supers, GenericInfo info) {
            this.name = name;
            this.supers = Collections.unmodifiableList(supers);
            this.operators = new EnumMap<>(OpSpTypeNode.class);
            this.staticOperators = new EnumMap<>(OpSpTypeNode.class);
            this.info = info;
            this.isConstClass = false;
        }
    }

    private static final class InterfaceFnInfo {
        private FunctionInfo info;
        private boolean hasImpl;

        public InterfaceFnInfo(FunctionInfo info, boolean hasImpl) {
            this.info = info;
            this.hasImpl = hasImpl;
        }
    }

    private static final class InterfaceAttrInfo {
        private AttributeInfo info;
        private boolean hasImpl;

        public InterfaceAttrInfo(AttributeInfo info, boolean hasImpl) {
            this.info = info;
            this.hasImpl = hasImpl;
        }
    }
}
