package main.java.converter;

import main.java.parser.LineInfo;
import main.java.parser.OpSpTypeNode;
import main.java.util.OptionalUint;
import main.java.util.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

public final class UnionTypeObject extends UserType<UnionTypeObject.Info> {

    public UnionTypeObject(String name, GenericInfo info) {
        this(name, Collections.emptyList(), info);
    }

    public UnionTypeObject(String name, List<TypeObject> supers, GenericInfo info) {
        super(new Info(name, supers, info, new ArrayList<>()), "", true);
    }

    private UnionTypeObject(@NotNull UnionTypeObject other, List<TypeObject> generics) {
        super(other.info, other.typedefName, generics, other.isConst);
    }

    private UnionTypeObject(@NotNull UnionTypeObject other, String typedefName) {
        super(other.info, typedefName, other.generics, other.isConst);
    }

    @Override
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
    public Pair<Set<String>, Set<OpSpTypeNode>> contract() {
        return Pair.of(Collections.emptySet(), Collections.emptySet());
    }

    @Override
    public String name() {
        if (!generics.isEmpty()) {
            var valueJoiner = new StringJoiner(", ", "[", "]");
            for (var cls : generics) {
                valueJoiner.add(cls.name());
            }
            return info.name + valueJoiner.toString();
        } else {
            return typedefName.isEmpty() ? info.name : typedefName;
        }
    }

    @NotNull
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
        var op = info.operators.get(o);
        if (op != null) {
            if (isConst && op.isMut()) {
                return Optional.empty();
            }
            if (AccessLevel.canAccess(op.getAccessLevel(), access)) {
                return Optional.of(op.getInfo());
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    @Override
    public TypeObject typedefAs(String name) {
        return new UnionTypeObject(this, name);
    }

    @NotNull
    @Contract("_, _ -> new")
    @Override
    public TypeObject generify(LineInfo lineInfo, @NotNull TypeObject... args) {
        var trueArgs = info.info.generify(args);
        if (trueArgs.isEmpty() || trueArgs.orElseThrow().size() != info.info.getParams().size()) {
            throw CompilerException.of("Cannot generify object in this manner", lineInfo);
        } else {
            return new UnionTypeObject(this, trueArgs.orElseThrow());
        }
    }

    public OptionalUint getVariantNumber(String name) {
        for (int i = 0; i < info.variants.size(); i++) {
            var pair = info.variants.get(i);
            if (pair.getKey().equals(name)) {
                return OptionalUint.of(i);
            }
        }
        return OptionalUint.empty();
    }

    public Optional<TypeObject> variantType(String name) {
        for (int i = 0; i < info.variants.size(); i++) {
            var pair = info.variants.get(i);
            if (pair.getKey().equals(name)) {
                return Optional.of(pair.getValue());
            }
        }
        return Optional.empty();
    }

    public void setVariants(List<Pair<String, TypeObject>> variants) {
        assert !info.isSealed && info.variants.isEmpty();
        info.variants.addAll(variants);
    }

    public void setOperators(Map<OpSpTypeNode, MethodInfo> args) {
        assert !info.isSealed && info.operators.isEmpty();
        info.operators = args;
    }

    public void setStaticOperators(Map<OpSpTypeNode, MethodInfo> args) {
        assert !info.isSealed && info.staticOperators.isEmpty();
        info.staticOperators = args;
    }

    public void setAttributes(Map<String, AttributeInfo> attributes) {
        assert !info.isSealed && info.attributes == null;
        info.attributes = attributes;
    }

    public void setStaticAttributes(Map<String, AttributeInfo> attributes) {
        assert !info.isSealed && info.staticAttributes == null;
        info.staticAttributes = attributes;
    }

    public void seal() {
        addFulfilledInterfaces();
        info.seal();
    }

    protected static final class Info extends UserType.Info<MethodInfo, AttributeInfo> {
        private final List<Pair<String, TypeObject>> variants;

        public Info(String name, List<TypeObject> supers, GenericInfo info, List<Pair<String, TypeObject>> variants) {
            super(name, supers, info);
            this.variants = variants;
        }
    }
}
