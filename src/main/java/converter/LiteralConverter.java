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
        for (var pair : Zipper.of(node.getBuilders(), node.getIsSplats())) {
            var builder = pair.getKey();
            var splat = pair.getValue();
            var constant = TestConverter.constantReturn(builder, info, 1);
            if (constant.isEmpty()) {
                return Optional.empty();
            } else {
                var value = constant.orElseThrow();
                switch (splat) {
                    case "":
                        values.add(Pair.of(info.addConstant(value), value.getType()));
                        break;
                    case "*":
                        if (value instanceof TupleConstant) {
                            values.addAll(((TupleConstant) value).getValues());
                        } else {
                            return Optional.empty();
                        }
                        break;
                    default:
                        return Optional.empty();
                }
            }
        }
        return Optional.of(new TupleConstant(values));
    }

    private enum LiteralType {
        LIST("list", Bytecode.LIST_CREATE, Bytecode.LIST_DYN),
        SET("set", Bytecode.SET_CREATE, Bytecode.SET_DYN),
        TUPLE("tuple", Bytecode.PACK_TUPLE),
        ;

        String name;
        Bytecode bytecode;
        Bytecode dynCode;

        LiteralType(String name, Bytecode bytecode) {
            this.name = name;
            this.bytecode = bytecode;
            this.dynCode = null;
        }

        LiteralType(String name, Bytecode bytecode, Bytecode dynCode) {
            this.name = name;
            this.bytecode = bytecode;
            this.dynCode = dynCode;
        }

        TypeObject type() {
            return switch (this) {
                case LIST -> Builtins.list();
                case SET -> Builtins.set();
                case TUPLE -> Builtins.tuple();
            };
        }

        static LiteralType fromBrace(@NotNull String brace, Lined lineInfo) {
            return switch (brace) {
                case "[" -> LiteralType.LIST;
                case "{" -> LiteralType.SET;
                case "(" -> LiteralType.TUPLE;
                default -> throw CompilerInternalError.format("Unknown brace type %s", lineInfo, brace);
            };
        }
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        var literalType = LiteralType.fromBrace(node.getBraceType(), node);
        var literalCls = literalType.type().makeMut();
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
    public BytecodeList convert() {
        var literalType = LiteralType.fromBrace(node.getBraceType(), node);
        if (retCount == 0) {  // If this is not being assigned, no need to actually create the list, just get side effects
            CompilerWarning.warnf("Unnecessary %s creation", WarningType.UNUSED, info, node, literalType.name);
            var bytes = new BytecodeList();
            for (var value : node.getBuilders()) {
                bytes.addAll(BaseConverter.bytes(value, info));
            }
            return bytes;
        } else if (node.getBuilders().length == 0) {
            return convertEmpty(literalType);
        } else {
            return convertSingle(literalType);
        }
    }

    @NotNull
    private BytecodeList convertSingle(LiteralType literalType) {
        if (retCount > 1) {
            throw CompilerException.format("Literal returns 1 value, expected %d", node, retCount);
        }
        var bytes = new BytecodeList();
        var constant = constantReturn();
        if (constant.isPresent()) {
            bytes.add(Bytecode.LOAD_CONST, info.constIndex(constant.orElseThrow()));
            return bytes;
        }
        Set<Integer> unknowns = new HashSet<>();
        short additional = 0;
        var builders = node.getBuilders();
        var isSplats = node.getIsSplats();
        if (literalType == LiteralType.TUPLE) {
            var retTypes = tupleReturnTypes();
            for (int i = 0; i < retTypes.length; i++) {
                additional += convertInner(
                        bytes, builders[i], isSplats[i], retTypes[i + additional], unknowns, i
                );
            }
            completeLiteral(bytes, literalType, unknowns, additional, null);
        } else {
            var retType = returnTypes();
            for (int i = 0; i < builders.length; i++) {
                additional += convertInner(bytes, builders[i], isSplats[i], retType, unknowns, i);
            }
            completeLiteral(bytes, literalType, unknowns, additional, retType);
        }
        return bytes;
    }

    private void completeLiteral(
            BytecodeList bytes, LiteralType literalType,
            @NotNull Set<Integer> unknowns, short additional, TypeObject retType
    ) {
        var builderLen = node.getBuilders().length + additional - unknowns.size();
        assert builderLen >= 0 : String.format("Should not have a negative number of builders (%d)", builderLen);
        if (unknowns.isEmpty()) {
            if (retType != null) {
                bytes.addAll(new TypeLoader(node.getLineInfo(), retType, info).convert());
            }
            bytes.add(literalType.bytecode, builderLen);
        } else {
            if (literalType == LiteralType.TUPLE) {
                int index = unknowns.iterator().next();
                throw CompilerException.of("Cannot unpack iterables in tuple literal", node.getBuilders()[index]);
            }
            if (builderLen != 0) {
                bytes.add(Bytecode.LOAD_CONST, info.constIndex(LangConstant.of(builderLen)));
                bytes.add(Bytecode.PLUS);
            }
            bytes.addAll(new TypeLoader(node.getLineInfo(), retType, info).convert());
            bytes.add(literalType.dynCode);
        }
    }

    private short convertInner(
            BytecodeList bytes, TestNode value, @NotNull String splat, TypeObject retType, Set<Integer> unknowns, int i
    ) {
        switch (splat) {
            case "":
                bytes.addAll(TestConverter.bytesMaybeOption(value, info, 1, retType));
                if (!unknowns.isEmpty()) {
                    bytes.add(Bytecode.SWAP_2);  // Keep unknown length on top
                }
                return 0;
            case "*":
                if (value instanceof LiteralNode) {
                    return convertStarLiteral(bytes, (LiteralNode) value, unknowns, i, retType);
                } else {
                    return convertStar(bytes, value, unknowns, i);
                }
            case "**":
                throw dictSplatException(value);
            default:
                throw unknownSplatError(value, splat);
        }
    }

    private short convertStarLiteral(
            BytecodeList bytes, LiteralNode value, Set<Integer> unknowns, int i, TypeObject retType
    ) {
        var selfType = LiteralType.fromBrace(node.getBraceType(), node);
        var literalType = LiteralType.fromBrace(value.getBraceType(), value);
        if (literalType == LiteralType.SET && selfType != LiteralType.SET) {
            return convertStar(bytes, value, unknowns, i);
        }
        Set<Integer> values = new HashSet<>();
        int additional = value.getBuilders().length - 1;
        for (int j = 0; j < value.getBuilders().length; j++) {
            var builder = value.getBuilders()[j];
            var splat = value.getIsSplats()[i];
            additional += convertInner(bytes, builder, splat, retType, values, j);
        }
        if (!values.isEmpty()) {
            unknowns.add(i);
        }
        return (short) additional;
    }

    private short convertStar(BytecodeList bytes, TestNode value, Set<Integer> unknowns, int i) {
        var converter = TestConverter.of(info, value, 1);
        var convRet = converter.returnType()[0];
        var constant = converter.constantReturn();
        if (constant.isPresent() && constant.orElseThrow() instanceof TupleConstant) {
            return convertTupleLiteral(bytes, (TupleConstant) constant.orElseThrow());
        }
        bytes.addAll(converter.convert());
        if (convRet instanceof TupleType) {
            bytes.add(Bytecode.UNPACK_TUPLE);
            if (!unknowns.isEmpty()) {
                throw CompilerTodoError.of("Tuple unpacking after dynamic unpack", value);
            }
            return (short) (convRet.getGenerics().size() - 1);
        } else if (convRet.operatorInfo(OpSpTypeNode.ITER, info).isPresent()) {
            bytes.add(Bytecode.UNPACK_ITERABLE);
            if (!unknowns.isEmpty()) {
                bytes.add(Bytecode.DUP_TOP);
                bytes.add(Bytecode.SWAP_DYN);
                bytes.add(Bytecode.PLUS);
            }
            unknowns.add(i);
            return 0;
        } else {
            throw splatException(value, convRet);
        }
    }

    private short convertTupleLiteral(BytecodeList bytes, @NotNull TupleConstant constant) {
        for (var value : constant.getValues()) {
            bytes.add(Bytecode.LOAD_CONST, value.getKey());
        }
        return (short) (constant.getValues().size() - 1);
    }

    @NotNull
    private BytecodeList convertEmpty(LiteralType literalType) {
        BytecodeList bytes = new BytecodeList();
        if (literalType == LiteralType.TUPLE) {
            bytes.add(Bytecode.PACK_TUPLE, 0);
            return bytes;
        }
        if (expected == null) {
            throw CompilerException.format("Cannot deduce type of %s literal", node, literalType.name);
        }
        var generics = expected[0].getGenerics();
        literalType.type().generify(node, generics.toArray(new TypeObject[0]));  // Ensure generification is possible
        bytes.addAll(new TypeLoader(node.getLineInfo(), returnTypes(), info).convert());
        bytes.add(literalType.bytecode, 0);
        return bytes;
    }

    @NotNull
    private TypeObject returnTypes() {
        var args = node.getBuilders();
        var varargs = node.getIsSplats();
        TypeObject expectedVal;
        if (expected != null && expectedTypeWorks()) {
            expectedVal = TypeObject.union(expected[0].getGenerics());
        } else {
            expectedVal = null;
        }
        List<TypeObject> result = new ArrayList<>(args.length);
        for (int i = 0; i < args.length; i++) {
            switch (varargs[i]) {
                case "" -> result.add(TestConverter.returnType(args[i], info, 1)[0]);
                case "*" -> {
                    var retType = TestConverter.returnType(args[i], info, 1)[0];
                    if (retType instanceof TupleType) {
                        result.addAll(retType.getGenerics());
                    } else if (Builtins.iterable().isSuperclass(retType)) {
                        result.add(Builtins.deIterable(retType)[0]);
                    } else {
                        throw splatException(args[i], retType);
                    }
                }
                case "**" -> throw dictSplatException(args[i]);
                default -> throw unknownSplatError(args[i], varargs[i]);
            }
        }
        if (expectedVal == null) {
            return args.length == 0 ? Builtins.object() : TypeObject.union(result);
        } else {
            if (args.length == 0) {
                return expectedVal;
            } else {
                return convertExpected(TypeObject.union(result), expectedVal);
            }
        }
    }

    private boolean expectedTypeWorks() {
        assert expected != null;
        return LiteralType.fromBrace(node.getBraceType(), node).type().sameBaseType(expected[0]);
    }

    @NotNull
    private TypeObject[] tupleReturnTypes() {
        var originals = originalReturnTypes();
        if (expected != null && expectedTypeWorks()) {
            var generics = expected[0].getGenerics();
            if (generics.size() == originals.size()) {
                for (int i = 0; i < originals.size(); i++) {
                    var given = originals.get(i);
                    var expected = generics.get(i);
                    originals.set(i, convertExpected(given, expected));
                }
            }
        }
        return originals.toArray(new TypeObject[0]);
    }

    private List<TypeObject> originalReturnTypes() {
        var args = node.getBuilders();
        var splats = node.getIsSplats();
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
        return result;
    }

    private TypeObject convertExpected(TypeObject given, TypeObject expected) {
        if (expected.isSuperclass(given)) {
            return expected;
        } else if (OptionTypeObject.needsAndSuper(expected, given)) {
            return expected;
        } else {
            return given;
        }
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
