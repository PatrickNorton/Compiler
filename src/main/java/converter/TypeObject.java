package main.java.converter;

import main.java.parser.IndexNode;
import main.java.parser.OpSpTypeNode;
import main.java.parser.OperatorTypeNode;
import main.java.parser.TestNode;
import main.java.parser.VariableNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.SortedSet;
import java.util.TreeSet;

public interface TypeObject extends LangObject, Comparable<TypeObject> {
    boolean isSuperclass(TypeObject other);
    String name();

    @Override
    default TypeObject getType() {
        return Builtins.TYPE.generify(this);
    }

    default FunctionInfo operatorInfo(OpSpTypeNode o) {
        return null;
    }

    default TypeObject[] operatorReturnType(OperatorTypeNode o) {
        return operatorReturnType(OpSpTypeNode.translate(o));
    }

    default TypeObject[] operatorReturnType(OpSpTypeNode o) {
        var info = operatorInfo(o);
        return info == null ? null : info.getReturns();
    }

    default TypeObject generify(TypeObject... args) {
        throw new UnsupportedOperationException("Cannot generify object");
    }

    default TypeObject attrType(String value) {
        throw new UnsupportedOperationException("Cannot get attribute type of object");
    }

    default TypeObject[] staticOperatorReturnType(OpSpTypeNode o) {
        return null;
    }

    default TypeObject staticAttrType(String value) {
        return null;
    }

    @Override
    default int compareTo(@NotNull TypeObject o) {
        return this.hashCode() - o.hashCode();
    }

    default TypeObject stripNull() {
        return this;
    }

    static TypeObject union(@NotNull TypeObject... values) {
        SortedSet<TypeObject> sortedSet = new TreeSet<>();
        for (var type : values) {
            if (type instanceof UnionTypeObject) {
                sortedSet.addAll(((UnionTypeObject) type).subTypes());
            } else {
                sortedSet.add(type);
            }
        }
        sortedSet.remove(Builtins.THROWS);
        if (sortedSet.isEmpty()) {
            return Builtins.THROWS;
        }
        return sortedSet.size() == 1 ? sortedSet.first() : new UnionTypeObject(sortedSet);
    }

    static TypeObject intersection(@NotNull TypeObject... values) {
        SortedSet<TypeObject> sortedSet = new TreeSet<>();
        for (var type : values) {
            if (type instanceof IntersectionTypeObject) {
                sortedSet.addAll(((IntersectionTypeObject) type).subTypes());
            } else {
                sortedSet.add(type);
            }
        }
        sortedSet.remove(Builtins.THROWS);
        if (sortedSet.isEmpty()) {
            return Builtins.THROWS;
        }
        return sortedSet.size() == 1 ? sortedSet.first() : new IntersectionTypeObject(sortedSet);
    }

    @NotNull
    @Contract(pure = true)
    static TypeObject optional(@NotNull TypeObject value) {
        return value instanceof OptionalTypeObject ? value : new OptionalTypeObject(value);
    }

    @Nullable
    static TypeObject of(CompilerInfo info, TestNode arg) {
        if (arg instanceof VariableNode) {
            return info.classOf(((VariableNode) arg).getName());
        } else if (arg instanceof IndexNode) {
            var node = (IndexNode) arg;
            var cls = of(info, node.getVar());
            var args = node.getIndices();
            if (cls == null)
                return null;
            TypeObject[] generics = new TypeObject[args.length];
            for (int i = 0; i < args.length; i++) {
                generics[i] = of(info, args[i]);
                if (generics[i] == null)
                    return null;
            }
            return cls.generify(generics);
        } else {
            return null;
        }
    }

    @NotNull
    @Contract(value = "_ -> new", pure = true)
    static TypeObject list(TypeObject... args) {
        return new ListTypeObject(args);
    }

    @NotNull
    static String[] name(@NotNull TypeObject... args) {
        String[] result = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            result[i] = args[i].name();
        }
        return result;
    }
}
