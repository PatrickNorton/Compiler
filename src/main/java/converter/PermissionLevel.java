package main.java.converter;

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
