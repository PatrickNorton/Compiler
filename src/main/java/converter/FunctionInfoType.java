package main.java.converter;

import main.java.parser.LineInfo;
import main.java.parser.OpSpTypeNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

public final class FunctionInfoType extends TypeObject {
    private final FunctionInfo info;

    public FunctionInfoType(FunctionInfo info) {
        this.info = info;
    }

    @Override
    protected boolean isSubclass(TypeObject other) {
        return this.equals(other);
    }

    @Override
    public Optional<TypeObject[]> operatorReturnType(OpSpTypeNode o, AccessLevel access) {
        if (o == OpSpTypeNode.CALL) {
            return Optional.of(info.boundify().getReturns());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<FunctionInfo> operatorInfo(OpSpTypeNode o, AccessLevel access) {
        if (o == OpSpTypeNode.CALL) {
            return Optional.of(info);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public TypeObject generify(LineInfo lineInfo, TypeObject... args) {
        return new GenerifiedFnInfoType(info, List.of(args));
    }

    @Override
    public String name() {
        var argStr = info.getArgs().argStr();
        if (info.getReturns().length == 0) {
            return "func" + argStr;
        } else {
            var joiner = new StringJoiner(", ", "func" + argStr + " -> ", "");
            for (var ret : info.getReturns()) {
                joiner.add(ret.name());
            }
            return joiner.toString();
        }
    }

    @Override
    public String baseName() {
        return name();
    }

    @Override
    public TypeObject typedefAs(String name) {
        throw new UnsupportedOperationException("How on earth did you typedef this?");
    }

    @Override
    public boolean sameBaseType(TypeObject other) {
        if (other instanceof FunctionInfoType) {
            return ((FunctionInfoType) other).info == info;
        } else if (other instanceof GenerifiedFnInfoType) {
            return ((GenerifiedFnInfoType) other).baseType().info == info;
        } else {
            return false;
        }
    }

    @Override
    public int baseHash() {
        return info.hashCode();
    }

    @Override
    public Optional<Map<Integer, TypeObject>> generifyAs(TypeObject parent, TypeObject other) {
        if (!(other instanceof FunctionInfoType)) {
            return Optional.empty();
        }
        var otherT = (FunctionInfoType) other;
        var otherInfo = otherT.info;
        Map<Integer, TypeObject> result = new HashMap<>();
        var args = info.getArgs();
        var otherArgs = otherInfo.getArgs();
        var argC = Math.min(args.size(), otherArgs.size());
        for (int i = 0; i < argC; i++) {
            var gen = args.get(i).getType().generifyAs(parent, otherArgs.get(i).getType());
            if (gen.isEmpty()) {
                return Optional.empty();
            }
            result.putAll(gen.orElseThrow());
        }
        var retCount = Math.min(otherInfo.getReturns().length, info.getReturns().length);
        for (int i = 0; i < retCount; i++) {
            var gen = info.getReturns()[i].generifyAs(parent, otherInfo.getReturns()[i]);
            if (gen.isEmpty()) {
                return Optional.empty();
            }
            result.putAll(gen.orElseThrow());
        }
        return Optional.of(result);
    }

    @Override
    public TypeObject generifyWith(TypeObject parent, List<TypeObject> values) {
        return new FunctionInfoType(info.generify(parent, values));
    }
}
