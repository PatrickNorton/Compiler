package main.java.converter;

import main.java.parser.LineInfo;
import main.java.parser.OpSpTypeNode;
import main.java.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

    public StdTypeObject(String name, List<TypeObject> supers, GenericInfo info) {
        this(name, supers, info, true);
    }

    public StdTypeObject(String name, List<TypeObject> supers, GenericInfo info, boolean isFinal) {
        super(new Info(name, supers, info, isFinal), "", true);
    }

    private StdTypeObject(StdTypeObject other, String typedefName) {
        super(other.info, typedefName, other.isConst);
    }

    private StdTypeObject(StdTypeObject other, boolean isConst) {
        super(other.info, other.typedefName, other.generics, isConst);
    }

    private StdTypeObject(StdTypeObject other, List<TypeObject> generics) {
        super(other.info, other.typedefName, generics, other.isConst);
    }

    @Override
    public String name() {
        if (generics.isEmpty()) {
            String name = typedefName.isEmpty() ? info.name : typedefName;
            if (isConst) {
                return name;
            } else {
                return String.format("mut %s", name);
            }
        } else {
            var valueJoiner = new StringJoiner(", ", "[", "]");
            for (var cls : generics) {
                valueJoiner.add(cls.name());
            }
            if (isConst) {
                return info.name + valueJoiner.toString();
            } else {
                return String.format("mut %s%s", info.name, valueJoiner);
            }
        }
    }

    @Override
    public TypeObject typedefAs(String name) {
        return new StdTypeObject(this, name);
    }

    @Override
    public TypeObject generify(LineInfo lineInfo,TypeObject... args) {
        var trueArgs = info.info.generify(args);
        if (trueArgs.isEmpty() || trueArgs.orElseThrow().size() != info.info.getParams().size()) {
            throw CompilerException.of("Cannot generify object in this manner", lineInfo);
        } else {
            return new StdTypeObject(this, trueArgs.orElseThrow());
        }
    }

    public GenericInfo getGenericInfo() {
        return info.info;
    }

    public void setOperators(Map<OpSpTypeNode, MethodInfo> args) {
        assert !info.isSealed && info.operators.isEmpty();
        info.operators = args;
    }

    public void setStaticOperators(Map<OpSpTypeNode, MethodInfo> args) {
        assert !info.isSealed && info.staticOperators.isEmpty();
        info.staticOperators = args;
    }

    @Override
    public Optional<FunctionInfo> operatorInfo(OpSpTypeNode o, AccessLevel access) {
        var trueInfo = trueOperatorInfo(o, access);
        if (generics.size() == 0) {
            return trueInfo.map(FunctionInfo::boundify);
        } else {
            return trueInfo.map(x -> x.generify(this, generics));
        }
    }

    public Optional<FunctionInfo> trueOperatorInfo(OpSpTypeNode o, AccessLevel access) {
        // TODO: Check access bounds
        return Optional.ofNullable(info.operators.get(o)).map(MethodInfo::getInfo);
    }

    @Override

    public Optional<TypeObject[]> staticOperatorReturnType(OpSpTypeNode o) {
        return info.staticOperatorReturnType(o);
    }

    public List<TypeObject> getSupers() {
        if (generics.isEmpty()) {
            return info.supers;
        } else {
            List<TypeObject> result = new ArrayList<>(info.supers.size());
            for (var value : info.supers) {
                result.add(value.generifyWith(this, generics));
            }
            return result;
        }
    }

    public void setSupers(List<TypeObject> supers) {
        assert !info.isSealed && info.supers.isEmpty();
        info.supers = supers;
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

    boolean constSemantics() {
        return info.isConstClass;
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

    public TypeObject generifyWith(TypeObject parent, List<TypeObject> values) {
        return new StdTypeObject(this, generifyWithInner(parent, values));
    }

    @Override

    public Pair<Set<String>, Set<OpSpTypeNode>> contract() {
        return Pair.of(Collections.emptySet(), Collections.emptySet());
    }

    protected static final class Info extends UserType.Info<MethodInfo, AttributeInfo> {
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
    }
}
