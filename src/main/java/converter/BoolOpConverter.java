package main.java.converter;

import main.java.converter.bytecode.ArgcBytecode;
import main.java.converter.bytecode.ConstantBytecode;
import main.java.parser.ArgumentNode;
import main.java.parser.Lined;
import main.java.parser.OperatorTypeNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class BoolOpConverter extends OperatorConverter {
    private final OperatorTypeNode op;
    private final ArgumentNode[] args;
    private final Lined lineInfo;
    private final CompilerInfo info;
    private final int retCount;

    public BoolOpConverter(
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
        return switch (op) {
            case BOOL_NOT -> boolNotConst();
            case BOOL_AND -> boolAndConst();
            case BOOL_OR -> boolOrConst();
            case BOOL_XOR -> boolXorConst();
            default -> throw CompilerInternalError.format("Unknown boolean operator: %s", lineInfo, op);
        };
    }

    @Override
    @NotNull
    public TypeObject[] returnType() {
        return new TypeObject[] {Builtins.bool()};
    }

    @Override
    @NotNull
    public BytecodeList convert() {
        return switch (op) {
            case BOOL_AND, BOOL_OR -> convertBoolOp();
            case BOOL_NOT -> convertBoolNot();
            case BOOL_XOR -> convertBoolXor();
            default -> throw CompilerInternalError.format("Invalid boolean operator %s", lineInfo, op);
        };
    }

    @NotNull
    private BytecodeList convertBoolOp() {
        assert op == OperatorTypeNode.BOOL_AND || op == OperatorTypeNode.BOOL_OR;
        var bytes = new BytecodeList();
        bytes.add(Bytecode.LOAD_CONST, new ConstantBytecode(Builtins.boolConstant(), info));
        bytes.addAll(TestConverter.bytes(args[0].getArgument(), info, 1));
        bytes.add(Bytecode.DUP_TOP);
        var label = info.newJumpLabel();
        var bytecode = op == OperatorTypeNode.BOOL_OR ? Bytecode.JUMP_TRUE : Bytecode.JUMP_FALSE;
        bytes.add(bytecode, label);
        addPostJump(bytes, label);
        bytes.add(Bytecode.CALL_TOS, ArgcBytecode.one());
        if (retCount == 0) {
            bytes.add(Bytecode.POP_TOP);
        }
        return bytes;
    }

    private BytecodeList convertBoolNot() {
        assert op == OperatorTypeNode.BOOL_NOT;
        if (args.length > 1) {
            throw CompilerException.format(
                    "'not' operator cannot have multiple operands, got %d",
                    lineInfo, args.length
            );
        }
        var bytes = new BytecodeList(TestConverter.bytes(args[0].getArgument(), info, 1));
        bytes.add(Bytecode.BOOL_NOT);
        return bytes;
    }

    private BytecodeList convertBoolXor() {
        assert op == OperatorTypeNode.BOOL_XOR;
        if (args.length != 2) {
            throw CompilerException.format("'xor' operator must have 2 operands, got %d", lineInfo, args.length);
        }
        var bytes = new BytecodeList(TestConverter.bytes(args[0].getArgument(), info, 1));
        bytes.addAll(TestConverter.bytes(args[1].getArgument(), info, 1));
        bytes.add(Bytecode.BOOL_XOR);
        return bytes;
    }

    private void addPostJump(@NotNull BytecodeList bytes, Label label) {
        bytes.add(Bytecode.POP_TOP);
        bytes.addAll(TestConverter.bytes(args[1].getArgument(), info, 1));
        bytes.addLabel(label);
    }

    private Optional<LangConstant> boolAndConst() {
        assert op == OperatorTypeNode.BOOL_AND;
        var values = boolValues(args);
        if (values.isPresent()) {
            for (var value : values.orElseThrow()) {
                if (!value) {
                    return Optional.of(Builtins.FALSE);
                }
            }
            return Optional.of(Builtins.TRUE);
        }
        return Optional.empty();
    }

    private Optional<LangConstant> boolOrConst() {
        assert op == OperatorTypeNode.BOOL_OR;
        var values = boolValues(args);
        if (values.isPresent()) {
            for (var value : values.orElseThrow()) {
                if (value) {
                    return Optional.of(Builtins.TRUE);
                }
            }
            return Optional.of(Builtins.FALSE);
        }
        return Optional.empty();
    }

    private Optional<LangConstant> boolXorConst() {
        assert op == OperatorTypeNode.BOOL_XOR;
        if (args.length != 2) {
            return Optional.empty();
        }
        var values = boolValues(args);
        if (values.isPresent() && values.orElseThrow().length == 2) {
            var booleans = values.orElseThrow();
            return Optional.of(LangConstant.of(booleans[0] ^ booleans[1]));
        }
        return Optional.empty();
    }

    private Optional<boolean[]> boolValues(ArgumentNode[] operands) {
        var boolValues = new boolean[operands.length];
        for (int i = 0; i < boolValues.length; i++) {
            var opConst = TestConverter.constantReturn(operands[i].getArgument(), info, retCount);
            if (opConst.isPresent()) {
                var boolVal = opConst.orElseThrow().boolValue();
                if (boolVal.isPresent()) {
                    boolValues[i] = boolVal.orElseThrow();
                } else {
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(boolValues);
    }

    private Optional<LangConstant> boolNotConst() {
        if (args.length > 1) {
            return Optional.empty();
        } else {
            var constant = TestConverter.constantReturn(args[0].getArgument(), info, retCount);
            return constant.flatMap(x -> x.boolValue().mapValues(Builtins.FALSE, Builtins.TRUE));
        }
    }

    @Override
    @NotNull
    protected Pair<BytecodeList, TypeObject> convertWithAs() {
        throw asException(lineInfo);
    }
}
