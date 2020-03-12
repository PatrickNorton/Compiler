package main.java.converter;

import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public enum Bytecode {
    NOP(0x0),
    LOAD_NULL(0x1),
    LOAD_CONST(0x2, Type.CONSTANT),
    LOAD_VALUE(0x3, Type.VARIABLE),
    LOAD_DOT(0x4, Type.CONSTANT),
    LOAD_SUBSCRIPT(0x5, Type.ARGC),
    LOAD_OP(0x6, Type.OPERATOR),
    POP_TOP(0x7),
    DUP_TOP(0x8),
    SWAP_2(0x9),
    SWAP_3(0xA),
    SWAP_N(0xB, Type.ARGC),
    STORE(0xC, Type.VARIABLE),
    STORE_SUBSCRIPT(0xD, Type.ARGC),
    STORE_ATTR(0xE, Type.CONSTANT),
    // Binary operators
    PLUS(0x10),
    MINUS(0x11),
    TIMES(0x12),
    DIVIDE(0x13),
    MOD(0x14),
    SUBSCRIPT(0x15),
    POWER(0x16),
    L_BITSHIFT(0x17),
    R_BITSHIFT(0x18),
    BITWISE_AND(0x19),
    BITWISE_OR(0x1A),
    BITWISE_XOR(0x1B),
    COMPARE(0x1C),
    DEL_SUBSCRIPT(0x1D),
    U_MINUS(0x1E),
    BITWISE_NOT(0x1F),
    BOOL_AND(0x20),
    BOOL_OR(0x21),
    BOOL_NOT(0x22),
    BOOL_XOR(0x23),
    IDENTICAL(0x24),
    INSTANCEOF(0x25),
    CALL_OP(0x26, Type.OPERATOR, Type.ARGC),
    PACK_TUPLE(0x27),
    UNPACK_TUPLE(0x28),
    EQUAL(0x29),
    LESS_THAN(0x2A),
    GREATER_THAN(0x2B),
    LESS_EQUAL(0x2C),
    GREATER_EQUAL(0x2D),
    CONTAINS(0x2E),
    // Jumps, etc.
    JUMP(0x30, Type.LOCATION),
    JUMP_FALSE(0x31, Type.LOCATION),
    JUMP_TRUE(0x32, Type.LOCATION),
    JUMP_NN(0x33, Type.LOCATION),
    JUMP_NULL(0x34, Type.LOCATION),
    CALL_METHOD(0x35, Type.ARGC),
    CALL_TOS(0x36, Type.ARGC),
    TAIL_METHOD(0x37, Type.ARGC),
    TAIL_TOS(0x38, Type.ARGC),
    RETURN(0x39, Type.ARGC),
    THROW(0x3A),
    THROW_QUICK(0x3B, Type.ARGC),
    ENTER_TRY(0x3C, Type.LOCATION),
    EXCEPT_N(0x3D, Type.ARGC),
    FINALLY(0x3E),
    END_TRY(0x3F, Type.ARGC),
    // Markers
    FUNC_DEF(0x40),
    CLASS_DEF(0x41),
    END_CLASS(0x42),
    // Loop stuff
    FOR_ITER(0x50, Type.LOCATION),
    LIST_CREATE(0x51, Type.ARGC),
    SET_CREATE(0x52, Type.ARGC),
    DICT_CREATE(0x53, Type.ARGC),
    LIST_ADD(0x54),
    SET_ADD(0x55),
    DICT_ADD(0x56),
    DOTIMES(0x57, Type.LOCATION),
    ;

    private enum Type {
        VARIABLE(2),
        CONSTANT(2),
        LOCATION(4),
        ARGC(2),
        OPERATOR(2),
        ;
        final byte byteCount;

        Type(int bytes) {
            byteCount = (byte) bytes;
        }
    }

    public final byte value;
    private final Type[] operands;
    private final int sum;

    @Contract(pure = true)
    Bytecode(int value, @NotNull Type... operands) {
        this.value = (byte) value;
        this.operands = operands;
        int sum = 0;
        for (Type t : operands) {
            sum += t.byteCount;
        }
        this.sum = sum;
    }

    static {
        // Ensure no two bytes are the same
        assert Arrays.stream(values())
                .mapToInt(b -> b.value)
                .distinct().count() == values().length
                : "Not all values distinct in bytecode";
    }

    public int size() {
        return sum + 1;
    }

    private static final Map<Byte, Bytecode> VALUE_MAP;

    static {
        Map<Byte, Bytecode> temp = new HashMap<>();
        for (var value : values()) {
            temp.put(value.value, value);
        }
        VALUE_MAP = Collections.unmodifiableMap(temp);
    }

    @NotNull
    static String disassemble(CompilerInfo info, @NotNull List<Byte> bytes) {
        var sb = new StringBuilder();
        for (int i = 0; i < bytes.size();) {
            var op = VALUE_MAP.get(bytes.get(i++));
            if (op.operands.length > 0) {
                 sb.append(String.format("%-7d%-16s", i - 1, op));
                StringJoiner sj = new StringJoiner(", ");
                for (var operand : op.operands) {
                    var operandSize = operand.byteCount;
                    var value = fromBytes(bytes.subList(i, i + operandSize));
                    i += operandSize;
                    switch (operand) {
                        case ARGC:
                        case LOCATION:
                        case VARIABLE:
                            sj.add(Integer.toString(value));
                            break;
                        case CONSTANT:
                            sj.add(String.format("%d (%s)", value, info.getConstant((short) value).name()));
                            break;
                        case OPERATOR:
                            sj.add(String.format("%d (%s)", value, OpSpTypeNode.values()[value]));
                            break;
                    }
                }
                sb.append(sj);
                sb.append("\n");
            } else {
                sb.append(String.format("%-7d%s%n", i, op));
            }
        }
        return sb.toString();
    }

    private static int fromBytes(@NotNull List<Byte> bytes) {
        int total = 0;
        for (int i = 0; i < bytes.size(); i++) {
            total |= byteToInt(bytes.get(i)) << Byte.SIZE * (bytes.size() - i - 1);
        }
        return total;
    }

    private static int byteToInt(byte value) {
        return value < 0 ? value + 256 : value;
    }
}
