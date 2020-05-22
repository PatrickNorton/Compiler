package main.java.converter;

import main.java.parser.DescriptorNode;
import main.java.util.Counter;
import main.java.util.HashCounter;

public final class AccessHandler {
    private final Counter<TypeObject> classesWithAccess = new HashCounter<>();
    private final Counter<TypeObject> classesWithProtected = new HashCounter<>();

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
}
