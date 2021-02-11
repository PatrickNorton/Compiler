package main.java.converter;

import main.java.parser.ArgumentNode;
import main.java.parser.Lined;
import main.java.parser.OperatorTypeNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class NormalOperatorConverter extends OperatorConverter {
    private final OperatorTypeNode op;
    private final ArgumentNode[] args;
    private final Lined lineInfo;
    private final CompilerInfo info;
    private final int retCount;

    public NormalOperatorConverter(
            OperatorTypeNode op,
            ArgumentNode[] args,
            Lined lineInfo,
            CompilerInfo info,
            int retCount
    ) {
        this.op = op;
        this.args = args;
        this.lineInfo = lineInfo;
        this.info = info;
        this.retCount = retCount;
    }

    @Override
    public Optional<LangConstant> constantReturn() {
        return defaultConstant(op, info, args);
    }

    @Override
    @NotNull
    public TypeObject[] returnType() {
        var firstOpConverter = TestConverter.of(info, args[0].getArgument(), 1);
        var retType = firstOpConverter.returnType()[0].operatorReturnType(op, info);
        return retType.orElseGet(() -> new TypeObject[]{Builtins.throwsType()});
    }

    @Override
    @NotNull
    public List<Byte> convert(int start) {
        var constant = constantReturn();
        if (constant.isPresent()) {
            return loadConstant(info, constant.orElseThrow());
        }
        if (op == OperatorTypeNode.NOT_EQUALS) {
            return convertNotEquals(start);
        }
        List<Byte> bytes = new ArrayList<>();
        int opCount = args.length;
        TypeObject opType = null;
        ArgumentNode previousArg = null;
        for (var arg : args) {
            var converter = TestConverter.of(info, arg.getArgument(), 1);
            var retTypes = converter.returnType();
            if (retTypes.length == 0) {
                throw CompilerException.of("Cannot use return type of function with 0 returns", arg);
            }
            var retType = retTypes[0];
            if (opType != null && opType.operatorReturnType(op, info).isEmpty()) {
                throw CompilerException.format(
                        "'%s' returns type '%s', which has no overloaded '%s'",
                        previousArg, previousArg, opType.name(), op
                );
            }
            opType = opType == null ? retType : opType.operatorReturnType(op, info).orElseThrow()[0];
            previousArg = arg;
            bytes.addAll(TestConverter.bytes(start + bytes.size(), arg.getArgument(), info, 1));
        }
        var bytecode = BYTECODE_MAP.get(op);
        if (opCount == (op.isUnary() ? 1 : 2)) {
            bytes.add(bytecode.value);
        } else if (MANDATORY_ARG_COUNT.contains(op)) {
            throw CompilerException.format(
                    "Cannot call operator '%s' with %d operands (expected exactly %d)",
                    lineInfo, op, opCount, op.isUnary() ? 1 : 2
            );
        } else {
            throw CompilerTodoError.of("Operators with > 2 operands not yet supported", lineInfo);
        }
        if (retCount == 0) {
            bytes.add(Bytecode.POP_TOP.value);
        }
        return bytes;
    }

    @NotNull
    private List<Byte> convertNotEquals(int start) {
        int opCount = args.length;
        assert opCount == 2 && op == OperatorTypeNode.NOT_EQUALS;
        List<Byte> bytes = new ArrayList<>(TestConverter.bytes(start, args[0].getArgument(), info, 1));
        bytes.addAll(TestConverter.bytes(start + bytes.size(), args[1].getArgument(), info, 1));
        if (opCount == (op.isUnary() ? 1 : 2)) {
            bytes.add(Bytecode.EQUAL.value);
        } else {
            throw CompilerTodoError.of("Operators with > 2 operands not yet supported", lineInfo);
        }
        bytes.add(Bytecode.BOOL_NOT.value);
        if (retCount == 0) {
            bytes.add(Bytecode.POP_TOP.value);
        }
        return bytes;
    }

    @Override
    @NotNull
    protected Pair<List<Byte>, TypeObject> convertWithAs(int start) {
        throw asException(lineInfo);
    }
}
