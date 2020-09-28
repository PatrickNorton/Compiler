package main.java.converter;

import main.java.parser.OpSpTypeNode;
import main.java.util.Pair;
import main.java.util.Zipper;
import org.jetbrains.annotations.NotNull;

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
        } else if (other instanceof UserType && this.sameBaseType(other)) {
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

    @NotNull
    public final Optional<TypeObject[]> operatorReturnTypeWithGenerics(OpSpTypeNode o, AccessLevel access) {
        return info.operatorReturnTypeWithGenerics(o, access);
    }

    @NotNull
    @Override
    public final Optional<TypeObject[]> operatorReturnType(OpSpTypeNode o, AccessLevel access) {
        var types = operatorReturnTypeWithGenerics(o, access);
        return types.map(t -> Arrays.copyOf(t, t.length));
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
    public final Optional<Map<Integer, TypeObject>> generifyAs(TypeObject parent, TypeObject other) {
        if (sameBaseType(other)) {
            return makeMatch(parent, generics, other.getGenerics());
        } else if (other instanceof ObjectType) {
            return Optional.of(new HashMap<>());
        } else if (other instanceof TemplateParam) {
            var param = (TemplateParam) other;
            if (param.getParent().sameBaseType(parent)) {
                return Optional.of(Map.of(param.getIndex(), this));
            } else {
                return Optional.empty();
            }
        }
        for (var sup : this.recursiveSupers()) {
            if (sup.sameBaseType(other)) {
                var supGenerics = sup.getGenerics();
                var objGenerics = other.getGenerics();
                return makeMatch(parent, supGenerics, objGenerics);
            }
        }
        return Optional.empty();
    }

    private Optional<Map<Integer, TypeObject>> makeMatch(
            TypeObject parent, @NotNull List<TypeObject> supGenerics, List<TypeObject> objGenerics
    ) {
        if (supGenerics.isEmpty() && objGenerics.isEmpty()) {
            return Optional.of(Collections.emptyMap());
        } else if (supGenerics.isEmpty()) {
            return Optional.empty();
        } else if (objGenerics.isEmpty()) {
            return Optional.of(Collections.emptyMap());
        }
        assert supGenerics.size() == objGenerics.size();
        Map<Integer, TypeObject> result = new HashMap<>(supGenerics.size());
        for (var pair : Zipper.of(supGenerics, objGenerics)) {
            var supG = pair.getKey();
            var objG = pair.getValue();
            if (supG instanceof TemplateParam) {
                var param = (TemplateParam) supG;
                if (param.getParent().sameBaseType(parent)) {
                    result.put(param.getIndex(), objG);
                } else {
                    return Optional.empty();
                }
            } else if (supG instanceof ListTypeObject && objG instanceof ListTypeObject) {
                var generics = supG.generifyAs(parent, objG);
                if (generics.isEmpty() || !TypeObject.addGenericsToMap(generics.orElseThrow(), result)) {
                    return Optional.empty();
                }
            } else if (!supG.equals(objG)) {
                return Optional.empty();
            }
        }
        return Optional.of(result);
    }

    @Override
    public final boolean sameBaseType(TypeObject other) {
        if (!(other instanceof UserType<?>)) {
            return false;
        }
        return this.info == ((UserType<?>) other).info;
    }

    @Override
    public int baseHash() {
        return info.hashCode();
    }

    @Override
    public List<TypeObject> getGenerics() {
        return generics;
    }

    @NotNull
    protected final List<TypeObject> generifyWithInner(TypeObject parent, List<TypeObject> values) {
        List<TypeObject> result = new ArrayList<>(generics.size());
        for (var generic : generics) {
            if (generic instanceof TemplateParam) {
                var template = (TemplateParam) generic;
                if (template.getParent().sameBaseType(parent)) {
                    result.add(values.get(template.getIndex()));
                } else {
                    result.add(template);
                }
            } else {
                result.add(generic.generifyWith(parent, values));
            }
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
            for (var pair : contractorAttr.generifyAs(contractor, attrT).orElseThrow().entrySet()) {
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
            var attr = contractorAttr.toCallable().generifyAs(contractor, attrT.toCallable());
            for (var pair : attr.orElseThrow().entrySet()) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserType<?> userType = (UserType<?>) o;
        return isConst == userType.isConst &&
                Objects.equals(info, userType.info) &&
                Objects.equals(typedefName, userType.typedefName) &&
                Objects.equals(generics, userType.generics);
    }

    @Override
    public int hashCode() {
        return Objects.hash(info, typedefName, generics, isConst);
    }

    @Override
    public final boolean canSetAttr(String name, AccessLevel access) {
        if (isConst) {
            return false;
        }
        var attr = info.attributes.get(name);
        if (attr == null) {
            return false;
        } else {
            var attrInfo = attr.intoAttrInfo();
            var accessLevel = attrInfo.getAccessLevel();
            if (!AccessLevel.canAccess(accessLevel, access)) {
                return false;
            } else if (accessLevel == AccessLevel.PUBGET && !AccessLevel.canAccess(AccessLevel.PRIVATE, access)) {
                return false;
            } else {
                return !attr.intoAttrInfo().getMutType().isConstRef();
            }
        }
    }

    protected static abstract class Info<O extends IntoMethodInfo, A extends IntoAttrInfo> {
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

        @NotNull
        public final Optional<TypeObject[]> operatorReturnTypeWithGenerics(OpSpTypeNode o, AccessLevel access) {
            if (operators.containsKey(o)) {  // TODO: Bounds-check
                return Optional.of(operators.get(o).intoMethodInfo().getReturns());
            }
            for (var sup : supers) {
                var opRet = sup.operatorReturnType(o, access);
                if (opRet.isPresent()) {
                    return opRet;
                }
            }
            return Optional.empty();
        }

        @NotNull
        public final Optional<TypeObject[]> staticOperatorReturnType(OpSpTypeNode o) {
            if (staticOperators.containsKey(o)) {
                return Optional.of(staticOperators.get(o).intoMethodInfo().getReturns());
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
    }
}
