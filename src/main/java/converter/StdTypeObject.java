package main.java.converter;

import main.java.parser.DescriptorNode;
import main.java.parser.OpSpTypeNode;
import main.java.util.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

public final class StdTypeObject extends UserType<StdTypeObject.Info> {

    public StdTypeObject(String name) {
        this(name, Collections.emptyList());
    }

    public StdTypeObject(String name, List<TypeObject> supers) {
        super(new Info(name, supers), "", true);
    }

    public StdTypeObject(String name, GenericInfo info) {
        this(name, Collections.emptyList(), info, true);
    }

    public StdTypeObject(String name, List<TypeObject> supers, GenericInfo info, boolean isFinal) {
        super(new Info(name, supers, info, isFinal), "", true);
    }

    private StdTypeObject(@NotNull StdTypeObject other, String typedefName) {
        super(other.info, typedefName, other.isConst);
    }

    private StdTypeObject(@NotNull StdTypeObject other, boolean isConst) {
        super(other.info, other.typedefName, isConst);
    }

    private StdTypeObject(@NotNull StdTypeObject other, List<TypeObject> generics) {
        super(other.info, other.typedefName, generics, other.isConst);
    }

    @Override
    public String name() {
        if (generics.isEmpty()) {
            return typedefName.isEmpty() ? info.name : typedefName;
        } else {
            var valueJoiner = new StringJoiner(", ", "[", "]");
            for (var cls : generics) {
                valueJoiner.add(cls.name());
            }
            return info.name + valueJoiner.toString();
        }
    }

    @NotNull
    @Contract("_ -> new")
    @Override
    public TypeObject typedefAs(String name) {
        return new StdTypeObject(this, name);
    }

    @NotNull
    @Contract("_ -> new")
    @Override
    public TypeObject generify(@NotNull TypeObject... args) {
        var trueArgs = info.info.generify(args);
        if (trueArgs.size() != info.info.getParams().size()) {
            throw new UnsupportedOperationException("Cannot generify object in this manner");
        } else {
            return new StdTypeObject(this, trueArgs);
        }
    }

    public GenericInfo getGenericInfo() {
        return info.info;
    }

    public void setOperators(Map<OpSpTypeNode, FunctionInfo> args) {
        assert !info.isSealed && info.operators.isEmpty();
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

    @Override
    public TypeObject[] staticOperatorReturnType(OpSpTypeNode o) {
        return info.staticOperatorReturnType(o);
    }

    public List<TypeObject> getSupers() {
        return info.supers;
    }

    @Nullable
    public TypeObject attrTypeWithGenerics(String value, DescriptorNode access) {
        var attr = info.attributes.get(value);
        if (attr == null || (isConst && attr.getDescriptors().contains(DescriptorNode.MUT))) {
            return null;
        }
        return DescriptorNode.canAccess(attr.getDescriptors(), access) ? attr.getType() : null;
    }

    public void setAttributes(Map<String, AttributeInfo> attributes) {
        assert !info.isSealed && info.attributes == null;
        info.attributes = attributes;
    }

    public void setStaticAttributes(Map<String, AttributeInfo> attributes) {
        assert !info.isSealed && info.staticAttributes == null;
        info.staticAttributes = attributes;
    }

    void isConstClass() {
        assert !info.isSealed && !info.isConstClass;
        info.isConstClass = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StdTypeObject that = (StdTypeObject) o;
        return (info.isConstClass || isConst == that.isConst) && info == that.info;
    }

    @Override
    public int hashCode() {
        return Objects.hash(info, isConst);
    }

    @Override
    public TypeObject makeConst() {
        return isConst ? this : new StdTypeObject(this, true);
    }

    @Override
    public TypeObject makeMut() {
        return isConst ? new StdTypeObject(this, false) : this;
    }

    public boolean isFinal() {
        return info.isFinal;
    }

    public void seal() {
        addFulfilledInterfaces();
        info.seal();
    }

    @Override
    @NotNull
    public Pair<Set<String>, Set<OpSpTypeNode>> contract() {
        return Pair.of(Collections.emptySet(), Collections.emptySet());
    }

    protected static final class Info extends UserType.Info<FunctionInfo, AttributeInfo> {
        private boolean isConstClass;
        private final boolean isFinal;

        public Info(String name, List<TypeObject> supers) {
            super(name, supers, GenericInfo.empty());
            this.isFinal = true;
            this.isConstClass = false;
        }

        public Info(String name, List<TypeObject> supers, GenericInfo info, boolean isFinal) {
            super(name, supers, info);
            this.isFinal = isFinal;
            this.isConstClass = false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            Info info = (Info) o;
            return isConstClass == info.isConstClass &&
                    isFinal == info.isFinal;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), isConstClass, isFinal);
        }
    }
}
