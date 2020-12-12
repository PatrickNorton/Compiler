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
        List<Byte> bytes = new ArrayList<>();
        var literalType = LiteralType.fromBrace(node.getBraceType(), node);
        if (retCount == 0) {  // If this is not being assigned, no need to actually create the list, just get side effects
            CompilerWarning.warnf("Unnecessary %s creation", node, literalType.name);
            for (var value : node.getBuilders()) {
                bytes.addAll(BaseConverter.bytes(start + bytes.size(), value, info));
            }
        } else if (node.getBuilders().length == 0) {
            convertEmpty(bytes, literalType);
        } else {
            convertSingle(bytes, start, literalType);
        }
        return bytes;
    }

    private void convertSingle(List<Byte> bytes, int start, LiteralType literalType) {
        if (retCount > 1) {
            throw CompilerException.format("Literal returns 1 value, expected %d", node, retCount);
        }
        var constant = constantReturn();
        if (constant.isPresent()) {
            bytes.add(Bytecode.LOAD_CONST.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(constant.orElseThrow())));
            return;
        }
        short builderLen = (short) node.getBuilders().length;
        if (literalType == LiteralType.TUPLE) {
            var builders = node.getBuilders();
            var isSplats = node.getIsSplats();
            var retTypes = tupleReturnTypes();
            for (int i = 0; i < retTypes.length; i++) {
                builderLen += convertInner(bytes, start, builders[i], isSplats[i], retTypes[i]);
            }
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
    }

    private short convertInner(List<Byte> bytes, int start, TestNode value, String splat, TypeObject retType) {
        if (splat.isEmpty()) {
            bytes.addAll(TestConverter.bytesMaybeOption(start + bytes.size(), value, info, 1, retType));
            return 0;
        } else if (splat.equals("*")) {
            var converter = TestConverter.of(info, value, 1);
            var convRet = converter.returnType()[0];
            bytes.addAll(TestConverter.bytesMaybeOption(converter, start + bytes.size(), retType));
            if (convRet instanceof TupleType) {
                bytes.add(Bytecode.UNPACK_TUPLE.value);
                return (short) (convRet.getGenerics().size() - 1);
            } else if (convRet.operatorInfo(OpSpTypeNode.ITER, info).isPresent()) {
                throw CompilerTodoError.of("Unpacking iterables in literals", value);
            } else {
                throw CompilerException.format(
                        "Cannot unpack type '%s': Unpacking is only valid on tuples or iterables",
                        value, convRet.name()
                );
            }
        } else {
            throw CompilerException.format("Invalid splat '%s'", value, splat);
        }
    }

    private void convertEmpty(List<Byte> bytes, LiteralType literalType) {
        if (literalType == LiteralType.TUPLE) {
            bytes.add(Bytecode.PACK_TUPLE.value);
            bytes.addAll(Util.shortZeroBytes());
            return;
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
                    throw CompilerException.format(
                            "* is only valid with tuples or iterables, not '%s'",
                            args[i], retType
                    );
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
        for (int i = 0; i < args.length; i++) {
            var value = TestConverter.returnType(args[i], info, 1)[0];
            if (splats[i].isEmpty()) {
                result.add(value);
            } else if (splats[i].equals("*")) {
                if (value instanceof TupleType) {
                    result.addAll(value.getGenerics());
                } else if (value.operatorInfo(OpSpTypeNode.ITER, info).isPresent()) {
                    throw CompilerException.of("Cannot unpack iterable in tuple literal", args[i]);
                } else {
                    throw CompilerException.format("Can only unpack iterable or tuple, not %s", args[i], value);
                }
            } else {
                throw CompilerException.format("Invalid splat '%s'", args[i], splats[i]);
            }
        }
        return result.toArray(new TypeObject[0]);
    }
}
