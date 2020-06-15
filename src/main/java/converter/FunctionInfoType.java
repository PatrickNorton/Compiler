package main.java.converter;

import main.java.parser.DescriptorNode;
import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FunctionInfoType extends TypeObject {
    private final FunctionInfo info;

    public FunctionInfoType(FunctionInfo info) {
        this.info = info;
    }

    @Override
    protected boolean isSubclass(@NotNull TypeObject other) {
        return this.equals(other);
    }

    @Nullable
    @Contract(pure = true)
    @Override
    public TypeObject[] operatorReturnType(OpSpTypeNode o, DescriptorNode access) {
        if (o == OpSpTypeNode.CALL) {
            return info.boundify().getReturns();
        } else {
            return null;
        }
    }

    @Nullable
    @Contract(pure = true)
    @Override
    public FunctionInfo operatorInfo(OpSpTypeNode o, DescriptorNode access) {
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

    @Override
    public Map<Integer, TypeObject> generifyAs(TypeObject other) {
        assert other instanceof FunctionInfoType;
        var otherT = (FunctionInfoType) other;
        var otherInfo = otherT.info;
        Map<Integer, TypeObject> result = new HashMap<>();
        var argC = Math.min(otherInfo.getArgs().size(), info.getArgs().size());
        for (int i = 0; i < argC; i++) {
            result.putAll(info.getArgs().get(i).getType().generifyAs(otherInfo.getArgs().get(i).getType()));
        }
        var retCount = Math.min(otherInfo.getReturns().length, info.getReturns().length);
        for (int i = 0; i < retCount; i++) {
            result.putAll(info.getReturns()[i].generifyAs(otherInfo.getReturns()[i]));
        }
        return result;
    }
}
