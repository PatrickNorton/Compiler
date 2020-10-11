package main.java.converter;

import main.java.parser.Lined;
import main.java.parser.LiteralNode;
import main.java.parser.TestNode;
import main.java.util.Pair;
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
            return new TypeObject[]{literalCls.generify(tupleReturnTypes(node.getBuilders()))};
        } else if (node.getBuilders().length == 0) {
            if (expected == null) {
                throw CompilerException.format("Cannot deduce type of %s literal", node, literalType.name);
            }
            var generics = expected[0].getGenerics();
            return new TypeObject[] {literalCls.generify(node, generics.toArray(new TypeObject[0]))};
        } else {
            return new TypeObject[]{literalCls.generify(returnTypes(node.getBuilders())).makeMut()};
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
            if (retCount > 1) {
                throw CompilerException.format("Literal returns 1 value, expected %d", node, retCount);
            }
            var retType = returnTypes(node.getBuilders());
            for (var value : node.getBuilders()) {
                bytes.addAll(TestConverter.bytesMaybeOption(start + bytes.size(), value, info, 1, retType));
            }
            if (literalType != LiteralType.TUPLE) {
                var genericType = returnTypes(node.getBuilders());
                bytes.add(Bytecode.LOAD_CONST.value);
                bytes.addAll(Util.shortToBytes(info.constIndex(info.typeConstant(node, genericType))));
            }
            bytes.add(literalType.bytecode.value);
            bytes.addAll(Util.shortToBytes((short) node.getBuilders().length));
        }
        return bytes;
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
        var genericType = returnTypes(node.getBuilders());
        bytes.add(Bytecode.LOAD_CONST.value);
        bytes.addAll(Util.shortToBytes(info.constIndex(info.typeConstant(node, genericType))));
        bytes.add(literalType.bytecode.value);
        bytes.addAll(Util.shortZeroBytes());
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
}
