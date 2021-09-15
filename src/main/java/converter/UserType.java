package main.java.converter;

import main.java.parser.OpSpTypeNode;
import main.java.util.Pair;
import main.java.util.Zipper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

public abstract class UserType<I extends UserType.Info<?, ?>> extends NameableType {
    protected final I info;
    protected final String typedefName;
    protected final List<TypeObject> generics;
    protected final boolean isConst;

    public UserType(I info, String typedefName, boolean isConst) {
        this.info = info;
        this.typedefName = typedefName;
        this.generics = Collections.emptyList();
        this.isConst = isConst;
    }

    public UserType(I info, String typedefName, List<TypeObject> generics, boolean isConst) {
        this.info = info;
        this.typedefName = typedefName;
        this.generics = generics;
        this.isConst = isConst;
    }

    public abstract boolean isFinal();
    public abstract List<TypeObject> getSupers();
    public abstract GenericInfo getGenericInfo();
    public abstract Pair<Set<String>, Set<OpSpTypeNode>> contract();

    @Override
    public final String baseName() {
        return info.name;
    }

    public final boolean isSubclass(@NotNull TypeObject other) {
        if (this.equals(other) || this.sameBaseType(Builtins.throwsType())) {
            return true;
        } else if (other instanceof UserType && this.sameBaseType(other)) {
            if (!constSemantics() && !((UserType<?>) other).isConst && isConst) {
                return false;
            } else if (((UserType<?>) other).generics.isEmpty()) {
                return constSemantics() || ((UserType<?>) other).isConst || !isConst;
            } else if (generics.isEmpty()) {
                return false;
            } else {
                for (int i = 0; i < generics.size(); i++) {
                    if (!((UserType<?>) other).generics.get(i).isSuperclass(generics.get(i))) {
                        return false;
                    }
                }
                return true;
            }
        } else if (!isConst && this.makeConst().equals(other)) {
            return true;
        }
        for (var sup : getSupers()) {
            if (other.isSuperclass(sup)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public final Optional<TypeObject[]> operatorReturnTypeWithGenerics(OpSpTypeNode o, AccessLevel access) {
        return info.operatorReturnTypeWithGenerics(o, access);
    }

    @NotNull
    @Override
    public final Optional<TypeObject[]> operatorReturnType(OpSpTypeNode o, AccessLevel access) {
        var types = operatorReturnTypeWithGenerics(o, access);
        return types.map(this::generifyAll);
    }

    private TypeObject[] generifyAll(TypeObject[] values) {
        if (generics.isEmpty()) {
            return Arrays.copyOf(values, values.length);
        }
        TypeObject[] result = new TypeObject[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i].generifyWith(this, generics);
        }
        return result;
    }

    @Override
    @NotNull
    public final Optional<TypeObject> attrType(String value, AccessLevel access) {
        var type = attrTypeWithGenerics(value, access);
        return type.map(this::generifyAttrType);
    }

    @Override
    @NotNull
    public final Optional<TypeObject> staticAttrType(String value, AccessLevel access) {
        var type = staticAttrTypeWithGenerics(value, access);
        return type.map(this::generifyAttrType);
    }

    @NotNull
    private TypeObject generifyAttrType(TypeObject type) {
        return generics.isEmpty() ? type : type.generifyWith(this, generics);
    }

    public final void addFulfilledInterfaces() {
        assert !info.isSealed;
        var fulfilled = fulfilledInterfaces();
        if (!fulfilled.isEmpty()) {
            var result = new ArrayList<>(info.supers);
            result.addAll(fulfilledInterfaces());
            info.supers = result;
        }
    }

    public boolean constSemantics() {
        return false;
    }

    @Override
    @NotNull
    public final Optional<Map<Integer, TypeObject>> generifyAs(TypeObject parent, TypeObject other) {
        if (isSuperclass(other)) {
            return Optional.of(Collections.emptyMap());
        } else if (sameBaseType(other)) {
            return makeMatch(parent, generics, other.getGenerics());
        } else if (other instanceof UserType<?>) {
            for (var sup : ((UserType<?>) other).recursiveSupers()) {
                if (sameBaseType(sup)) {
                    var supGenerics = this.getGenerics();
                    var objGenerics = sup.getGenerics();
                    return makeMatch(parent, supGenerics, objGenerics);
                }
            }
        }
        // FIXME: Add generification of function types that implement Callable
        return Optional.empty();
    }

    private Optional<Map<Integer, TypeObject>> makeMatch(
            TypeObject parent, @NotNull List<TypeObject> supGenerics, List<TypeObject> objGenerics
    ) {
        if (supGenerics.isEmpty() && objGenerics.isEmpty()) {
            return Optional.of(Collections.emptyMap());
        } else if (supGenerics.isEmpty()) {
            return Optional.empty();
        } else if (objGenerics.isEmpty()) {
            return Optional.of(Collections.emptyMap());
        }
        assert supGenerics.size() == objGenerics.size();
        Map<Integer, TypeObject> result = new HashMap<>(supGenerics.size());
        for (var pair : Zipper.of(supGenerics, objGenerics)) {
            var supG = pair.getKey();
            var objG = pair.getValue();
            if (supG instanceof TemplateParam param) {
                if (param.getParent().sameBaseType(parent)) {
                    result.put(param.getIndex(), objG);
                } else {
                    return Optional.empty();
                }
            } else if (supG instanceof ListTypeObject && objG instanceof ListTypeObject) {
                var generics = supG.generifyAs(parent, objG);
                if (generics.isEmpty() || TypeObject.addGenericsToMap(generics.orElseThrow(), result)) {
                    return Optional.empty();
                }
            } else if (!supG.equals(objG)) {
                return Optional.empty();
            }
        }
        return Optional.of(result);
    }

    @Override
    public final boolean sameBaseType(TypeObject other) {
        if (!(other instanceof UserType<?>)) {
            return false;
        }
        return this.info == ((UserType<?>) other).info;
    }

    @Override
    public int baseHash() {
        return info.hashCode();
    }

    @Override
    public List<TypeObject> getGenerics() {
        return generics;
    }

    /**
     * Returns an iterator over all the superclasses of {@code this}, and all
     * their superclasses, etc...
     * <p>
     *     This currently assumes the type is an instance of {@link UserType},
     *     though this is probably no longer necessary and should be removed.
     * </p>
     * <p>
     *     Despite the "recursive" in the name, this is clever enough to not
     *     blow the stack in the event of deeply nested types &#8212 though it
     *     is not yet clever enough to recognise recursion and may well hang
     *     if it encounters it.
     * </p>
     *
     * @return An iterator over all the superclasses of {@code this}
     */
    @Contract(pure = true)
    @NotNull
    public final Iterable<TypeObject> recursiveSupers() {
        return () -> new RecursiveSuperIterator(this);
    }

    /**
     * The "meat" of the {@link #generifyWith} implementation for {@code
     * UserTypes}.
     * <p>
     *     This exists to be used in overloads of {@link #generifyWith}, in the
     *     manner as it is used in e.g. {@link StdTypeObject#generifyWith},
     *     because Java has no concept of {@code cls}.
     * </p>
     *
     * @param parent The parent of all changed {@code TemplateParams}
     * @param values The values to generify with
     * @return The new list of generic values
     * @see #generifyWith(TypeObject, List)
     */
    @NotNull
    protected final List<TypeObject> generifyWithInner(TypeObject parent, List<TypeObject> values) {
        if (this.sameBaseType(parent)) {
            return values;
        }
        List<TypeObject> result = new ArrayList<>(generics.size());
        for (var generic : generics) {
            if (generic instanceof TemplateParam template) {
                if (template.getParent().sameBaseType(parent)) {
                    var value = values.get(template.getIndex());
                    result.add(template.isVararg() ? TypeObject.list(value) : value);
                } else {
                    result.add(template);
                }
            } else {
                result.add(generic.generifyWith(parent, values));
            }
        }
        return result;
    }

    @NotNull
    private List<TypeObject> fulfilledInterfaces() {
        List<TypeObject> result = new ArrayList<>();
        for (var inter : ImportHandler.ALL_DEFAULT_INTERFACES.keySet()) {
            if (!isSubclass(inter) && fulfillsContract(inter)) {
                result.add(inter.generify(generifiedParams(inter)));
            }
        }
        return result;
    }

    @NotNull
    private TypeObject[] generifiedParams(@NotNull UserType<?> contractor) {
        var genericCount = contractor.info.info.size();
        if (genericCount == 0) {
            return new TypeObject[0];
        }
        var result = new TypeObject[genericCount];
        var contract = contractor.contract();
        for (var attr : contract.getKey()) {
            var attrT = attrTypeWithGenerics(attr, AccessLevel.PUBLIC).orElseThrow();
            var contractorAttr = contractor.attrTypeWithGenerics(attr, AccessLevel.PUBLIC).orElseThrow();
            for (var pair : contractorAttr.generifyAs(contractor, attrT).orElseThrow().entrySet()) {
                var index = pair.getKey();
                var val = pair.getValue();
                if (result[index] == null) {
                    result[index] = val;
                } else {
                    result[index] = TypeObject.union(result[index], val);
                }
            }
        }
        for (var op : contract.getValue()) {
            var attrT = trueOperatorInfo(op, AccessLevel.PUBLIC).orElseThrow();
            var contractorAttr = contractor.trueOperatorInfo(op, AccessLevel.PUBLIC).orElseThrow();
            var attr = contractorAttr.toCallable().generifyAs(contractor, attrT.toCallable());
            for (var pair : attr.orElseThrow().entrySet()) {
                var index = pair.getKey();
                var val = pair.getValue();
                if (result[index] == null) {
                    result[index] = val;
                } else {
                    result[index] = TypeObject.union(result[index], val);
                }
            }
        }
        assert !Arrays.asList(result).contains(null);
        return result;
    }

    @NotNull
    public Optional<TypeObject> attrTypeWithGenerics(String value, AccessLevel access) {
        // Should only be taken during auto-interface check of superclass
        // Given that, the auto interface will be applied to this type instead
        // of the superclass and thus still work
        if (info.attributes == null) {
            return Optional.empty();
        }
        var attr = info.attributes.get(value);
        if (attr == null) {
            var newAccess = access == AccessLevel.PRIVATE ? AccessLevel.PROTECTED : access;
            for (var superCls : info.supers) {
                var supAttr = superCls.attrTypeWithGenerics(value, newAccess);
                if (supAttr.isPresent() && hasImpl(value, superCls)) {
                    return supAttr;
                }
            }
            return Optional.empty();
        } else {
            return typeFromAttr(attr.intoAttrInfo(), access);
        }
    }

    public Optional<FunctionInfo> trueOperatorInfo(OpSpTypeNode o, AccessLevel access) {
        if (info.operators == null) {
            return Optional.empty();
        }
        var op = info.operators.get(o);
        if (op == null) {
            return superOperatorInfo(o, access);
        }
        var opInfo = op.intoMethodInfo();
        if (isConst && opInfo.isMut() && o != OpSpTypeNode.NEW) {
            return Optional.empty();
        } else if (AccessLevel.canAccess(opInfo.getAccessLevel(), access)) {
            return Optional.of(opInfo.getInfo());
        } else {
            return superOperatorInfo(o, access);
        }
    }

    private Optional<FunctionInfo> superOperatorInfo(OpSpTypeNode o, AccessLevel access) {
        var newAccess = access == AccessLevel.PRIVATE ? AccessLevel.PROTECTED : access;
        for (var superCls : info.supers) {
            var supAttr = superCls.trueOperatorInfo(o, newAccess);
            if (supAttr.isPresent() && hasImpl(o, superCls)) {
                return supAttr;
            }
        }
        return new ObjectType().operatorInfo(o, access);
    }

    private boolean hasImpl(String value, TypeObject superCls) {
        if (superCls instanceof UserType) {
            return !((UserType<?>) superCls).contract().getKey().contains(value);
        } else {
            return true;
        }
    }

    private boolean hasImpl(OpSpTypeNode value, TypeObject superCls) {
        if (superCls instanceof UserType) {
            return !((UserType<?>) superCls).contract().getValue().contains(value);
        } else {
            return true;
        }
    }

    @NotNull
    @Override
    public Optional<TypeObject> staticAttrTypeWithGenerics(String value, AccessLevel access) {
        var attr = info.staticAttributes.get(value);
        if (attr == null && info.info.getParamMap().containsKey(value)) {
            return Optional.of(info.info.getParamMap().get(value).getType());
        }
        return typeFromAttr(attr == null ? null : attr.intoAttrInfo(), access);
    }

    private Optional<TypeObject> typeFromAttr(AttributeInfo attr, AccessLevel access) {
        if (attr == null) {
            return Optional.empty();
        }
        if (attr.getMutType() == MutableType.MUT_METHOD) {
            return isConst ? Optional.empty() : Optional.of(attr.getType());
        } else if (isConst) {
            return AccessLevel.canAccess(attr.getAccessLevel(), access)
                    ? Optional.of(attr.getType().makeConst()) : Optional.empty();
        } else if (attr.getAccessLevel() == AccessLevel.PUBGET) {
            return Optional.of(AccessLevel.canAccess(AccessLevel.PRIVATE, access)
                    ? attr.getType().makeMut() : attr.getType().makeConst());
        } else if (AccessLevel.canAccess(attr.getAccessLevel(), access)) {
            if (attr.getMutType().isConstType()) {
                return Optional.of(attr.getType().makeConst());
            } else {
                return Optional.of(attr.getType().makeMut());
            }
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserType<?> userType = (UserType<?>) o;
        return isConst == userType.isConst &&
                Objects.equals(info, userType.info) &&
                Objects.equals(typedefName, userType.typedefName) &&
                Objects.equals(generics, userType.generics);
    }

    @Override
    public int hashCode() {
        return Objects.hash(info, typedefName, generics, isConst);
    }

    @Override
    public final boolean canSetAttr(String name, AccessLevel access) {
        if (isConst) {
            return false;
        }
        var attr = info.attributes.get(name);
        if (attr == null) {
            return false;
        } else {
            var attrInfo = attr.intoAttrInfo();
            var accessLevel = attrInfo.getAccessLevel();
            if (!AccessLevel.canAccess(accessLevel, access)) {
                return false;
            } else if (accessLevel == AccessLevel.PUBGET) {
                return AccessLevel.canAccess(AccessLevel.PRIVATE, access);
            } else {
                return !attr.intoAttrInfo().getMutType().isConstRef();
            }
        }
    }

    @Override
    public Optional<Iterable<String>> getDefined() {
        return Optional.of(DefinedIterator::new);
    }

    @Override
    public Optional<Iterable<String>> staticDefined() {
        return Optional.of(StaticIterator::new);
    }

    public Iterable<String> getFields() {
        return FieldIterator::new;
    }

    protected static String stdName(
            String baseName, List<TypeObject> generics, boolean isConst,
            String typedefName, boolean isConstClass
    ) {
        if (generics.isEmpty()) {
            String name = typedefName.isEmpty() ? baseName : typedefName;
            if (isConst || isConstClass) {
                return name;
            } else {
                return String.format("mut %s", name);
            }
        } else {
            var valueJoiner = new StringJoiner(", ", "[", "]");
            for (var cls : generics) {
                valueJoiner.add(cls.name());
            }
            if (isConst || isConstClass) {
                return baseName + valueJoiner;
            } else {
                return String.format("mut %s%s", baseName, valueJoiner);
            }
        }
    }

    protected static abstract class Info<O extends IntoMethodInfo, A extends IntoAttrInfo> {
        protected final String name;
        protected List<TypeObject> supers;
        protected Map<OpSpTypeNode, O> operators;
        protected Map<OpSpTypeNode, O> staticOperators;
        protected final GenericInfo info;
        protected Map<String, A> attributes;
        protected Map<String, A> staticAttributes;
        protected boolean isSealed;

        public Info(String name, List<TypeObject> supers, GenericInfo info) {
            this.name = name;
            this.supers = Collections.unmodifiableList(supers);
            this.operators = new EnumMap<>(OpSpTypeNode.class);
            this.staticOperators = new EnumMap<>(OpSpTypeNode.class);
            this.info = info;
            this.isSealed = false;
        }

        @NotNull
        public final Optional<TypeObject[]> operatorReturnTypeWithGenerics(OpSpTypeNode o, AccessLevel access) {
            if (operators.containsKey(o)) {
                var op = operators.get(o).intoMethodInfo();
                if (AccessLevel.canAccess(op.getAccessLevel(), access)) {
                    return Optional.of(op.getReturns());
                } else {
                    return Optional.empty();
                }
            }
            for (var sup : supers) {
                var opRet = sup.operatorReturnType(o, access);
                if (opRet.isPresent()) {
                    return opRet;
                }
            }
            return Optional.empty();
        }

        @NotNull
        public final Optional<TypeObject[]> staticOperatorReturnType(OpSpTypeNode o) {
            if (staticOperators.containsKey(o)) {
                return Optional.of(staticOperators.get(o).intoMethodInfo().getReturns());
            }
            for (var sup : supers) {
                var opRet = sup.staticOperatorReturnType(o);
                if (opRet.isPresent()) {
                    return opRet;
                }
            }
            return Optional.empty();
        }

        public final void seal() {
            assert !isSealed : String.format("Class %s sealed twice", name);
            isSealed = true;
            if (operators == null) {
                operators = Collections.emptyMap();
            }
            if (staticOperators == null) {
                staticOperators = Collections.emptyMap();
            }
            if (attributes == null) {
                attributes = Collections.emptyMap();
            }
            if (staticAttributes == null) {
                staticAttributes = Collections.emptyMap();
            }
        }
    }

    private final class DefinedIterator implements Iterator<String> {
        private int superIndex = -1;
        private Iterator<String> currentIter = info.attributes.keySet().iterator();

        @Override
        public boolean hasNext() {
            if (!currentIter.hasNext()) updateIter();
            return superIndex < info.supers.size();
        }

        @Override
        public String next() {
            if (!currentIter.hasNext()) {
                updateIter();
                if (superIndex >= info.supers.size()) {
                    throw new NoSuchElementException();
                }
            }
            return currentIter.next();
        }

        private void updateIter() {
            while (!currentIter.hasNext() && superIndex < info.supers.size() - 1) {
                superIndex++;
                currentIter = info.supers.get(superIndex).getDefined().orElse(Collections.emptyList()).iterator();
            }
            if (!currentIter.hasNext()) {
                superIndex++;
            }
        }
    }

    private final class StaticIterator implements Iterator<String> {
        private int superIndex = -1;
        private Iterator<String> currentIter = info.staticAttributes.keySet().iterator();

        @Override
        public boolean hasNext() {
            if (!currentIter.hasNext()) updateIter();
            return superIndex < info.supers.size();
        }

        @Override
        public String next() {
            if (!currentIter.hasNext()) {
                updateIter();
                if (superIndex >= info.supers.size()) {
                    throw new NoSuchElementException();
                }
            }
            return currentIter.next();
        }

        private void updateIter() {
            while (!currentIter.hasNext() && superIndex < info.supers.size() - 1) {
                superIndex++;
                currentIter = info.supers.get(superIndex).staticDefined().orElse(Collections.emptyList()).iterator();
            }
            if (!currentIter.hasNext()) {
                superIndex++;
            }
        }
    }

    private final class FieldIterator implements Iterator<String> {
        private final Iterator<? extends Map.Entry<String, ? extends IntoAttrInfo>>
                values = info.attributes.entrySet().iterator();
        @Nullable
        private String next = null;

        @Override
        public boolean hasNext() {
            if (next != null) {
                return true;
            }
            while (values.hasNext()) {
                var entry = values.next();
                if (!entry.getValue().intoAttrInfo().isMethod()) {
                    next = entry.getKey();
                    return true;
                }
            }
            return false;
        }

        @Override
        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            } else {
                assert this.next != null;
                var next = this.next;
                this.next = null;
                return next;
            }
        }
    }

    private static final class RecursiveSuperIterator implements Iterator<TypeObject> {
        private final Deque<Iterator<TypeObject>> iterators;

        public RecursiveSuperIterator(@NotNull UserType<?> val) {
            this.iterators = new ArrayDeque<>();
            iterators.addLast(val.getSupers().iterator());
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
}
