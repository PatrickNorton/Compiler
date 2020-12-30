package main.java.converter;

import main.java.parser.IndexNode;
import main.java.parser.LineInfo;
import main.java.parser.Lined;
import main.java.parser.OpSpTypeNode;
import main.java.parser.OperatorTypeNode;
import main.java.parser.TestNode;
import main.java.parser.VariableNode;
import main.java.util.Levenshtein;
import main.java.util.Zipper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public abstract String baseName();
    public abstract boolean sameBaseType(TypeObject other);
    public abstract int baseHash();
    public abstract TypeObject typedefAs(String name);

    public Optional<Iterable<String>> getDefined() {
        return Optional.empty();
    }

    public Optional<Iterable<String>> staticDefined() {
        return Optional.empty();
    }

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

    /**
     * Returns whether or not calling {@link #isSuperclass} will delegate to
     * <code>other.{@link #isSubclass isSubclass}(this)</code>.
     *
     * @implNote This method should be overridden iff {@link #isSuperclass} is
     *           overridden, and have the body {@code return false}, and
     *           nothing else
     * @return If the recursion will occur
     * @see #isSuperclass
     * @see #isSubclass
     */
    public boolean willSuperRecurse() {
        return true;
    }

    /**
     * Returns the made-{@code const} version of this type, or {@code this} if
     * it is already const.
     *
     * @return The const-made type
     * @see #makeMut()
     */
    public TypeObject makeConst() {
        return this;
    }

    /**
     * Returns the made-{@code mut} version of this type, or {@code this} if
     * it is already mut.
     *
     * @return The mut-made type
     * @see #makeConst()
     */
    public TypeObject makeMut() {
        return this;
    }

    /**
     * Whether or not it is legal to assign to the given attribute of this type.
     *
     * @param name The name of the attribute to be assigned to
     * @param access The level of access permitted to the assignee
     * @return If the assignment is legal or not
     * @see #canSetAttr(String, CompilerInfo)
     */
    public boolean canSetAttr(String name, AccessLevel access) {
        return false;
    }

    /**
     * Whether or not it is legal to assign to the given attribute of this type.
     *
     * @param name The name of the attribute to be assigned to
     * @param info The {@link CompilerInfo} from which to get the permitted
     *             access level
     * @return If the assignment is legal or not
     * @see #canSetAttr(String, AccessLevel)
     */
    public final boolean canSetAttr(String name, @NotNull CompilerInfo info) {
        return canSetAttr(name, info.accessLevel(this));
    }

    @Override
    public final TypeObject getType() {
        return Builtins.TYPE.generify(this);
    }

    /**
     * Gets the {@link FunctionInfo} for a given operator on this type.
     * <p>
     *     This method returns {@link Optional#empty()} if either the operator
     *     is not defined for this type or the access level given is not strict
     *     enough to access the value. For a method that unwraps the {@link
     *     Optional} and gives standardized error messages, see {@link
     *     #tryOperatorInfo(LineInfo, OpSpTypeNode, AccessLevel)}.
     * </p>
     *
     * @param o The operator to get the info for
     * @param access The access level requested
     * @return The information for calling the operator, if accessible
     * @see #operatorInfo(OpSpTypeNode, CompilerInfo)
     * @see #tryOperatorInfo(LineInfo, OpSpTypeNode, AccessLevel)
     */
    public Optional<FunctionInfo> operatorInfo(OpSpTypeNode o, AccessLevel access) {
        return Optional.empty();
    }

    /**
     * Gets the {@link FunctionInfo} for a given operator on this type.
     * <p>
     *     This method returns {@link Optional#empty()} if either the operator
     *     is not defined for this type or the access level given is not strict
     *     enough to access the value. For a method that unwraps the {@link
     *     Optional} and gives standardized error messages, see {@link
     *     #tryOperatorInfo(LineInfo, OpSpTypeNode, CompilerInfo)}.
     * </p>
     *
     * @param o The operator to get the info for
     * @param info The {@link CompilerInfo} containing the access level for the
     *             type
     * @return The information for calling the operator, if accessible
     * @see #operatorInfo(OpSpTypeNode, AccessLevel)
     * @see #tryOperatorInfo(LineInfo, OpSpTypeNode, CompilerInfo)
     */
    public Optional<FunctionInfo> operatorInfo(OpSpTypeNode o, @NotNull CompilerInfo info) {
        return operatorInfo(o, info.accessLevel(this));
    }

    /**
     * Gets the return type given by calling a given operator on this type.
     * <p>
     *     This method returns {@link Optional#empty()} if {@link
     *     #operatorInfo(OpSpTypeNode, CompilerInfo) #operatorInfo(o, info)}
     *     would return {@link Optional#empty()}. If the operator has no
     *     returns, this method will return a zero-length array. For a method
     *     that unwraps the {@link Optional} and gives standardized error
     *     messages, see {@link
     *     #tryOperatorReturnType(Lined, OpSpTypeNode, CompilerInfo)}.
     * </p>
     *
     * @param o The operator to {@link OpSpTypeNode#translate translate} and
     *          get the return type for
     * @param info The {@link CompilerInfo} containing the access level for
     *             this type
     * @return The return type of the operator
     * @see #tryOperatorReturnType(Lined, OpSpTypeNode, CompilerInfo)
     * @see #operatorReturnType(OpSpTypeNode, AccessLevel)
     */
    public final Optional<TypeObject[]> operatorReturnType(OperatorTypeNode o, @NotNull CompilerInfo info) {
        return operatorReturnType(OpSpTypeNode.translate(o), info.accessLevel(this));
    }

    /**
     * Gets the return type given by calling a given operator on this type.
     * <p>
     *     This method returns {@link Optional#empty()} if {@link
     *     #operatorInfo(OpSpTypeNode, AccessLevel) #operatorInfo(o, access)}
     *     would return {@link Optional#empty()}. If the operator has no
     *     returns, this method will return a zero-length array. For a method
     *     that unwraps the {@link Optional} and gives standardized error
     *     messages, see {@link
     *     #tryOperatorReturnType(Lined, OpSpTypeNode, CompilerInfo)}.
     * </p>
     *
     * @param o The operator to {@link OpSpTypeNode#translate translate} and
     *          get the return type for
     * @param access The access level the calling code has
     * @return The return type of the operator
     * @see #tryOperatorReturnType(Lined, OpSpTypeNode, CompilerInfo)
     * @see #operatorReturnType(OpSpTypeNode, AccessLevel)
     */
    public Optional<TypeObject[]> operatorReturnType(OpSpTypeNode o, AccessLevel access) {
        var info = operatorInfo(o, access);
        return info.map(FunctionInfo::getReturns);
    }

    /**
     * Generifies the type with the given arguments.
     * <p>
     *     For most circumstances, using {@link #generify(Lined, TypeObject...)}
     *     or {@link #generify(LineInfo, TypeObject...)} is preferable, as it
     *     will give more descriptive messages on an error, while this will use
     *     {@link LineInfo#empty()}. This should really only be used where all
     *     parameters are known, such as in {@link Builtins}.
     * </p>
     *
     * @param args The arguments for generification
     * @return The generified type
     * @see #generify(Lined, TypeObject...)
     * @see #generify(LineInfo, TypeObject...)
     */
    public final TypeObject generify(TypeObject... args) {
        return generify(LineInfo.empty(), args);
    }

    /**
     * Generifies the type with the given arguments.
     *
     * @param lineInfo The object to turn into {@link LineInfo} in an error
     * @param args The arguments for generification
     * @return The generified type
     * @see #generify(LineInfo, TypeObject...)
     */
    public final TypeObject generify(@NotNull Lined lineInfo, TypeObject... args) {
        return generify(lineInfo.getLineInfo(), args);
    }

    /**
     * Generifies the type with the given arguments.
     *
     * @param lineInfo The information to use in case of an error
     * @param args The arguments for generification
     * @return The generified type
     * @see #generify(Lined, TypeObject...)
     */
    public TypeObject generify(LineInfo lineInfo, TypeObject... args) {
        throw CompilerException.of("Cannot generify object", lineInfo);
    }

    /**
     * Gets the type of a given attribute of {@code this}.
     * <p>
     *     This method returns {@link Optional#empty()} if either the attribute
     *     is not defined for the type or the access level given is not strict
     *     enough. For a method that unwraps the value and gives custom error
     *     messages, see {@link #tryAttrType(LineInfo, String, AccessLevel)}. If
     *     attempting to assign to the attribute, use {@link
     *     #canSetAttr(String, AccessLevel)} to check validity.
     * </p>
     *
     * @param value The name of the attribute to access
     * @param access The access level with which to access the code
     * @return The type of the attribute
     * @see #tryAttrType(LineInfo, String, AccessLevel)
     * @see #canSetAttr(String, AccessLevel)
     */
    @NotNull
    public Optional<TypeObject> attrType(String value, AccessLevel access) {
        return Optional.empty();
    }

    /**
     * Gets the type of a given static attribute of {@code this}.
     * <p>
     *     This method returns {@link Optional#empty()} if either the attribute
     *     is not defined for the type or the access level given is not strict
     *     enough. For a method that unwraps the value and gives custom error
     *     messages, see {@link
     *     #tryStaticAttrType(LineInfo, String, AccessLevel)}.
     * </p>
     *
     * @param value The name of the attribute to access
     * @param access The access level with which to access the code
     * @return The type of the attribute
     * @see #tryStaticAttrType(LineInfo, String, AccessLevel)
     */
    @NotNull
    public Optional<TypeObject> staticAttrType(String value, AccessLevel access) {
        return Optional.empty();
    }

    /**
     * Gets the return type of a static operator of {@code this}.
     *
     * @param o The operator to get the return type of
     * @return The return type of the operator
     */
    public Optional<TypeObject[]> staticOperatorReturnType(OpSpTypeNode o) {
        return Optional.empty();
    }

    /**
     * Returns the generics needed to transform one type into another.
     * <p>
     *     The purpose of this is for more complex generic transformations, e.g.
     *     finding the correct {@code T} such that {@code list[int] instanceof
     *     Iterable[T]}. The {@code parent} parameter is the parent of all
     *     {@link TemplateParam}s which are to be included in the
     *     transformation.
     * </p>
     * <p>
     *     If it is impossible for this class to ever be a subclass of the given
     *     class, or it is impossible while only changing {@link TemplateParam}s
     *     with the given parent, {@link Optional#empty()} will be returned.
     * </p>
     * <p>
     *     If this type contains enough information to generify the parent
     *     completely, it will be possible to transform the returned map into
     *     a list with no empty indices. However, for parents with complex
     *     generics, it may not be. If all information is given, then it will
     *     be the case that, given
     * <pre>
     * <code>var params = transform(this.generifyAs(parent, other))</code>
     * </pre>
     *     where {@code transform} is the function turning the returned
     *     {@link Map} into a {@link List}, then
     * <pre><code>
     * this.{@link #generifyWith(TypeObject, List) generifyWith}(parent, params)
     *      .{@link #isSuperclass isSuperclass}(other.{@link #generifyWith(TypeObject, List)
     *      generifyWith}(parent, params))
     * </code></pre>
     *     must always be {@code true}.
     * </p>
     *
     * @param parent The parent of all changed {@link TemplateParam
     *               TemplateParams}
     * @param other The object to attempt to transform into
     * @return The parameters which must be changed to properly transform the
     *         type
     * @see #generifyWith(TypeObject, List)
     */
    public Optional<Map<Integer, TypeObject>> generifyAs(TypeObject parent, TypeObject other) {
        return Optional.empty();
    }

    /**
     * Gets the generics associated with this type.
     * <p>
     *     For types without varargs, this should be in a one-to-one
     *     correspondence with {@link #generify(TypeObject...)}, e.g.
     * <pre><code>
     *     TypeObject[] generics = ...
     *     var generified = something.generify(generics)
     *     return generified.getGenerics().equals(List.of(generics))
     * </code></pre>
     *     should always return {@code true} if {@code something} has no
     *     varargs (and if no errors occur).
     * </p>
     *
     * @return The list of generics for the type
     * @see #generify(TypeObject...)
     * @see #generify(LineInfo, TypeObject...)
     * @see #generify(Lined, TypeObject...)
     */
    public List<TypeObject> getGenerics() {
        return Collections.emptyList();
    }

    /**
     * Generifies the {@link TemplateParam template parameters} of another type,
     * replacing them with the new objects given in the list.
     * <p>
     *     This is a relatively niche method, which should probably only be
     *     called as part of the process of {@link
     *     #generify(LineInfo, TypeObject...) #generify} or similar methods.
     * </p>
     *
     * @param parent The parent to change the parameters of
     * @param values The list of new values to change them to
     * @return The newly generified type
     */
    public TypeObject generifyWith(TypeObject parent, List<TypeObject> values) {
        return this;
    }

    /**
     * Attempts to get the {@link #operatorInfo(OpSpTypeNode, AccessLevel)
     * operator info} of a type, or throws an error with a descriptive message
     * on failure.
     * <p>
     *     This method will throw a {@link CompilerException} iff this.{@link
     *     #operatorInfo(OpSpTypeNode, AccessLevel) operatorInfo(o, access)}
     *     would return {@link Optional#empty()}.
     * </p>
     *
     * @param lineInfo The line info representing the location attempting to
     *                 access the operator
     * @param o The type of operator to access
     * @param access The level of access granted
     * @return The information representing the operator
     * @see #operatorInfo(OpSpTypeNode, AccessLevel)
     * @see #tryOperatorInfo(Lined, OpSpTypeNode, CompilerInfo)
     */
    @NotNull
    public final FunctionInfo tryOperatorInfo(LineInfo lineInfo, OpSpTypeNode o, AccessLevel access) {
        var info = operatorInfo(o, access);
        return info.orElseThrow(() -> opInfoException(lineInfo, o, access));
    }

    @NotNull
    private CompilerException opInfoException(LineInfo lineInfo, OpSpTypeNode o, AccessLevel access) {
        if (access != AccessLevel.PRIVATE && operatorInfo(o, AccessLevel.PRIVATE).isPresent()) {
            return CompilerException.format(
                    "Cannot get '%s' from type '%s': operator has a too-strict access level",
                    lineInfo, o, name()
            );
        } else if (makeMut().operatorInfo(o, access).isPresent()) {
            return CompilerException.format(
                    "'%s' requires a mut variable for type '%s'",
                    lineInfo, o, name()
            );
        } else {
            return CompilerException.format("'%s' does not exist in type '%s'", lineInfo, o, name());
        }
    }

    /**
     * Attempts to get the {@link #attrType(String, AccessLevel)) attribute
     * type} of a type, or throws an error with a descriptive message on
     * failure.
     * <p>
     *     This method will throw a {@link CompilerException} iff this.{@link
     *     #attrType(String, AccessLevel) attrType(value, access)} would return
     *     {@link Optional#empty()}.
     * </p>
     *
     * @param lineInfo The line info representing the location attempting to
     *                 access the operator
     * @param value The name of the accessed attribute
     * @param access The level of access granted
     * @return The information representing the operator
     * @see #attrType(String, AccessLevel)
     * @see #tryAttrType(LineInfo, String, AccessLevel)
     * @implNote This is not {@code final} because {@link TypeTypeObject} has a
     *           better version for itself
     */
    @NotNull
    public TypeObject tryAttrType(LineInfo lineInfo, String value, AccessLevel access) {
        var info = attrType(value, access);
        return info.orElseThrow(() -> attrException(lineInfo, value, access));
    }

    @NotNull
    private CompilerException attrException(LineInfo lineInfo, String value, AccessLevel access) {
        if (access != AccessLevel.PRIVATE && attrType(value, AccessLevel.PRIVATE).isPresent()) {
            return CompilerException.format(
                    "Cannot get attribute '%s' from type '%s': too strict of an access level required",
                    lineInfo, value, name()
            );
        } else if (makeMut().attrType(value, access).isPresent()) {
            return CompilerException.format(
                    "Attribute '%s' requires a mut variable for type '%s'",
                    lineInfo, value, name()
            );
        } else {
            var closest = getDefined().flatMap(x -> Levenshtein.closestName(value, x));
            if (closest.isPresent()) {
                return CompilerException.format(
                        "Attribute '%s' does not exist in type '%s'%nDid you mean '%s'?",
                        lineInfo, value, name(), closest.orElseThrow()
                );
            } else {
                return CompilerException.format(
                        "Attribute '%s' does not exist in type '%s'", lineInfo, value, name()
                );
            }
        }
    }

    /**
     * Attempts to get the {@link #staticAttrType(String, AccessLevel) static
     * attribute type} of a type, or throws an error with a descriptive message
     * on failure.
     * <p>
     *     This method will throw a {@link CompilerException} iff this.{@link
     *     #staticAttrType(String, AccessLevel) attrType(value, access)} would
     *     return {@link Optional#empty()}.
     * </p>
     *
     * @param lineInfo The line info representing the location attempting to
     *                 access the operator
     * @param value The name of the accessed attribute
     * @param access The level of access granted
     * @return The information representing the operator
     * @see #staticAttrType(String, AccessLevel)
     */
    @NotNull
    public final TypeObject tryStaticAttrType(LineInfo lineInfo, String value, AccessLevel access) {
        var info = staticAttrType(value, access);
        return info.orElseThrow(() -> staticAttrException(lineInfo, value, access));
    }

    @NotNull
    private CompilerException staticAttrException(LineInfo lineInfo, String value, AccessLevel access) {
        if (access != AccessLevel.PRIVATE && staticAttrType(value, AccessLevel.PRIVATE).isPresent()) {
            return CompilerException.format(
                    "Cannot get static attribute '%s' from type '%s':" +
                            " too-strict of an access level required",
                    lineInfo, value, name()
            );
        } else if (makeMut().staticAttrType(value, access).isPresent()) {
            return CompilerException.format(
                    "Static attribute '%s' requires a mut variable for type '%s'",
                    lineInfo, value, name()
            );
        } else {
            var closest = staticDefined().flatMap(x -> Levenshtein.closestName(value, x));
            if (closest.isPresent()) {
                return CompilerException.format(
                        "Static attribute '%s' does not exist in type '%s'%nDid you mean '%s'?",
                        lineInfo, value, name(), closest.orElseThrow()
                );
            } else {
                return CompilerException.format(
                        "Static attribute '%s' does not exist in type '%s'", lineInfo, value, name()
                );
            }
        }
    }

    /**
     * Attempts to get the {@link
     * #operatorReturnType(OpSpTypeNode, AccessLevel) operator return type} of
     * a type, or throws an error with a descriptive message on failure.
     * <p>
     *     This method will throw a {@link CompilerException} iff this.{@link
     *     #operatorReturnType(OpSpTypeNode, AccessLevel)
     *     operatorReturnType(o, info)} would return {@link Optional#empty()}.
     * </p>
     *
     * @param lineInfo The line info representing the location attempting to
     *                 access the operator
     * @param o The type of operator to access
     * @param info The {@link CompilerInfo} containing the access level for
     *             {@code this}
     * @return The information representing the operator
     * @see #operatorReturnType(OpSpTypeNode, AccessLevel)
     * @see #tryOperatorReturnType(Lined, OpSpTypeNode, CompilerInfo)
     */
    @NotNull
    public final TypeObject[] tryOperatorReturnType(LineInfo lineInfo, OpSpTypeNode o, CompilerInfo info) {
        return tryOperatorInfo(lineInfo, o, info).getReturns();
    }

    /**
     * Attempts to get the {@link
     * #operatorReturnType(OpSpTypeNode, AccessLevel) operator return type} of
     * a type, or throws an error with a descriptive message on failure.
     * <p>
     *     This method will throw a {@link CompilerException} iff this.{@link
     *     #operatorReturnType(OpSpTypeNode, AccessLevel)
     *     operatorReturnType(o, info)} would return {@link Optional#empty()}.
     * </p>
     *
     * @param lined The object containing the {@link LineInfo} to use in an
     *              error
     * @param o The type of operator to access
     * @param info The {@link CompilerInfo} containing the access level for
     *             {@code this}
     * @return The information representing the operator
     * @see #operatorReturnType(OpSpTypeNode, AccessLevel)
     * @see #tryOperatorReturnType(LineInfo, OpSpTypeNode, CompilerInfo)
     */
    public final TypeObject[] tryOperatorReturnType(@NotNull Lined lined, OpSpTypeNode o, CompilerInfo info) {
        return tryOperatorReturnType(lined.getLineInfo(), o, info);
    }

    /**
     * Attempts to get the {@link #operatorInfo(OpSpTypeNode, CompilerInfo)
     * operator info} of a type, or throws an error with a descriptive message
     * on failure.
     * <p>
     *     This method will throw a {@link CompilerException} iff this.{@link
     *     #operatorInfo(OpSpTypeNode, CompilerInfo) operatorInfo(o, info)}
     *     would return {@link Optional#empty()}.
     * </p>
     *
     * @param lineInfo The line info representing the location attempting to
     *                 access the operator
     * @param o The type of operator to access
     * @param info The {@link CompilerInfo} containing the access level for
     *             {@code this}
     * @return The information representing the operator
     * @see #operatorInfo(OpSpTypeNode, AccessLevel)
     * @see #tryOperatorInfo(LineInfo, OpSpTypeNode, AccessLevel)
     */
    @NotNull
    public final FunctionInfo tryOperatorInfo(LineInfo lineInfo, OpSpTypeNode o, @NotNull CompilerInfo info) {
        return tryOperatorInfo(lineInfo, o, info.accessLevel(this));
    }

    /**
     * Attempts to get the {@link #operatorInfo(OpSpTypeNode, CompilerInfo)
     * operator info} of a type, or throws an error with a descriptive message
     * on failure.
     * <p>
     *     This method will throw a {@link CompilerException} iff this.{@link
     *     #operatorInfo(OpSpTypeNode, CompilerInfo) operatorInfo(o, info)}
     *     would return {@link Optional#empty()}.
     * </p>
     *
     * @param lined The object containing the {@link LineInfo} to use in an
     *              error
     * @param o The type of operator to access
     * @param info The {@link CompilerInfo} containing the access level for
     *             {@code this}
     * @return The information representing the operator
     * @see #operatorInfo(OpSpTypeNode, AccessLevel)
     * @see #tryOperatorInfo(Lined, OpSpTypeNode, CompilerInfo)
     */
    @NotNull
    public final FunctionInfo tryOperatorInfo(@NotNull Lined lined, OpSpTypeNode o, @NotNull CompilerInfo info) {
        return tryOperatorInfo(lined.getLineInfo(), o, info.accessLevel(this));
    }

    /**
     * Attempts to get the {@link #attrType(String, AccessLevel)) attribute
     * type} of a type, or throws an error with a descriptive message on
     * failure.
     * <p>
     *     This method will throw a {@link CompilerException} iff this.{@link
     *     #attrType(String, AccessLevel) attrType(value, access)} would return
     *     {@link Optional#empty()}.
     * </p>
     *
     * @param node The object containing the {@link LineInfo} to use in an error
     * @param value The name of the accessed attribute
     * @param info The {@link CompilerInfo} containing the access level for
     *             {@code this}
     * @return The information representing the operator
     * @see #attrType(String, AccessLevel)
     * @see #tryAttrType(LineInfo, String, AccessLevel)
     */
    @NotNull
    public final TypeObject tryAttrType(@NotNull Lined node, String value, @NotNull CompilerInfo info) {
        return tryAttrType(node.getLineInfo(), value, info.accessLevel(this));
    }

    public Optional<TypeObject> attrTypeWithGenerics(String value, AccessLevel access) {
        return attrType(value, access);
    }

    public Optional<TypeObject> staticAttrTypeWithGenerics(String value, AccessLevel access) {
        return staticAttrType(value, access);
    }

    public Optional<FunctionInfo> trueOperatorInfo(OpSpTypeNode o, AccessLevel access) {
        return operatorInfo(o, access);
    }

    @Override
    public int compareTo(@NotNull TypeObject o) {
        return this.hashCode() - o.hashCode();
    }

    TypeObject stripNull() {
        return this;
    }

    /**
     * Determines whether this type fulfills the contract set out by an {@link
     * InterfaceType interface}.
     *
     * @param contractor The contractor to check the fulfillment of
     * @return Whether or not the contract is fulfilled
     */
    public final boolean fulfillsContract(@NotNull UserType<?> contractor) {
        var contract = contractor.contract();
        for (var attr : contract.getKey()) {
            if (attrType(attr, AccessLevel.PUBLIC).isEmpty()) {
                return false;
            }
        }
        for (var op : contract.getValue()) {
            if (operatorInfo(op, AccessLevel.PUBLIC).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the union of the values given.
     * <p>
     *     In older versions of the language, this would simply construct a
     *     {@code LangUnion}, but that no longer exists, so the algorithm is
     *     slightly more complicated.
     * </p>
     * <p>
     *     The current union algorithm is meant to return the "most specific"
     *     class that is a superclass of all given values. If the list of values
     *     has length 1, the given value is returned. If all given values are
     *     the special value {@link Builtins#THROWS}, it is returned, otherwise
     *     it is removed from the set.
     * </p>
     *
     * @param values The values to get the union of
     * @return A type that is a superclass of all of them
     */
    static TypeObject union(@NotNull TypeObject... values) {
        assert values.length != 0;
        return union(new HashSet<>(Arrays.asList(values)));
    }

    /**
     * Gets the union of the values given.
     * <p>
     *     In older versions of the language, this would simply construct a
     *     {@code LangUnion}, but that no longer exists, so the algorithm is
     *     slightly more complicated.
     * </p>
     * <p>
     *     The current union algorithm is meant to return the "most specific"
     *     class that is a superclass of all given values. If the list of values
     *     has length 1, the given value is returned. If all given values are
     *     the special value {@link Builtins#THROWS}, it is returned, otherwise
     *     it is removed from the set.
     * </p>
     *
     * @param values The values to get the union of
     * @return A type that is a superclass of all of them
     */
    static TypeObject union(@NotNull List<TypeObject> values) {
        assert values.size() != 0 : "Empty union";
        return union(new HashSet<>(values));
    }

    private static TypeObject union(Set<TypeObject> valueSet) {
        if (valueSet.size() == 1) {
            return valueSet.iterator().next();
        } else {
            valueSet.remove(Builtins.THROWS);
            if (valueSet.isEmpty()) {
                return Builtins.THROWS;
            }
            TypeObject currentSuper = null;
            boolean isOptional = false;
            for (var value : valueSet) {
                if (value == Builtins.NULL_TYPE) {
                    isOptional = true;
                } else if (value instanceof OptionTypeObject) {
                    isOptional = true;
                    var option = ((OptionTypeObject) value).getOptionVal();
                    currentSuper = currentSuper == null ? option : getSuper(currentSuper, option);
                } else {
                    currentSuper = currentSuper == null ? value : getSuper(currentSuper, value);
                }
            }
            if (currentSuper == null) {  // Can only happen if all types are null
                return Builtins.NULL_TYPE;
            }
            return isOptional ? optional(currentSuper) : currentSuper;
        }
    }

    @NotNull
    private static TypeObject getSuper(@NotNull TypeObject a, TypeObject b) {
        if (a.isSuperclass(b)) {
            return a;
        } else if (b.isSuperclass(a)) {
            return b;
        }
        if (!(a instanceof UserType) || !(b instanceof UserType)) {
            throw CompilerTodoError.format(
                    "'getSuper' on non-user type '%s' or '%s'",
                    LineInfo.empty(), a.name(), b.name()
            );
        }
        var userA = (UserType<?>) a;
        var userB = (UserType<?>) b;
        Set<TypeObject> aSupers = new HashSet<>();
        Set<TypeObject> bSupers = new HashSet<>();
        for (var pair : Zipper.of(userA.recursiveSupers(), userB.recursiveSupers())) {
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

    @NotNull
    @Contract(pure = true)
    static TypeObject optional(@NotNull TypeObject value) {
        return new OptionTypeObject(value);
    }

    @Nullable
    static TypeObject of(CompilerInfo info, TestNode arg) {
        if (arg instanceof VariableNode) {
            return info.classOf(((VariableNode) arg).getName()).orElse(null);
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
            return cls.generify(arg, generics);
        } else {
            return null;
        }
    }

    /**
     * Adds the given generics to the {@link Map map} given in {@code result},
     * then returns whether or not an error occurred.
     * <p>
     *     An error will occur if both maps contain an entry for the same key
     *     and neither of those entries is a direct {@link
     *     #isSuperclass(TypeObject) superclass} of the other.
     * </p>
     * <p>
     *     Note: An error condition in this function will result in {@code
     *     true} being returned. This will leave {@code result} in a partially
     *     updated state, and should not be used henceforth.
     * </p>
     *
     * @param toAdd The values to add to the map
     * @param result The map to add to
     * @return If the addition was not successful
     */
    static boolean addGenericsToMap(@NotNull Map<Integer, TypeObject> toAdd, Map<Integer, TypeObject> result) {
        for (var pair : toAdd.entrySet()) {
            int index = pair.getKey();
            var obj = pair.getValue();
            var resultType = result.get(index);
            if (resultType == null) {
                result.put(index, obj);
            } else {
                if (obj.isSuperclass(resultType)) {
                    result.put(index, obj);
                } else if (!resultType.isSuperclass(obj)) {
                    return true;
                }
            }
        }
        return false;
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
