package main.java.converter;

import main.java.parser.LineInfo;
import main.java.parser.OpSpTypeNode;
import main.java.util.Zipper;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

public final class TupleType extends TypeObject {
    private final List<TypeObject> generics;
    private final String typedefName;

    public TupleType() {
        this.generics = List.of();
        this.typedefName = "";
    }

    public TupleType(TypeObject... generics) {
        this.generics = List.of(generics);
        this.typedefName = "";
    }

    private TupleType(String typedefName, TypeObject... generics) {
        this.generics = List.of(generics);
        this.typedefName = typedefName;
    }

    @Override
    public List<TypeObject> getGenerics() {
        return Collections.unmodifiableList(generics);
    }

    @Override
    public TypeObject generify(LineInfo lineInfo, TypeObject... args) {
        if (generics.isEmpty()) {
            return new TupleType(args);
        } else {
            throw CompilerException.of("Cannot generify object", lineInfo);
        }
    }

    @Override
    public Optional<TypeObject> attrType(String value, AccessLevel access) {
        try {
            var intVal = Integer.parseInt(value);
            return intVal >= 0 && intVal < generics.size() ? Optional.of(generics.get(intVal)) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<FunctionInfo> operatorInfo(OpSpTypeNode o, AccessLevel access) {
        switch (o) {
            case EQUALS:
                return Optional.of(new FunctionInfo(
                        ArgumentInfo.of(Builtins.TUPLE.generify(generics.toArray(new TypeObject[0]))),
                        Builtins.BOOL
                ));
            case BOOL:
                return Optional.of(new FunctionInfo(Builtins.BOOL));
            case STR:
            case REPR:
                return Optional.of(new FunctionInfo(Builtins.STR));
            default:
                return Optional.empty();
        }
    }

    @Override
    protected boolean isSubclass(TypeObject other) {
        if (other instanceof TupleType) {
            var tuple = (TupleType) other;
            if (generics.size() != tuple.generics.size()) {
                return false;
            }
            for (var pair : Zipper.of(generics, tuple.generics)) {
                if (!pair.getKey().isSuperclass(pair.getValue())) {
                    return false;
                }
            }
            return true;
        } else if (other.willSuperRecurse()) {
            return false;
        } else {
            return other.isSuperclass(this);
        }
    }

    @Override
    public String name() {
        if (!typedefName.isEmpty()) {
            return typedefName;
        } else if (generics.isEmpty()) {
            return "tuple";
        } else {
            var sj = new StringJoiner(", ", "tuple[", "]");
            for (var generic : generics) {
                sj.add(generic.name());
            }
            return sj.toString();
        }
    }

    @Override
    public Optional<Map<Integer, TypeObject>> generifyAs(TypeObject parent, TypeObject other) {
        if (isSuperclass(other) || equals(other)) {
            return Optional.of(Collections.emptyMap());
        } else if (sameBaseType(other)) {
            var otherGen = other.getGenerics();
            if (generics.isEmpty()) {
                return Optional.of(Collections.emptyMap());
            } else if (generics.size() != otherGen.size()) {
                return Optional.empty();
            }
            Map<Integer, TypeObject> result = new HashMap<>(generics.size());
            for (var pair : Zipper.of(generics, otherGen)) {
                var map = pair.getKey().generifyAs(parent, pair.getValue());
                if (map.isEmpty() || TypeObject.addGenericsToMap(map.orElseThrow(), result)) {
                    return Optional.empty();
                }
            }
            return Optional.of(result);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String baseName() {
        return "tuple";
    }

    @Override
    public boolean sameBaseType(TypeObject other) {
        return other instanceof TupleType;
    }

    @Override
    public int baseHash() {
        return baseName().hashCode();
    }

    @Override
    public TypeObject typedefAs(String name) {
        return new TupleType(name, generics.toArray(new TypeObject[0]));
    }
}
