package main.java.converter;

import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

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

    @NotNull
    @Override
    public Optional<FunctionInfo> operatorInfo(OpSpTypeNode o, AccessLevel access) {
        if (o == OpSpTypeNode.CALL) {
            return Optional.of(info.generify(this, generics));
        } else {
            return Optional.empty();
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

    @Override
    public boolean sameBaseType(TypeObject other) {
        return baseType().sameBaseType(other);
    }

    @Contract(" -> new")
    @NotNull
    public FunctionInfoType baseType() {
        return new FunctionInfoType(info);
    }
}
