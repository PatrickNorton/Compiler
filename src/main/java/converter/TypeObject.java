package main.java.converter;

import main.java.parser.BaseNode;
import main.java.parser.DescriptorNode;
import main.java.parser.IndexNode;
import main.java.parser.LineInfo;
import main.java.parser.OpSpTypeNode;
import main.java.parser.OperatorTypeNode;
import main.java.parser.TestNode;
import main.java.parser.VariableNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.SortedSet;
import java.util.TreeSet;

public abstract class TypeObject implements LangObject, Comparable<TypeObject> {
    public abstract boolean isSuperclass(TypeObject other);
    public abstract boolean isSubclass(@NotNull TypeObject other);
    public abstract String name();
    public abstract TypeObject typedefAs(String name);

    public TypeObject makeConst() {
        return this;
    }

    public TypeObject makeMut() {
        return this;
    }

    boolean superWillRecurse() {
        return false;
    }

    boolean subWillRecurse() {
        return false;
    }

    @Override
    public TypeObject getType() {
        return Builtins.TYPE.generify(this);
    }

    public FunctionInfo operatorInfo(OpSpTypeNode o, DescriptorNode access) {
        return null;
    }

    public final TypeObject[] operatorReturnType(OperatorTypeNode o, DescriptorNode access) {
        return operatorReturnType(OpSpTypeNode.translate(o), access);
    }

    public final TypeObject[] operatorReturnType(OperatorTypeNode o, @NotNull CompilerInfo info) {
        return operatorReturnType(OpSpTypeNode.translate(o), info.accessLevel(this));
    }

    public TypeObject[] operatorReturnType(OpSpTypeNode o, DescriptorNode access) {
        var info = operatorInfo(o, access);
        return info == null ? null : info.getReturns();
    }

    public final TypeObject[] operatorReturnType(OpSpTypeNode o, @NotNull CompilerInfo info) {
        return operatorReturnType(o, info.accessLevel(this));
    }

    public TypeObject generify(TypeObject... args) {
        throw new UnsupportedOperationException("Cannot generify object");
    }

    @Nullable
    public TypeObject attrType(String value, DescriptorNode access) {
        return null;
    }

    public TypeObject[] staticOperatorReturnType(OpSpTypeNode o) {
        return null;
    }

    public TypeObject staticAttrType(String value) {
        return null;
    }

    @NotNull
    public final FunctionInfo tryOperatorInfo(LineInfo lineInfo, OpSpTypeNode o, DescriptorNode access) {
        var info = operatorInfo(o, access);
        if (info == null) {
            if (access != DescriptorNode.PRIVATE && operatorInfo(o, DescriptorNode.PRIVATE) != null) {
                throw CompilerException.format(
                        "Cannot get '%s' from type '%s': operator has a too-strict access level",
                        lineInfo, o, name()
                );
            } else if (makeMut().operatorInfo(o, access) != null) {
                throw CompilerException.format(
                        "'%s' requires a mut variable for type '%s'",
                        lineInfo, o, name()
                );
            } else {
                throw CompilerException.format("'%s' does not exist in type '%s'", lineInfo, o, name());
            }
        } else {
            return info;
        }
    }

    @NotNull
    public final TypeObject tryAttrType(LineInfo lineInfo, String value, DescriptorNode access) {
        var info = attrType(value, access);
        if (info == null) {
            if (access != DescriptorNode.PRIVATE && attrType(value, DescriptorNode.PRIVATE) != null) {
                throw CompilerException.format(
                        "Cannot get attribute '%s' from type '%s': too-strict of an access level required",
                        lineInfo, value, name()
                );
            } else if (makeMut().attrType(value, access) != null) {
                throw CompilerException.format(
                        "Attribute '%s' requires a mut variable for type '%s'",
                        lineInfo, value, name()
                );
            } else {
                throw CompilerException.format(
                        "Attribute '%s' does not exist in type '%s'", lineInfo, value, name()
                );
            }
        } else {
            return info;
        }
    }

    @NotNull
    public final TypeObject[] tryOperatorReturnType(LineInfo lineInfo, OpSpTypeNode o, CompilerInfo info) {
        return tryOperatorInfo(lineInfo, o, info).getReturns();
    }

    @NotNull
    public final FunctionInfo tryOperatorInfo(LineInfo lineInfo, OpSpTypeNode o, @NotNull CompilerInfo info) {
        return tryOperatorInfo(lineInfo, o, info.accessLevel(this));
    }

    @NotNull
    public final TypeObject tryAttrType(@NotNull BaseNode node, String value, @NotNull CompilerInfo info) {
        return tryAttrType(node.getLineInfo(), value, info.accessLevel(this));
    }

    @Override
    public int compareTo(@NotNull TypeObject o) {
        return this.hashCode() - o.hashCode();
    }

    TypeObject stripNull() {
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
        } else if (sortedSet.size() == 2 && sortedSet.contains(Builtins.NULL_TYPE)) {
            sortedSet.remove(Builtins.NULL_TYPE);
            return optional(sortedSet.first());
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
