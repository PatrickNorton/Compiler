package main.java.converter;

import main.java.parser.OpSpTypeNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public abstract class UserType<I extends UserType.Info<?, ?>> extends NameableType {
    protected final I info;
    protected final String typedefName;
    protected final List<TypeObject> generics;
    protected final boolean isConst;

    public UserType(I info, String typedefName, boolean isConst) {
        this.info = info;
        this.typedefName = typedefName;
        this.generics = Collections.emptyList();
        this.isConst = isConst;
    }

    public UserType(I info, String typedefName, List<TypeObject> generics, boolean isConst) {
        this.info = info;
        this.typedefName = typedefName;
        this.generics = generics;
        this.isConst = isConst;
    }

    public abstract boolean isFinal();
    public abstract List<TypeObject> getSupers();
    public abstract GenericInfo getGenericInfo();
    public abstract Pair<Set<String>, Set<OpSpTypeNode>> contract();

    @Override
    public final String baseName() {
        return info.name;
    }

    public final boolean isSubclass(@NotNull TypeObject other) {
        if (this.equals(other)) {
            return true;
        } else if (other instanceof UserType && ((UserType<?>) other).info == info) {
            if (!((UserType<?>) other).isConst && isConst) {
                return false;
            } else if (((UserType<?>) other).generics.isEmpty()) {
                return ((UserType<?>) other).isConst || !isConst;
            } else if (generics.isEmpty()) {
                return false;
            } else {
                for (int i = 0; i < generics.size(); i++) {
                    if (!((UserType<?>) other).generics.get(i).isSuperclass(generics.get(i))) {
                        return false;
                    }
                }
                return true;
            }
        } else if (!isConst && this.makeConst().equals(other)) {
            return true;
        }
        for (var sup : info.supers) {
            if (other.isSuperclass(sup)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public final TypeObject[] operatorReturnTypeWithGenerics(OpSpTypeNode o, AccessLevel access) {
        return info.operatorReturnTypeWithGenerics(o, access);
    }

    @NotNull
    @Override
    public final Optional<TypeObject[]> operatorReturnType(OpSpTypeNode o, AccessLevel access) {
        var types = operatorReturnTypeWithGenerics(o, access);
        if (types == null) return Optional.empty();
        return Optional.of(Arrays.copyOf(types, types.length));
    }

    @Override
    @NotNull
    public final Optional<TypeObject> attrType(String value, AccessLevel access) {
        var type = attrTypeWithGenerics(value, access);
        return type.map(this::generifyAttrType);
    }

    @Override
    @NotNull
    public final Optional<TypeObject> staticAttrType(String value, AccessLevel access) {
        var type = staticAttrTypeWithGenerics(value, access);
        return type.map(this::generifyAttrType);
    }

    @NotNull
    private TypeObject generifyAttrType(TypeObject type) {
        if (type instanceof TemplateParam) {
            var t = (TemplateParam) type;
            return generics.isEmpty() ? t.getBound() : generics.get(t.getIndex());
        } else {
            return type;
        }
    }

    public final void addFulfilledInterfaces() {
        assert !info.isSealed;
        var fulfilled = fulfilledInterfaces();
        if (!fulfilled.isEmpty()) {
            var result = new ArrayList<>(info.supers);
            result.addAll(fulfilledInterfaces());
            info.supers = result;
        }
    }

    @Override
    @NotNull
    public final Map<Integer, TypeObject> generifyAs(TypeObject other) {
        assert other instanceof UserType;
        var otherT = (UserType<?>) other;
        Map<Integer, TypeObject> result = new HashMap<>();
        for (int i = 0; i < generics.size(); i++) {
            result.putAll(generics.get(i).generifyAs(otherT.generics.get(i)));
        }
        return result;
    }

    @NotNull
    private List<TypeObject> fulfilledInterfaces() {
        List<TypeObject> result = new ArrayList<>();
        for (var inter : ImportHandler.ALL_DEFAULT_INTERFACES.keySet()) {
            if (!isSubclass(inter) && fulfillsContract(inter)) {
                result.add(inter.generify(generifiedParams(inter)));
            }
        }
        return result;
    }

    @NotNull
    private TypeObject[] generifiedParams(@NotNull UserType<?> contractor) {
        var genericCount = contractor.info.info.size();
        if (genericCount == 0) {
            return new TypeObject[0];
        }
        var result = new TypeObject[genericCount];
        var contract = contractor.contract();
        for (var attr : contract.getKey()) {
            var attrT = attrTypeWithGenerics(attr, AccessLevel.PUBLIC).orElseThrow();
            var contractorAttr = contractor.attrTypeWithGenerics(attr, AccessLevel.PUBLIC).orElseThrow();
            for (var pair : contractorAttr.generifyAs(attrT).entrySet()) {
                var index = pair.getKey();
                var val = pair.getValue();
                if (result[index] == null) {
                    result[index] = val;
                } else {
                    result[index] = TypeObject.union(result[index], val);
                }
            }
        }
        for (var op : contract.getValue()) {
            var attrT = trueOperatorInfo(op, AccessLevel.PUBLIC).orElseThrow();
            var contractorAttr = contractor.trueOperatorInfo(op, AccessLevel.PUBLIC).orElseThrow();
            for (var pair : contractorAttr.toCallable().generifyAs(attrT.toCallable()).entrySet()) {
                var index = pair.getKey();
                var val = pair.getValue();
                if (result[index] == null) {
                    result[index] = val;
                } else {
                    result[index] = TypeObject.union(result[index], val);
                }
            }
        }
        assert Arrays.stream(result).noneMatch(Objects::isNull);
        return result;
    }

    protected static abstract class Info<O extends IntoFnInfo, A extends IntoAttrInfo> {
        protected final String name;
        protected List<TypeObject> supers;
        protected Map<OpSpTypeNode, O> operators;
        protected Map<OpSpTypeNode, O> staticOperators;
        protected final GenericInfo info;
        protected Map<String, A> attributes;
        protected Map<String, A> staticAttributes;
        protected boolean isSealed;

        public Info(String name, List<TypeObject> supers, GenericInfo info) {
            this.name = name;
            this.supers = Collections.unmodifiableList(supers);
            this.operators = new EnumMap<>(OpSpTypeNode.class);
            this.staticOperators = new EnumMap<>(OpSpTypeNode.class);
            this.info = info;
            this.isSealed = false;
        }

        @Nullable
        public final TypeObject[] operatorReturnTypeWithGenerics(OpSpTypeNode o, AccessLevel access) {
            if (operators.containsKey(o)) {  // TODO: Bounds-check
                return operators.get(o).intoFnInfo().getReturns();
            }
            for (var sup : supers) {
                var opRet = sup.operatorReturnType(o, access);
                if (opRet.isPresent()) {
                    return opRet.orElseThrow();
                }
            }
            return null;
        }

        @NotNull
        public final Optional<TypeObject[]> staticOperatorReturnType(OpSpTypeNode o) {
            if (staticOperators.containsKey(o)) {
                return Optional.of(staticOperators.get(o).intoFnInfo().getReturns());
            }
            for (var sup : supers) {
                var opRet = sup.staticOperatorReturnType(o);
                if (opRet.isPresent()) {
                    return opRet;
                }
            }
            return Optional.empty();
        }

        public final void seal() {
            assert !isSealed;
            isSealed = true;
            if (operators == null) {
                operators = Collections.emptyMap();
            }
            if (staticOperators == null) {
                staticOperators = Collections.emptyMap();
            }
            if (attributes == null) {
                attributes = Collections.emptyMap();
            }
            if (staticAttributes == null) {
                staticAttributes = Collections.emptyMap();
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Info<?, ?> info1 = (Info<?, ?>) o;
            return isSealed == info1.isSealed &&
                    Objects.equals(name, info1.name) &&
                    Objects.equals(supers, info1.supers) &&
                    Objects.equals(operators, info1.operators) &&
                    Objects.equals(staticOperators, info1.staticOperators) &&
                    Objects.equals(info, info1.info) &&
                    Objects.equals(attributes, info1.attributes) &&
                    Objects.equals(staticAttributes, info1.staticAttributes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, supers, operators, staticOperators, info, attributes, staticAttributes, isSealed);
        }
    }
}
