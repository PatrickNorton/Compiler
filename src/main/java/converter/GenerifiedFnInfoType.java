package main.java.converter;

import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class GenerifiedFnInfoType implements TypeObject {
    private FunctionInfo info;
    private List<TypeObject> generics;

    public GenerifiedFnInfoType(FunctionInfo info, List<TypeObject> generics) {
        this.info = info;
        this.generics = generics;
    }

    @Override
    public boolean isSuperclass(TypeObject other) {
        return false;  // TODO: Implement
    }

    @Nullable
    @Override
    public FunctionInfo operatorInfo(OpSpTypeNode o) {
        if (o == OpSpTypeNode.CALL) {
            return info.generify(generics);
        } else {
            return null;
        }
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public String name() {
        return "";
    }
}
