package main.java.converter;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class MethodInfo implements IntoMethodInfo {
    private final AccessLevel accessLevel;
    private final boolean isMut;
    private final FunctionInfo info;

    public MethodInfo(AccessLevel access, boolean isMut, FunctionInfo info) {
        this.accessLevel = access;
        this.isMut = isMut;
        this.info = info;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public boolean isMut() {
        return isMut;
    }

    public FunctionInfo getInfo() {
        return info;
    }

    public TypeObject[] getReturns() {
        return info.getReturns();
    }

    @Override
    public MethodInfo intoMethodInfo() {
        return this;
    }

    @Contract("_, _ -> new")
    @NotNull
    public static MethodInfo of(ArgumentInfo args, TypeObject... returns) {
        return new MethodInfo(AccessLevel.PUBLIC, false, new FunctionInfo(args, returns));
    }

    @Contract("_ -> new")
    @NotNull
    public static MethodInfo of(TypeObject... returns) {
        return new MethodInfo(AccessLevel.PUBLIC, false, new FunctionInfo(returns));
    }

    @Contract("_, _ -> new")
    @NotNull
    public static MethodInfo ofMut(ArgumentInfo args, TypeObject... returns) {
        return new MethodInfo(AccessLevel.PUBLIC, true, new FunctionInfo(args, returns));
    }
}
