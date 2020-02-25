package main.java.converter;

import main.java.parser.IndexNode;
import main.java.parser.OpSpTypeNode;
import main.java.parser.OperatorTypeNode;
import main.java.parser.TestNode;
import main.java.parser.VariableNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

public interface TypeObject extends LangObject, Comparable<TypeObject> {
    boolean isSuperclass(TypeObject other);
    String name();

    default TypeObject getType() {
        return new TypeTypeObject();
    }

    default TypeObject operatorReturnType(OperatorTypeNode o) {
        return operatorReturnType(OpSpTypeNode.translate(o));
    }

    default TypeObject operatorReturnType(OpSpTypeNode o) {
        return null;
    }

    default TypeObject generify(TypeObject... args) {
        throw new UnsupportedOperationException("Cannot generify object");
    }

    default TypeObject attrType(String value) {
        throw new UnsupportedOperationException("Cannot get attribute type of object");
    }

    @Override
    default int compareTo(@NotNull TypeObject o) {
        return this.hashCode() - o.hashCode();
    };

    static TypeObject union(TypeObject... values) {
        SortedSet<TypeObject> sortedSet = new TreeSet<>(Arrays.asList(values));
        assert !sortedSet.isEmpty();
        return sortedSet.size() == 1 ? sortedSet.first() : new UnionTypeObject(sortedSet);
    }

    static TypeObject intersection(TypeObject... values) {
        SortedSet<TypeObject> sortedSet = new TreeSet<>(Arrays.asList(values));
        assert !sortedSet.isEmpty();
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
}
