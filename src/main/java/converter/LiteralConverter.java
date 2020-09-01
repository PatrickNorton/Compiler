package main.java.converter;

import main.java.parser.Lined;
import main.java.parser.LiteralNode;
import main.java.parser.TestNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class LiteralConverter implements TestConverter {
    private final LiteralNode node;
    private final CompilerInfo info;
    private final int retCount;

    public LiteralConverter(CompilerInfo info, LiteralNode node, int retCount) {
        this.node = node;
        this.info = info;
        this.retCount = retCount;
    }

    private enum LiteralType {
        LIST(Builtins.LIST, "list", Bytecode.LIST_CREATE),
        SET(Builtins.SET, "set", Bytecode.SET_CREATE),
        TUPLE(Builtins.TUPLE, "tuple", Bytecode.PACK_TUPLE),
        ;

        TypeObject type;
        String name;
        Bytecode bytecode;

        LiteralType(TypeObject type, String name, Bytecode bytecode) {
            this.type = type;
            this.name = name;
            this.bytecode = bytecode;
        }

        static LiteralType fromBrace(@NotNull String brace, Lined lineInfo) {
            switch (brace) {
                case "[":
                    return LiteralType.LIST;
                case "{":
                    return LiteralType.SET;
                case "(":
                    return LiteralType.TUPLE;
                default:
                    throw CompilerInternalError.format("Unknown brace type %s", lineInfo, brace);
            }
        }
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        var literalType = LiteralType.fromBrace(node.getBraceType(), node);
        if (literalType == LiteralType.TUPLE) {
            return new TypeObject[]{literalType.type.generify(tupleReturnTypes(node.getBuilders()))};
        } else {
            var mainType = literalType.type;
            return new TypeObject[]{mainType.generify(returnTypes(node.getBuilders())).makeMut()};
        }
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        List<Byte> bytes = new ArrayList<>();
        var literalType = LiteralType.fromBrace(node.getBraceType(), node);
        if (retCount == 0) {  // If this is not being assigned, no need to actually create the list, just get side effects
            CompilerWarning.warnf("Unnecessary %s creation", node, literalType.name);
            for (var value : node.getBuilders()) {
                bytes.addAll(BaseConverter.bytes(start + bytes.size(), value, info));
            }
        } else {
            assert retCount == 1;
            var retType = returnTypes(node.getBuilders());
            for (var value : node.getBuilders()) {
                bytes.addAll(TestConverter.bytesMaybeOption(start + bytes.size(), value, info, 1, retType));
            }
            if (literalType != LiteralType.TUPLE) {
                var genericType = returnTypes(node.getBuilders());
                loadType(bytes, genericType);
            }
            bytes.add(literalType.bytecode.value);
            bytes.addAll(Util.shortToBytes((short) node.getBuilders().length));
        }
        return bytes;
    }

    @NotNull
    private TypeObject returnTypes(@NotNull TestNode[] args) {
        var result = new TypeObject[args.length];
        for (int i = 0; i < args.length; i++) {
            result[i] = TestConverter.returnType(args[i], info, 1)[0];
        }
        return args.length == 0 ? Builtins.OBJECT : TypeObject.union(result);
    }

    @NotNull
    private TypeObject[] tupleReturnTypes(@NotNull TestNode[] args) {
        var result = new TypeObject[args.length];
        for (int i = 0; i < args.length; i++) {
            result[i] = TestConverter.returnType(args[i], info, 1)[0];
        }
        return result;
    }

    private void loadType(List<Byte> bytes, @NotNull TypeObject type) {
        var name = type.baseName();
        if (name.isEmpty()) {
            throw CompilerInternalError.of(
                    "Error in literal conversion: Lists of non-nameable types not complete yet", node
            );
        }
        if (Builtins.BUILTIN_MAP.containsKey(name) && Builtins.BUILTIN_MAP.get(name) instanceof TypeObject) {
            bytes.add(Bytecode.LOAD_CONST.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(Builtins.constantOf(name))));
        } else {
            assert type instanceof UserType<?> : "All non-UserType types should be a builtin";
            bytes.add(Bytecode.LOAD_CONST.value);
            int index = info.classIndex(name).orElseThrow();
            bytes.addAll(Util.shortToBytes(info.constIndex(new ClassConstant(name, index, (UserType<?>) type))));
        }
    }
}
