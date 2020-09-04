package main.java.converter;

import main.java.parser.LineInfo;
import main.java.parser.OpSpTypeNode;
import main.java.util.Zipper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.List;
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

    @Contract(pure = true)
    @NotNull
    @UnmodifiableView
    public List<TypeObject> getGenerics() {
        return Collections.unmodifiableList(generics);
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
    @Nullable
    public TypeObject attrType(String value, AccessLevel access) {
        try {
            var intVal = Integer.parseInt(value);
            return intVal > 0 && intVal < generics.size() ? generics.get(intVal) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    @Nullable
    public FunctionInfo operatorInfo(@NotNull OpSpTypeNode o, AccessLevel access) {
        switch (o) {
            case EQUALS:
                return new FunctionInfo(
                        ArgumentInfo.of(Builtins.TUPLE.generify(generics.toArray(new TypeObject[0]))),
                        Builtins.BOOL
                );
            case BOOL:
                return new FunctionInfo(Builtins.BOOL);
            case STR:
            case REPR:
                return new FunctionInfo(Builtins.STR);
            default:
                return null;
        }
    }

    @Override
    protected boolean isSubclass(@NotNull TypeObject other) {
        if (other instanceof TupleType) {
            var tuple = (TupleType) other;
            if (generics.size() != tuple.generics.size()) {
                return false;
            }
            for (var pair : new Zipper<>(generics, tuple.generics)) {
                if (!pair.getKey().isSuperclass(pair.getValue())) {
                    return false;
                }
            }
            return true;
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

    @Contract(pure = true)
    @Override
    @NotNull
    public String baseName() {
        return "tuple";
    }

    @Contract("_ -> new")
    @Override
    @NotNull
    public TypeObject typedefAs(String name) {
        return new TupleType(name, generics.toArray(new TypeObject[0]));
    }
}
