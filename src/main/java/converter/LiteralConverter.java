package main.java.converter;

import main.java.parser.Lined;
import main.java.parser.LiteralNode;
import main.java.parser.OpSpTypeNode;
import main.java.parser.TestNode;
import main.java.util.Pair;
import main.java.util.Zipper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class LiteralConverter implements TestConverter {
    private final LiteralNode node;
    private final CompilerInfo info;
    private final int retCount;
    private final TypeObject[] expected;

    public LiteralConverter(CompilerInfo info, LiteralNode node, int retCount, TypeObject[] expected) {
        this.node = node;
        this.info = info;
        this.retCount = retCount;
        this.expected = expected;
    }

    public LiteralConverter(CompilerInfo info, LiteralNode node, int retCount) {
        this.node = node;
        this.info = info;
        this.retCount = retCount;
        this.expected = null;
    }

    @Override
    public Optional<LangConstant> constantReturn() {
        if (LiteralType.fromBrace(node.getBraceType(), node) != LiteralType.TUPLE) {
            return Optional.empty();
        }
        List<Pair<Short, TypeObject>> values = new ArrayList<>(node.getBuilders().length);
        for (var builder : node.getBuilders()) {
            var constant = TestConverter.constantReturn(builder, info, 1);
            if (constant.isEmpty()) {
                return Optional.empty();
            } else {
                var value = constant.orElseThrow();
                values.add(Pair.of(info.addConstant(value), value.getType()));
            }
        }
        return Optional.of(new TupleConstant(values));
    }

    private enum LiteralType {
        LIST(Builtins.LIST, "list", Bytecode.LIST_CREATE, Bytecode.LIST_DYN),
        SET(Builtins.SET, "set", Bytecode.SET_CREATE, Bytecode.SET_DYN),
        TUPLE(Builtins.TUPLE, "tuple", Bytecode.PACK_TUPLE),
        ;

        TypeObject type;
        String name;
        Bytecode bytecode;
        Bytecode dynCode;

        LiteralType(TypeObject type, String name, Bytecode bytecode) {
            this.type = type;
            this.name = name;
            this.bytecode = bytecode;
            this.dynCode = null;
        }

        LiteralType(TypeObject type, String name, Bytecode bytecode, Bytecode dynCode) {
            this.type = type;
            this.name = name;
            this.bytecode = bytecode;
            this.dynCode = dynCode;
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
        var literalCls = literalType.type.makeMut();
        if (literalType == LiteralType.TUPLE) {
            return new TypeObject[]{literalCls.generify(tupleReturnTypes())};
        } else if (node.getBuilders().length == 0) {
            if (expected == null) {
                throw CompilerException.format("Cannot deduce type of %s literal", node, literalType.name);
            }
            var generics = expected[0].getGenerics();
            return new TypeObject[] {literalCls.generify(node, generics.toArray(new TypeObject[0]))};
        } else {
            var generics = returnTypes();
            return new TypeObject[]{literalCls.generify(generics).makeMut()};
        }
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        var literalType = LiteralType.fromBrace(node.getBraceType(), node);
        if (retCount == 0) {  // If this is not being assigned, no need to actually create the list, just get side effects
            CompilerWarning.warnf("Unnecessary %s creation", node, literalType.name);
            List<Byte> bytes = new ArrayList<>();
            for (var value : node.getBuilders()) {
                bytes.addAll(BaseConverter.bytes(start + bytes.size(), value, info));
            }
            return bytes;
        } else if (node.getBuilders().length == 0) {
            return convertEmpty(literalType);
        } else {
            return convertSingle(start, literalType);
        }
    }

    private List<Byte> convertSingle(int start, LiteralType literalType) {
        if (retCount > 1) {
            throw CompilerException.format("Literal returns 1 value, expected %d", node, retCount);
        }
        List<Byte> bytes = new ArrayList<>();
        var constant = constantReturn();
        if (constant.isPresent()) {
            bytes.add(Bytecode.LOAD_CONST.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(constant.orElseThrow())));
            return bytes;
        }
        Set<Integer> unknowns = new HashSet<>();
        short additional = 0;
        var builders = node.getBuilders();
        var isSplats = node.getIsSplats();
        TypeObject retType;
        if (literalType == LiteralType.TUPLE) {
            retType = null;
            var retTypes = tupleReturnTypes();
            for (int i = 0; i < retTypes.length; i++) {
                additional += convertInner(
                        bytes, start, builders[i], isSplats[i], retTypes[i + additional], unknowns, i
                );
            }
        } else {
            retType = returnTypes();
            for (int i = 0; i < builders.length; i++) {
                additional += convertInner(bytes, start, builders[i], isSplats[i], retType, unknowns, i);
            }
        }
        var builderLen = node.getBuilders().length + additional - unknowns.size();
        assert builderLen >= 0 : "Should not have a negative number of builders";
        if (unknowns.isEmpty()) {
            if (retType != null) {
                bytes.add(Bytecode.LOAD_CONST.value);
                bytes.addAll(Util.shortToBytes(typeConst(retType)));
            }
            bytes.add(literalType.bytecode.value);
            bytes.addAll(Util.shortToBytes((short) builderLen));
        } else {
            if (literalType == LiteralType.TUPLE) {
                int index = unknowns.iterator().next();
                throw CompilerException.of("Cannot unpack iterables in tuple literal", node.getBuilders()[index]);
            }
            if (builderLen != 0) {
                bytes.add(Bytecode.LOAD_CONST.value);
                bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(builderLen))));
                bytes.add(Bytecode.PLUS.value);
            }
            bytes.add(Bytecode.LOAD_CONST.value);
            bytes.addAll(Util.shortToBytes(typeConst(retType)));
            bytes.add(literalType.dynCode.value);
        }
        return bytes;
    }

    private short convertInner(
            List<Byte> bytes, int start, TestNode value, String splat, TypeObject retType, Set<Integer> unknowns, int i
    ) {
        switch (splat) {
            case "":
                bytes.addAll(TestConverter.bytesMaybeOption(start + bytes.size(), value, info, 1, retType));
                if (!unknowns.isEmpty()) {
                    bytes.add(Bytecode.SWAP_2.value);  // Keep unknown length on top
                }
                return 0;
            case "*":
                var converter = TestConverter.of(info, value, 1);
                var convRet = converter.returnType()[0];
                bytes.addAll(converter.convert(start + bytes.size()));
                if (convRet instanceof TupleType) {
                    bytes.add(Bytecode.UNPACK_TUPLE.value);
                    if (!unknowns.isEmpty()) {
                        throw CompilerTodoError.of("Tuple unpacking after dynamic unpack", value);
                    }
                    return (short) (convRet.getGenerics().size() - 1);
                } else if (convRet.operatorInfo(OpSpTypeNode.ITER, info).isPresent()) {
                    bytes.add(Bytecode.UNPACK_ITERABLE.value);
                    if (!unknowns.isEmpty()) {
                        bytes.add(Bytecode.DUP_TOP.value);
                        bytes.add(Bytecode.SWAP_DYN.value);
                        bytes.add(Bytecode.PLUS.value);
                    }
                    unknowns.add(i);
                    return 0;
                } else {
                    throw splatException(value, convRet);
                }
            case "**":
                throw dictSplatException(value);
            default:
                throw unknownSplatError(value, splat);
        }
    }

    private List<Byte> convertEmpty(LiteralType literalType) {
        List<Byte> bytes = new ArrayList<>();
        if (literalType == LiteralType.TUPLE) {
            bytes.add(Bytecode.PACK_TUPLE.value);
            bytes.addAll(Util.shortZeroBytes());
            return bytes;
        }
        if (expected == null) {
            throw CompilerException.format("Cannot deduce type of %s literal", node, literalType.name);
        }
        var generics = expected[0].getGenerics();
        literalType.type.generify(node, generics.toArray(new TypeObject[0]));  // Ensure generification is possible
        var genericType = returnTypes();
        bytes.add(Bytecode.LOAD_CONST.value);
        bytes.addAll(Util.shortToBytes(typeConst(genericType)));
        bytes.add(literalType.bytecode.value);
        bytes.addAll(Util.shortZeroBytes());
        return bytes;
    }

    @NotNull
    private TypeObject returnTypes() {
        var args = node.getBuilders();
        var varargs = node.getIsSplats();
        if (expected != null) {
            return TypeObject.union(expected[0].getGenerics());
        }
        List<TypeObject> result = new ArrayList<>(args.length);
        for (int i = 0; i < args.length; i++) {
            switch (varargs[i]) {
                case "":
                    result.add(TestConverter.returnType(args[i], info, 1)[0]);
                    break;
                case "*":
                    var retType = TestConverter.returnType(args[i], info, 1)[0];
                    if (retType instanceof TupleType) {
                        result.addAll(retType.getGenerics());
                    } else if (Builtins.ITERABLE.isSuperclass(retType)) {
                        result.add(Builtins.deIterable(retType)[0]);
                    } else {
                        throw splatException(args[i], retType);
                    }
                    break;
                case "**":
                    throw dictSplatException(args[i]);
                default:
                    throw unknownSplatError(args[i], varargs[i]);
            }
        }
        return args.length == 0 ? Builtins.OBJECT : TypeObject.union(result);
    }

    @NotNull
    private TypeObject[] tupleReturnTypes() {
        var args = node.getBuilders();
        var splats = node.getIsSplats();
        if (expected != null) {
            return expected[0].getGenerics().toArray(new TypeObject[0]);
        }
        List<TypeObject> result = new ArrayList<>(args.length);
        for (var pair : Zipper.of(args, splats)) {
            var arg = pair.getKey();
            var splat = pair.getValue();
            var value = TestConverter.returnType(arg, info, 1)[0];
            switch (splat) {
                case "":
                    result.add(value);
                    break;
                case "*":
                    if (value instanceof TupleType) {
                        result.addAll(value.getGenerics());
                    } else if (value.operatorInfo(OpSpTypeNode.ITER, info).isPresent()) {
                        throw CompilerException.of("Cannot unpack iterable in tuple literal", arg);
                    } else {
                        throw splatException(arg, value);
                    }
                    break;
                case "**":
                    throw dictSplatException(arg);
                default:
                    throw unknownSplatError(arg, splat);
            }
        }
        return result.toArray(new TypeObject[0]);
    }

    private short typeConst(TypeObject value) {
        return info.constIndex(info.typeConstant(node, value));
    }

    private static CompilerException splatException(Lined info, TypeObject type) {
        return CompilerException.format(
                "Cannot unpack type '%s': Unpacking is only valid on tuples or iterables",
                        info, type.name()
        );
    }

    private static CompilerException dictSplatException(Lined info) {
        return CompilerException.of(
                "Dictionary unpacking with '**' is only allowed in dict literals", info
        );
    }

    private static CompilerInternalError unknownSplatError(Lined info, String splat) {
        return CompilerInternalError.format("Unknown splat type '%s'", info, splat);
    }
}
