package main.java.converter;

import main.java.parser.EscapedOperatorNode;
import main.java.parser.OpSpTypeNode;
import main.java.parser.VariableNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class DerivedOperatorConverter implements BaseConverter {
    private final DerivedOperatorNode node;
    private final CompilerInfo info;

    public DerivedOperatorConverter(CompilerInfo info, DerivedOperatorNode node) {
        this.node = node;
        this.info = info;
    }

    @Override
    @NotNull
    public List<Byte> convert(int start) {
        var op = node.getOperator();
        if (op instanceof EscapedOperatorNode) {
            switch (((EscapedOperatorNode) op).getOperator()) {
                case EQUALS:
                    return convertEquals(start);
                case COMPARE:
                    throw CompilerTodoError.of("$derive(\\<=>)", node);
                default:
                    throw CompilerException.of(
                            "Invalid derived operator: Can only derive ==, <=>, repr, and hash operators", node
                    );
            }
        } else if (op instanceof VariableNode) {
            switch (((VariableNode) op).getName()) {
                case "hash":
                    return convertHash();
                case "repr":
                    throw CompilerTodoError.of("$derive(repr)", node);
                default:
                    throw CompilerException.of(
                            "Invalid derived operator: Can only derive ==, <=>, repr, and hash operators", node
                    );
            }
        } else {
            throw CompilerException.of("Invalid derived operator", node);
        }
    }

    private List<Byte> convertEquals(int start) {
        var type = info.getType("self").orElseThrow();
        assert type instanceof UserType;
        List<Byte> bytes = new ArrayList<>();
        bytes.add(Bytecode.LOAD_VALUE.value);
        bytes.addAll(Util.shortZeroBytes());  // self
        bytes.add(Bytecode.GET_TYPE.value);
        bytes.add(Bytecode.LOAD_VALUE.value);
        bytes.addAll(Util.shortToBytes((short) 1));  // other
        bytes.add(Bytecode.GET_TYPE.value);
        bytes.add(Bytecode.EQUAL.value);
        bytes.addAll(postJumpBytes(start + bytes.size()));
        for (var field : ((UserType<?>) type).getFields()) {
            bytes.add(Bytecode.LOAD_VALUE.value);
            bytes.addAll(Util.shortZeroBytes());  // self
            bytes.add(Bytecode.LOAD_DOT.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(field))));
            bytes.add(Bytecode.LOAD_VALUE.value);
            bytes.addAll(Util.shortToBytes((short) 1));  // other
            bytes.add(Bytecode.LOAD_DOT.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(field))));
            bytes.add(Bytecode.EQUAL.value);
            bytes.addAll(postJumpBytes(start + bytes.size()));
        }
        return bytes;
    }

    private List<Byte> postJumpBytes(int start) {
        List<Byte> bytes = new ArrayList<>(postJumpSize());
        bytes.add(Bytecode.JUMP_TRUE.value);
        bytes.addAll(Util.shortZeroBytes());
        int jump = bytes.size();
        bytes.add(Bytecode.LOAD_CONST.value);
        bytes.addAll(Util.shortToBytes(info.constIndex(Builtins.FALSE)));
        bytes.add(Bytecode.RETURN.value);
        bytes.addAll(Util.shortToBytes((short) 1));
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jump);
        return bytes;
    }

    private int postJumpSize() {
        return Bytecode.JUMP_TRUE.size() + Bytecode.LOAD_CONST.size() + Bytecode.RETURN.size();
    }

    private List<Byte> convertHash() {
        var type = info.getType("self").orElseThrow();
        assert type instanceof UserType;
        List<Byte> bytes = new ArrayList<>();
        int fieldCount = 0;
        for (var field : ((UserType<?>) type).getFields()) {
            var fieldType = type.attrType(field, AccessLevel.PRIVATE).orElseThrow();
            if (fieldType.operatorInfo(OpSpTypeNode.HASH, AccessLevel.PRIVATE).isEmpty()) {
                throw CompilerException.format(
                        "Cannot derive hash for type '%s': " +
                                "Field %s has type '%s', which does not implement a hash operator",
                        node, type.name(), field, fieldType
                );
            }
            bytes.add(Bytecode.LOAD_VALUE.value);
            bytes.addAll(Util.shortZeroBytes());
            bytes.add(Bytecode.LOAD_DOT.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(field))));
            fieldCount++;
        }
        bytes.add(Bytecode.PACK_TUPLE.value);
        bytes.addAll(Util.shortToBytes((short) fieldCount));
        bytes.add(Bytecode.CALL_OP.value);
        bytes.addAll(Util.shortToBytes((short) OpSpTypeNode.HASH.ordinal()));
        bytes.addAll(Util.shortZeroBytes());
        return bytes;
    }
}
