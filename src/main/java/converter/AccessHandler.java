package main.java.converter;

import main.java.parser.DescriptorNode;
import main.java.util.Counter;
import main.java.util.HashCounter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;

public final class AccessHandler {
    private final Counter<TypeObject> classesWithAccess = new HashCounter<>();
    private final Counter<TypeObject> classesWithProtected = new HashCounter<>();
    private final Deque<TypeObject> clsTypes = new ArrayDeque<>();
    private final Deque<TypeObject> superTypes = new ArrayDeque<>();

    /**
     * The current access level of the given {@link TypeObject}.
     * <p>
     *     The access level is the highest-security level which is allowed
     *     to be accessed by values in the current scope. All security levels
     *     less strict than the one given may be used. Others will throw an
     *     exception at some point in compilation
     * </p>
     * @param obj The object to get the access level for
     * @return The security access level of the type
     */
    public DescriptorNode accessLevel(TypeObject obj) {
        return classesWithAccess.contains(obj) ? DescriptorNode.PRIVATE
                : classesWithProtected.contains(obj) ? DescriptorNode.PROTECTED : DescriptorNode.PUBLIC;
    }

    /**
     * Allows private access to the {@link TypeObject} given.
     *
     * @param obj The object to allow private access for
     * @see #removePrivateAccess
     */
    public void allowPrivateAccess(TypeObject obj) {
        classesWithAccess.increment(obj);
    }

    /**
     * Removes private access for the {@link TypeObject} given.
     * <p>
     *     This does not guarantee that the value returned by {@link
     *     #accessLevel} will be more strict than {@code private}, as it is
     *     possible that private access was given in another location. Private
     *     access will last until this has been called as many times as {@link
     *     #allowPrivateAccess}, in order to allow correct access.
     * </p>
     *
     * @param obj The object to remove private access for
     * @see #allowPrivateAccess
     */
    public void removePrivateAccess(TypeObject obj) {
        assert classesWithAccess.contains(obj);
        classesWithAccess.decrement(obj);
    }

    /**
     * Allows protected access to the {@link TypeObject} given.
     *
     * @param obj The object to allow protected access for
     * @see #removeProtectedAccess
     */
    public void allowProtectedAccess(TypeObject obj) {
        classesWithProtected.increment(obj);
    }

    /**
     * Removes protected access for the {@link TypeObject} given.
     * <p>
     *     This does not guarantee that the value returned by {@link
     *     #accessLevel} will be more strict than {@code protected}, as it is
     *     possible that protected access was given in another location.
     *     Protected access will last until this has been called as many times
     *     as {@link #allowProtectedAccess}, in order to allow correct access.
     * </p>
     *
     * @param obj The object to remove private access for
     * @see #allowProtectedAccess
     */
    public void removeProtectedAccess(TypeObject obj) {
        assert classesWithProtected.contains(obj);
        classesWithProtected.decrement(obj);
    }

    /**
     * Adds a new {@link TypeObject type} to be represented by the type
     * variable {@code cls} in method headers.
     * <p>
     *     This should always be used in conjunction with {@link #removeCls},
     *     to prevent leakage of types
     * </p>
     *
     * @param cls The type to be used
     * @see #removeCls()
     */
    public void addCls(@NotNull TypeObject cls) {
        clsTypes.push(cls);
    }

    /**
     * Adds a new {@link TypeObject type} to be represented by the type
     * variable {@code super} in method headers.
     * <p>
     *     This should always be used in conjunction with {@link #removeCls},
     *     to prevent leakage of types
     * </p>
     *
     * @param cls The type to be used
     * @see #removeSuper() ()
     */
    public void addSuper(@NotNull TypeObject cls) {
        superTypes.push(cls);
    }

    /**
     * Removes a {@link TypeObject type} from being represented by the {@code
     * cls} variable; e.g. it undoes what was done by {@link #addCls}.
     */
    public void removeCls() {
        clsTypes.pop();
    }

    /**
     * Removes a {@link TypeObject type} from being represented by the {@code
     * super} variable; e.g. it undoes what was done by {@link #addSuper}.
     */
    public void removeSuper() {
        superTypes.pop();
    }

    public TypeObject getCls() {
        return clsTypes.peekFirst();
    }

    public TypeObject getSuper() {
        return superTypes.peekFirst();
    }
}
