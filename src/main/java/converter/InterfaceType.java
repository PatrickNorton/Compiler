package main.java.converter;

import main.java.parser.OpSpTypeNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class InterfaceType extends UserType<InterfaceType.Info> {
    public InterfaceType(String name) {
        this(name, Collections.emptyList());
    }

    public InterfaceType(String name, List<TypeObject> supers) {
        super(new Info(name, supers), "", true);
    }

    private InterfaceType(@NotNull InterfaceType other, String typedefName) {
        super(other.info, typedefName, other.isConst);
    }

    private InterfaceType(@NotNull InterfaceType other, boolean isConst) {
        super(other.info, other.typedefName, isConst);
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

    @Override
    @NotNull
    public Pair<Set<String>, Set<OpSpTypeNode>> contract() {
        if (info.cachedContract != null) {
            return info.cachedContract;
        }
        Set<String> methods = new HashSet<>(info.attributes.size() + info.staticAttributes.size());
        for (var pair : info.attributes.entrySet()) {
            if (!pair.getValue().hasImpl) {
                methods.add(pair.getKey());
            }
        }
        for (var pair : info.staticAttributes.entrySet()) {
            if (!pair.getValue().hasImpl) {
                methods.add(pair.getKey());
            }
        }
        Set<OpSpTypeNode> ops = EnumSet.noneOf(OpSpTypeNode.class);
        for (var pair : info.operators.entrySet()) {
            if (!pair.getValue().hasImpl) {
                ops.add(pair.getKey());
            }
        }
        for (var sup : info.supers) {
            if (sup instanceof UserType) {
                var contract = ((UserType<?>) sup).contract();
                methods.addAll(contract.getKey());
                ops.addAll(contract.getValue());
            }
        }
        var contract = Pair.of(methods, ops);
        info.cachedContract = contract;
        return contract;
    }

    public void setAttributes(@NotNull Map<String, AttributeInfo> attributes, Set<String> generics) {
        assert !info.isSealed && info.attributes == null;
        Map<String, InterfaceAttrInfo> result = new HashMap<>();
        for (var pair : attributes.entrySet()) {
            result.put(pair.getKey(), new InterfaceAttrInfo(pair.getValue(), !generics.contains(pair.getKey())));
        }
        info.attributes = result;
    }

    public void setStaticAttributes(@NotNull Map<String, AttributeInfo> attributes, Set<String> generics) {
        assert !info.isSealed && info.staticAttributes == null;
        Map<String, InterfaceAttrInfo> result = new HashMap<>();
        for (var pair : attributes.entrySet()) {
            result.put(pair.getKey(), new InterfaceAttrInfo(pair.getValue(), !generics.contains(pair.getKey())));
        }
        info.staticAttributes = result;
    }

    public void setOperators(@NotNull Map<OpSpTypeNode, FunctionInfo> args, Set<OpSpTypeNode> generics) {
        assert !info.isSealed && info.operators.isEmpty();
        Map<OpSpTypeNode, InterfaceFnInfo> result = new HashMap<>();
        for (var pair : args.entrySet()) {
            result.put(pair.getKey(), new InterfaceFnInfo(pair.getValue(), !generics.contains(pair.getKey())));
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

    public void seal() {
        info.seal();
    }

    protected static final class Info extends UserType.Info<InterfaceFnInfo, InterfaceAttrInfo> {
        private Pair<Set<String>, Set<OpSpTypeNode>> cachedContract;

        public Info(String name, List<TypeObject> supers) {
            super(name, supers, GenericInfo.empty());
            this.operators = new EnumMap<>(OpSpTypeNode.class);
            this.staticOperators = new EnumMap<>(OpSpTypeNode.class);
        }

        public Info(String name, List<TypeObject> supers, GenericInfo info) {
            super(name, supers, info);
            this.operators = new EnumMap<>(OpSpTypeNode.class);
            this.staticOperators = new EnumMap<>(OpSpTypeNode.class);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            Info info = (Info) o;
            return Objects.equals(cachedContract, info.cachedContract);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), cachedContract);
        }
    }

    private static final class InterfaceFnInfo implements IntoFnInfo {
        private final FunctionInfo info;
        private final boolean hasImpl;

        public InterfaceFnInfo(FunctionInfo info, boolean hasImpl) {
            this.info = info;
            this.hasImpl = hasImpl;
        }

        public TypeObject[] getReturns() {
            return info.getReturns();
        }

        @Override
        public FunctionInfo intoFnInfo() {
            return info;
        }
    }

    private static final class InterfaceAttrInfo implements IntoAttrInfo {
        private final AttributeInfo info;
        private final boolean hasImpl;

        public InterfaceAttrInfo(AttributeInfo info, boolean hasImpl) {
            this.info = info;
            this.hasImpl = hasImpl;
        }

        @Override
        public AttributeInfo intoAttrInfo() {
            return info;
        }
    }
}
