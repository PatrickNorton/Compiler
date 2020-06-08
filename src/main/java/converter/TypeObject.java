package main.java.converter;

import main.java.parser.BaseNode;
import main.java.parser.DescriptorNode;
import main.java.parser.IndexNode;
import main.java.parser.LineInfo;
import main.java.parser.OpSpTypeNode;
import main.java.parser.OperatorTypeNode;
import main.java.parser.TestNode;
import main.java.parser.VariableNode;
import main.java.util.Zipper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public abstract class TypeObject implements LangObject, Comparable<TypeObject> {
    /**
     * Checks if this is a subclass of the other type.
     * <p>
     *     <b>IMPORTANT:</b> This should not be called outside of {@link
     *     #isSuperclass} (as a default implementation).
     * </p>
     *
     * @param other The type to test for inheritance
     * @return If this is a subtype
     * @see #isSuperclass
     */
    protected abstract boolean isSubclass(@NotNull TypeObject other);
    public abstract String name();
    public abstract TypeObject typedefAs(String name);

    /**
     * Checks if this is a superclass of another type.
     * <p>
     *     Type T being a supertype of type U means that the code {@code T x =
     *     y} is valid (where y is of type U).
     * </p>
     *
     * @param other The type to test for inheritance
     * @return If this is a supertype
     */
    public boolean isSuperclass(@NotNull TypeObject other) {
        return other.isSubclass(this);
    }

    public TypeObject makeConst() {
        return this;
    }

    public TypeObject makeMut() {
        return this;
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

    @Nullable
    public TypeObject staticAttrType(String value, DescriptorNode access) {
        return null;
    }

    public TypeObject[] staticOperatorReturnType(OpSpTypeNode o) {
        return null;
    }

    public Map<Integer, TypeObject> generifyAs(TypeObject other) {
        return new HashMap<>();
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
    public final TypeObject tryStaticAttrType(LineInfo lineInfo, String value, DescriptorNode access) {
        var info = staticAttrType(value, access);
        if (info == null) {
            if (access != DescriptorNode.PRIVATE && staticAttrType(value, DescriptorNode.PRIVATE) != null) {
                throw CompilerException.format(
                        "Cannot get static attribute '%s' from type '%s':" +
                                " too-strict of an access level required",
                        lineInfo, value, name()
                );
            } else if (makeMut().staticAttrType(value, access) != null) {
                throw CompilerException.format(
                        "Static attribute '%s' requires a mut variable for type '%s'",
                        lineInfo, value, name()
                );
            } else {
                throw CompilerException.format(
                        "Static attribute '%s' does not exist in type '%s'", lineInfo, value, name()
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

    public TypeObject attrTypeWithGenerics(String value, DescriptorNode access) {
        return attrType(value, access);
    }

    public TypeObject staticAttrTypeWithGenerics(String value, DescriptorNode access) {
        return staticAttrType(value, access);
    }

    public FunctionInfo trueOperatorInfo(OpSpTypeNode o, DescriptorNode access) {
        return operatorInfo(o, access);
    }

    @Override
    public int compareTo(@NotNull TypeObject o) {
        return this.hashCode() - o.hashCode();
    }

    TypeObject stripNull() {
        return this;
    }

    static TypeObject union(@NotNull TypeObject... values) {
        Set<TypeObject> valueSet = new HashSet<>(Arrays.asList(values));
        if (valueSet.size() == 1) {
            return valueSet.iterator().next();
        } else {
            valueSet.remove(Builtins.THROWS);
            TypeObject currentSuper = null;
            boolean isOptional = false;
            for (var value : valueSet) {
                if (value instanceof OptionTypeObject) {
                    isOptional = true;
                    var option = ((OptionTypeObject) value).getOptionVal();
                    currentSuper = currentSuper == null ? option : getSuper(currentSuper, option);
                } else {
                    currentSuper = currentSuper == null ? value : getSuper(currentSuper, value);
                }
            }
            return isOptional ? new OptionTypeObject(currentSuper) : currentSuper;
        }
    }

    @NotNull
    private static TypeObject getSuper(@NotNull TypeObject a, TypeObject b) {
        if (a.isSuperclass(b)) {
            return a;
        } else if (b.isSuperclass(a)) {
            return b;
        }
        assert a instanceof UserType && b instanceof UserType;
        var userA = (UserType<?>) a;
        var userB = (UserType<?>) b;
        Set<TypeObject> aSupers = new HashSet<>();
        Set<TypeObject> bSupers = new HashSet<>();
        for (var pair : new Zipper<>(new RecursiveSuperIterator(userA), new RecursiveSuperIterator(userB))) {
            if (bSupers.contains(pair.getKey())) {
                return pair.getKey();
            } else if (aSupers.contains(pair.getValue())) {
                return pair.getValue();
            } else {
                aSupers.add(pair.getKey());
                bSupers.add(pair.getValue());
            }
        }
        return Builtins.OBJECT;
    }

    private static final class RecursiveSuperIterator implements Iterator<TypeObject>, Iterable<TypeObject> {
        private final Deque<Iterator<TypeObject>> iterators;

        public RecursiveSuperIterator(@NotNull UserType<?> val) {
            this.iterators = new ArrayDeque<>();
            iterators.addLast(val.getSupers().iterator());
        }

        @NotNull
        @Override
        public Iterator<TypeObject> iterator() {
            return this;
        }

        public boolean hasNext() {
            while (!iterators.isEmpty() && !iterators.peek().hasNext()) {
                iterators.pop();
            }
            return !iterators.isEmpty();
        }

        @Override
        public TypeObject next() {
            while (!iterators.isEmpty() && !iterators.peek().hasNext()) {
                iterators.pop();
            }
            if (iterators.isEmpty()) {
                throw new NoSuchElementException();
            } else {
                var value = iterators.peek().next();
                List<TypeObject> supers = value instanceof UserType<?>
                        ? ((UserType<?>) value).getSupers() : Collections.emptyList();
                if (!supers.isEmpty()) {
                    iterators.addLast(supers.iterator());
                }
                return value;
            }
        }
    }

    @NotNull
    @Contract(pure = true)
    static TypeObject optional(@NotNull TypeObject value) {
        return value instanceof OptionTypeObject ? value : new OptionTypeObject(value);
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
