package main.java.converter;

import main.java.parser.DescriptorNode;
import main.java.parser.OpSpTypeNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    public final boolean isSubclass(@NotNull TypeObject other) {
        if (this.equals(other)) {
            return true;
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
    public final TypeObject[] operatorReturnTypeWithGenerics(OpSpTypeNode o, DescriptorNode access) {
        return info.operatorReturnTypeWithGenerics(o, access);
    }

    @Nullable
    @Override
    public final TypeObject[] operatorReturnType(OpSpTypeNode o, DescriptorNode access) {
        var types = operatorReturnTypeWithGenerics(o, access);
        if (types == null) return null;
        TypeObject[] result = new TypeObject[types.length];
        for (int i = 0; i < types.length; i++) {
            var type = types[i];
            if (type instanceof TemplateParam) {
                result[i] = generics.isEmpty()
                        ? ((TemplateParam) type).getBound()
                        : generics.get(((TemplateParam) type).getIndex());
            } else {
                result[i] = type;
            }
        }
        return result;
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

    @NotNull
    private List<TypeObject> fulfilledInterfaces() {
        List<TypeObject> result = new ArrayList<>();
        for (var inter : Builtins.DEFAULT_INTERFACES) {
            if (!isSubclass(inter) && fulfillsContract(inter)) {
                result.add(inter);
            }
        }
        return result;
    }

    private boolean fulfillsContract(@NotNull UserType<?> contractor) {
        var contract = contractor.contract();
        for (var attr : contract.getKey()) {
            if (attrType(attr, DescriptorNode.PUBLIC) == null) {
                return false;
            }
        }
        for (var op : contract.getValue()) {
            if (operatorInfo(op, DescriptorNode.PUBLIC) == null) {
                return false;
            }
        }
        return false;
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
        public final TypeObject[] operatorReturnTypeWithGenerics(OpSpTypeNode o, DescriptorNode access) {
            if (operators.containsKey(o)) {  // TODO: Bounds-check
                return operators.get(o).intoFnInfo().getReturns();
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
        public final TypeObject[] staticOperatorReturnType(OpSpTypeNode o) {
            if (staticOperators.containsKey(o)) {
                return staticOperators.get(o).intoFnInfo().getReturns();
            }
            for (var sup : supers) {
                var opRet = sup.staticOperatorReturnType(o);
                if (opRet != null) {
                    return opRet;
                }
            }
            return null;
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
