package main.java.converter;

import main.java.parser.ArgumentNode;
import main.java.parser.Lined;
import main.java.parser.OperatorTypeNode;
import main.java.util.Pair;

import java.util.ArrayList;
import java.util.List;
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
        switch (op) {
            case BOOL_NOT:
                return boolNotConst();
            case BOOL_AND:
                return boolAndConst();
            case BOOL_OR:
                return boolOrConst();
            case BOOL_XOR:
                return boolXorConst();
        }
        return Optional.empty();
    }

    @Override
    public TypeObject[] returnType() {
        return new TypeObject[] {Builtins.BOOL};
    }

    @Override
    public List<Byte> convert(int start) {
        switch (op) {
            case BOOL_AND:
            case BOOL_OR:
                return convertBoolOp(start);
            case BOOL_NOT:
                return convertBoolNot(start);
            case BOOL_XOR:
                return convertBoolXor(start);
            default:
                throw CompilerInternalError.format("Invalid boolean operator %s", lineInfo, op);
        }
    }

    private List<Byte> convertBoolOp(int start) {
        assert op == OperatorTypeNode.BOOL_AND || op == OperatorTypeNode.BOOL_OR;
        List<Byte> bytes = new ArrayList<>();
        bytes.add(Bytecode.LOAD_CONST.value);
        bytes.addAll(Util.shortToBytes(info.constIndex(Builtins.TRUE)));
        bytes.addAll(TestConverter.bytes(start, args[0].getArgument(), info, 1));
        bytes.add(Bytecode.DUP_TOP.value);
        var bytecode = op == OperatorTypeNode.BOOL_OR ? Bytecode.JUMP_FALSE : Bytecode.JUMP_TRUE;
        bytes.add(bytecode.value);
        addPostJump(start, bytes);
        bytes.add(Bytecode.CALL_TOS.value);
        bytes.addAll(Util.shortToBytes((short) 1));
        if (retCount == 0) {
            bytes.add(Bytecode.POP_TOP.value);
        }
        return bytes;
    }

    private List<Byte> convertBoolNot(int start) {
        assert op == OperatorTypeNode.BOOL_NOT;
        if (args.length > 1) {
            throw CompilerException.format("'not' operator cannot have multiple operands, got %d", lineInfo, args.length);
        }
        List<Byte> bytes = new ArrayList<>(TestConverter.bytes(start, args[0].getArgument(), info, 1));
        bytes.add(Bytecode.BOOL_NOT.value);
        return bytes;
    }

    private List<Byte> convertBoolXor(int start) {
        assert op == OperatorTypeNode.BOOL_XOR;
        if (args.length != 2) {
            throw CompilerException.format("'xor' operator must have 2 operands, got %d", lineInfo, args.length);
        }
        List<Byte> bytes = new ArrayList<>(TestConverter.bytes(start, args[0].getArgument(), info, 1));
        bytes.addAll(TestConverter.bytes(start, args[1].getArgument(), info, 1));
        bytes.add(Bytecode.BOOL_XOR.value);
        return bytes;
    }

    private void addPostJump(int start,List<Byte> bytes) {
        int jumpPos = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        bytes.add(Bytecode.POP_TOP.value);
        bytes.addAll(TestConverter.bytes(start + bytes.size(), args[1].getArgument(), info, 1));
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jumpPos);
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
        if (values.isPresent()) {
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
    protected Pair<List<Byte>, TypeObject> convertWithAs(int start) {
        throw asException(lineInfo);
    }
}
