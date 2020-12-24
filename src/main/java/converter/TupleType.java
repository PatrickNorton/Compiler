package main.java.converter;

import main.java.parser.LineInfo;
import main.java.parser.OpSpTypeNode;
import main.java.util.Zipper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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
    public Optional<Iterable<String>> getDefined() {
        return Optional.of(TupleIterator::new);
    }

    @Contract(pure = true)
    @NotNull
    @UnmodifiableView
    @Override
    public List<TypeObject> getGenerics() {
        return generics;
    }

    @Override
    @Contract("_, _ -> new")
    @NotNull
    public TypeObject generify(LineInfo lineInfo, TypeObject... args) {
        if (generics.isEmpty()) {
            return new TupleType(args);
        } else {
            throw CompilerException.of("Cannot generify object", lineInfo);
        }
    }

    @Override
    @NotNull
    public Optional<TypeObject> attrType(String value, AccessLevel access) {
        try {
            var intVal = Integer.parseInt(value);
            return intVal >= 0 && intVal < generics.size() ? Optional.of(generics.get(intVal)) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Override
    @NotNull
    public Optional<FunctionInfo> operatorInfo(@NotNull OpSpTypeNode o, AccessLevel access) {
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
            case HASH:
                if (isHashable()) {
                    return Optional.of(new FunctionInfo(Builtins.INT));
                } else {
                    return Optional.empty();
                }
            default:
                return Optional.empty();
        }
    }

    @Override
    protected boolean isSubclass(@NotNull TypeObject other) {
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

    @Contract(pure = true)
    @Override
    @NotNull
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

    @Contract(pure = true)
    @Override
    @NotNull
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

    @Contract("_ -> new")
    @Override
    @NotNull
    public TypeObject typedefAs(String name) {
        return new TupleType(name, generics.toArray(new TypeObject[0]));
    }

    private boolean isHashable() {
        for (var generic : generics) {
            if (generic.operatorInfo(OpSpTypeNode.HASH, AccessLevel.PUBLIC).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private final class TupleIterator implements Iterator<String> {
        private int i = 0;

        @Override
        public boolean hasNext() {
            return i < generics.size();
        }

        @Override
        public String next() {
            if (i >= generics.size()) throw new NoSuchElementException();
            return Integer.toString(i++);
        }
    }
}
