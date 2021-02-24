package main.java.converter;

/**
 * Permission levels for the compiled code.
 * <ul>Levels
 *     <li>Normal: All user-written code</li>
 *     <li>Stdlib: Code in the stdlib; has access to $native("sys")</li>
 *     <li>Builtin: Only for __builtins__.newlang; can use $builtin</li>
 * </ul>
 *
 * @author Patrick Norton
 */
public enum PermissionLevel {
    NORMAL,
    STDLIB,
    BUILTIN,
    ;

    public boolean isStdlib() {
        return this == STDLIB || this == BUILTIN;
    }

    public boolean isBuiltin() {
        return this == BUILTIN;
    }
}
