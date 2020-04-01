package main.java.converter;

import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class FunctionInfoType extends TypeObject {
    private FunctionInfo info;

    public FunctionInfoType(FunctionInfo info) {
        this.info = info;
    }

    @Override
    public boolean isSuperclass(TypeObject other) {
        return other instanceof FunctionInfoType && ((FunctionInfoType) other).info.equals(info);
    }

    @Override
    public boolean isSubclass(@NotNull TypeObject other) {
        return (other instanceof FunctionInfoType && ((FunctionInfoType) other).info.equals(info))
                || (!other.superWillRecurse() && other.isSuperclass(this));
    }

    @Nullable
    @Contract(pure = true)
    @Override
    public TypeObject[] operatorReturnType(OpSpTypeNode o) {
        if (o == OpSpTypeNode.CALL) {
            return info.boundify().getReturns();
        } else {
            return null;
        }
    }

    @Nullable
    @Contract(pure = true)
    @Override
    public FunctionInfo operatorInfo(OpSpTypeNode o) {
        if (o == OpSpTypeNode.CALL) {
            return info.boundify();
        } else {
            return null;
        }
    }

    @NotNull
    @Contract("_ -> new")
    @Override
    public TypeObject generify(TypeObject... args) {
        return new GenerifiedFnInfoType(info, List.of(args));
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public String name() {
        return "";
    }

    @Override
    public TypeObject typedefAs(String name) {
        throw new UnsupportedOperationException("How on earth did you typedef this?");
    }
}
