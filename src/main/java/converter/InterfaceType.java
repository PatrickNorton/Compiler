package main.java.converter;

import main.java.parser.LineInfo;
import main.java.parser.OpSpTypeNode;
import main.java.util.Pair;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

public final class InterfaceType extends UserType<InterfaceType.Info> {
    public InterfaceType(String name, GenericInfo generics) {
        this(name, generics, Collections.emptyList());
    }

    public InterfaceType(String name, GenericInfo info, List<TypeObject> supers) {
        super(new Info(name, supers, info), "", true);
    }

    public InterfaceType(String name, GenericInfo info, Map<OpSpTypeNode, MethodInfo> operators) {
        super(new Info(name, operators, info), "", true);
        this.seal();
    }

    private InterfaceType(InterfaceType other, String typedefName) {
        super(other.info, typedefName, other.generics, other.isConst);
    }

    private InterfaceType(InterfaceType other, boolean isConst) {
        super(other.info, other.typedefName, other.generics, isConst);
    }

    private InterfaceType(InterfaceType other, List<TypeObject> generics) {
        super(other.info, other.typedefName, generics, other.isConst);
    }

    @Override
    public String name() {
        if (typedefName.isEmpty()) {
            if (generics.isEmpty()) {
                return info.name;
            } else {
                var joiner = new StringJoiner(", ", "[", "]");
                for (var generic : generics) {
                    joiner.add(generic.name());
                }
                return info.name + joiner;
            }
        } else {
            return typedefName;
        }
    }

    @Override

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
    public TypeObject generify(LineInfo lineInfo, TypeObject... args) {
        var trueArgs = info.info.generify(args);
        if (trueArgs.isEmpty() || trueArgs.orElseThrow().size() != info.info.getParams().size()) {
            throw CompilerException.of("Cannot generify object in this manner", lineInfo);
        } else {
            return new InterfaceType(this, trueArgs.orElseThrow());
        }
    }

    @Override

    public TypeObject generifyWith(TypeObject parent, List<TypeObject> values) {
        return new InterfaceType(this, generifyWithInner(parent, values));
    }

    @Override
    public Optional<FunctionInfo> operatorInfo(OpSpTypeNode o, AccessLevel access) {
        var trueInfo = trueOperatorInfo(o, access);
        if (generics.isEmpty()) {
            return trueInfo.map(FunctionInfo::boundify);
        } else {
            return trueInfo.map(x -> x.generify(this, generics));
        }
    }

    public Optional<FunctionInfo> trueOperatorInfo(OpSpTypeNode o, AccessLevel access) {
        var operators = info.operators.get(o);
        if (operators == null) {
            return Optional.empty();
        } else {
            var attrLevel = operators.intoMethodInfo().getAccessLevel();
            if (AccessLevel.canAccess(attrLevel, access)) {
                return Optional.of(operators.fnInfo());
            } else {
                return Optional.empty();
            }
        }
    }

    @Override

    public Optional<TypeObject[]> staticOperatorReturnType(OpSpTypeNode o) {
        return info.staticOperatorReturnType(o);
    }

    @Override

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

    public void setAttributes(Map<String, AttributeInfo> attributes, Set<String> generics) {
        assert !info.isSealed && info.attributes == null;
        Map<String, InterfaceAttrInfo> result = new HashMap<>();
        for (var pair : attributes.entrySet()) {
            result.put(pair.getKey(), new InterfaceAttrInfo(pair.getValue(), !generics.contains(pair.getKey())));
        }
        info.attributes = result;
    }

    public void setStaticAttributes(Map<String, AttributeInfo> attributes, Set<String> generics) {
        assert !info.isSealed && info.staticAttributes == null;
        Map<String, InterfaceAttrInfo> result = new HashMap<>();
        for (var pair : attributes.entrySet()) {
            result.put(pair.getKey(), new InterfaceAttrInfo(pair.getValue(), !generics.contains(pair.getKey())));
        }
        info.staticAttributes = result;
    }

    public void setOperators(Map<OpSpTypeNode, MethodInfo> args, Set<OpSpTypeNode> generics) {
        assert !info.isSealed && info.operators.isEmpty();
        Map<OpSpTypeNode, InterfaceFnInfo> result = new HashMap<>();
        for (var pair : args.entrySet()) {
            result.put(pair.getKey(), new InterfaceFnInfo(pair.getValue(), !generics.contains(pair.getKey())));
        }
        info.operators = result;
    }

    public void setStaticOperators(Map<OpSpTypeNode, MethodInfo> args) {
        assert !info.isSealed && info.staticOperators.isEmpty();
        Map<OpSpTypeNode, InterfaceFnInfo> result = new HashMap<>();
        for (var pair : args.entrySet()) {
            result.put(pair.getKey(), new InterfaceFnInfo(pair.getValue(), true));
        }
        info.staticOperators = result;
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

        public Info(String name, List<TypeObject> supers, GenericInfo info) {
            super(name, supers, info);
        }

        public Info(String name, Map<OpSpTypeNode, MethodInfo> operators, GenericInfo info) {
            super(name, Collections.emptyList(), info);
            this.operators = convertMap(operators);
            this.staticOperators = new EnumMap<>(OpSpTypeNode.class);
        }

        private <T> Map<T, InterfaceFnInfo> convertMap(Map<T, MethodInfo> arg) {
            Map<T, InterfaceFnInfo> result = new HashMap<>();
            for (var pair : arg.entrySet()) {
                result.put(pair.getKey(), new InterfaceFnInfo(pair.getValue(), false));
            }
            return result;
        }
    }

    private static final class InterfaceFnInfo implements IntoMethodInfo {
        private final MethodInfo info;
        private final boolean hasImpl;

        public InterfaceFnInfo(MethodInfo info, boolean hasImpl) {
            this.info = info;
            this.hasImpl = hasImpl;
        }

        public TypeObject[] getReturns() {
            return info.getReturns();
        }

        @Override
        public MethodInfo intoMethodInfo() {
            return info;
        }

        public FunctionInfo fnInfo() {
            return info.getInfo();
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
