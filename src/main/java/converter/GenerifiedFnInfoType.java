package main.java.converter;

import main.java.parser.DescriptorNode;
import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class GenerifiedFnInfoType extends TypeObject {
    private final FunctionInfo info;
    private final List<TypeObject> generics;

    public GenerifiedFnInfoType(FunctionInfo info, List<TypeObject> generics) {
        this.info = info;
        this.generics = generics;
    }

    @Override
    protected boolean isSubclass(@NotNull TypeObject other) {
        return false;  // TODO: Implement
    }

    @Nullable
    @Override
    public FunctionInfo operatorInfo(OpSpTypeNode o, DescriptorNode access) {
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

    @NotNull
    @Contract(pure = true)
    @Override
    public String baseName() {
        return "";
    }

    @Override
    public TypeObject typedefAs(String name) {
        throw new UnsupportedOperationException("How on earth did you typedef this?");
    }
}
