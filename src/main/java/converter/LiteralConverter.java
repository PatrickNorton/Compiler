package main.java.converter;

import main.java.parser.Lined;
import main.java.parser.LiteralNode;
import main.java.parser.OpSpTypeNode;
import main.java.parser.TestNode;
import main.java.util.Pair;
import main.java.util.Zipper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    public Optional<Integer> knownLength() {
        assert LiteralType.fromBrace(node.getBraceType(), node) != LiteralType.TUPLE;
        int count = 0;
        for (var pair : Zipper.of(node.getBuilders(), node.getIsSplats())) {
            var value = pair.getKey();
            var splat = pair.getValue();
            if (splat.isEmpty()) {
                count++;
            } else if (splat.equals("*")) {
                var converter = TestConverter.of(info, value, 1);
                var retType = converter.returnType()[0];
                if (retType instanceof TupleType) {
                    count += retType.getGenerics().size();
                } else if (retType.operatorInfo(OpSpTypeNode.ITER, info).isPresent()) {
                    if (converter instanceof LiteralConverter) {
                        var val = ((LiteralConverter) converter).knownLength();
                        if (val.isPresent()) {
                            count += val.orElseThrow();
                        } else {
                            return Optional.empty();
                        }
                    } else {
                        return Optional.empty();
                    }
                } else {
                    throw splatException(value, retType);
                }
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(count);
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
            var generics = returnTypes(node.getBuilders(), node.getIsSplats());
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
        short builderLen = (short) node.getBuilders().length;
        if (literalType == LiteralType.TUPLE) {
            short additional = 0;
            var builders = node.getBuilders();
            var isSplats = node.getIsSplats();
            var retTypes = tupleReturnTypes();
            for (int i = 0; i < retTypes.length; i++) {
                additional += convertInner(bytes, start, builders[i], isSplats[i], retTypes[i + additional]);
            }
            builderLen += additional;
        } else {
            var retType = returnTypes(node.getBuilders(), node.getIsSplats());
            for (var pair : Zipper.of(node.getBuilders(), node.getIsSplats())) {
                builderLen += convertInner(bytes, start, pair.getKey(), pair.getValue(), retType);
            }
            bytes.add(Bytecode.LOAD_CONST.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(info.typeConstant(node, retType))));
        }
        bytes.add(literalType.bytecode.value);
        bytes.addAll(Util.shortToBytes(builderLen));
        return bytes;
    }

    private short convertInner(List<Byte> bytes, int start, TestNode value, String splat, TypeObject retType) {
        if (splat.isEmpty()) {
            bytes.addAll(TestConverter.bytesMaybeOption(start + bytes.size(), value, info, 1, retType));
            return 0;
        } else if (splat.equals("*")) {
            var converter = TestConverter.of(info, value, 1);
            var convRet = converter.returnType()[0];
            bytes.addAll(converter.convert(start + bytes.size()));
            if (convRet instanceof TupleType) {
                bytes.add(Bytecode.UNPACK_TUPLE.value);
                return (short) (convRet.getGenerics().size() - 1);
            } else if (convRet.operatorInfo(OpSpTypeNode.ITER, info).isPresent()) {
                if (converter instanceof LiteralConverter) {
                    var len = ((LiteralConverter) converter).knownLength();
                    if (len.isPresent()) {
                        var knownLen = len.orElseThrow();
                        bytes.add(Bytecode.UNPACK_ITERABLE.value);
                        bytes.add(Bytecode.POP_TOP.value);  // UNPACK_ITERABLE leaves length on top
                        return (short) (knownLen.shortValue() - 1);
                    }
                }
                throw CompilerTodoError.of("Unpacking iterables in literals", value);
            } else {
                throw splatException(value, convRet);
            }
        } else {
            throw CompilerException.format("Invalid splat '%s'", value, splat);
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
        var genericType = returnTypes(node.getBuilders(), node.getIsSplats());
        bytes.add(Bytecode.LOAD_CONST.value);
        bytes.addAll(Util.shortToBytes(info.constIndex(info.typeConstant(node, genericType))));
        bytes.add(literalType.bytecode.value);
        bytes.addAll(Util.shortZeroBytes());
        return bytes;
    }

    @NotNull
    private TypeObject returnTypes(@NotNull TestNode[] args, String[] varargs) {
        if (expected != null) {
            return TypeObject.union(expected[0].getGenerics());
        }
        List<TypeObject> result = new ArrayList<>(args.length);
        for (int i = 0; i < args.length; i++) {
            if (varargs[i].isEmpty()) {
                result.add(TestConverter.returnType(args[i], info, 1)[0]);
            } else {
                assert varargs[i].equals("*");
                var retType = TestConverter.returnType(args[i], info, 1)[0];
                if (retType instanceof TupleType) {
                    result.addAll(retType.getGenerics());
                } else if (Builtins.ITERABLE.isSuperclass(retType)) {
                    result.add(Builtins.deIterable(retType)[0]);
                } else {
                    throw splatException(args[i], retType);
                }
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
            if (splat.isEmpty()) {
                result.add(value);
            } else if (splat.equals("*")) {
                if (value instanceof TupleType) {
                    result.addAll(value.getGenerics());
                } else if (value.operatorInfo(OpSpTypeNode.ITER, info).isPresent()) {
                    throw CompilerException.of("Cannot unpack iterable in tuple literal", arg);
                } else {
                    throw splatException(arg, value);
                }
            } else {
                throw CompilerException.format("Invalid splat '%s'", arg, splat);
            }
        }
        return result.toArray(new TypeObject[0]);
    }

    private static CompilerException splatException(Lined info, TypeObject type) {
        return CompilerException.format(
                "Cannot unpack type '%s': Unpacking is only valid on tuples or iterables",
                        info, type.name()
        );
    }
}
