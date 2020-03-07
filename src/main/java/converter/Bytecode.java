package main.java.converter;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public enum Bytecode {
    NOP(0x0, 0),
    LOAD_NULL(0x1, 0),
    LOAD_CONST(0x2, 2),
    LOAD_VALUE(0x3, 2),
    LOAD_DOT(0x4, 2),
    LOAD_SUBSCRIPT(0x5, 2),
    LOAD_OP(0x6, 2),
    POP_TOP(0x7, 0),
    DUP_TOP(0x8, 0),
    SWAP_2(0x9, 0),
    SWAP_3(0xA, 0),
    SWAP_N(0xB, 4),
    STORE(0xC, 2),
    STORE_SUBSCRIPT(0xD, 2),
    STORE_ATTR(0xE, 2),
    // Binary operators
    PLUS(0x10, 0),
    MINUS(0x11, 0),
    TIMES(0x12, 0),
    DIVIDE(0x13, 0),
    MOD(0x14, 0),
    SUBSCRIPT(0x15, 0),
    POWER(0x16, 0),
    L_BITSHIFT(0x17, 0),
    R_BITSHIFT(0x18, 0),
    BITWISE_AND(0x19, 0),
    BITWISE_OR(0x1A, 0),
    BITWISE_XOR(0x1B, 0),
    COMPARE(0x1C, 0),
    DEL_SUBSCRIPT(0x1D, 0),
    U_MINUS(0x1E, 0),
    BITWISE_NOT(0x1F, 0),
    BOOL_AND(0x20, 0),
    BOOL_OR(0x21, 0),
    BOOL_NOT(0x22, 0),
    BOOL_XOR(0x23, 0),
    IDENTICAL(0x24, 0),
    INSTANCEOF(0x25, 0),
    CALL_OP(0x26, 2, 2),
    PACK_TUPLE(0x27, 0),
    UNPACK_TUPLE(0x28, 0),
    EQUAL(0x29, 0),
    LESS_THAN(0x2A, 0),
    GREATER_THAN(0x2B, 0),
    LESS_EQUAL(0x2C, 0),
    GREATER_EQUAL(0x2D, 0),
    // Jumps, etc.
    JUMP(0x30, 4),
    JUMP_FALSE(0x31, 4),
    JUMP_TRUE(0x32, 4),
    JUMP_NN(0x33, 4),
    JUMP_NULL(0x34, 4),
    CALL_METHOD(0x35, 2),
    CALL_TOS(0x36, 2),
    TAIL_METHOD(0x37, 2),
    TAIL_TOS(0x38, 2),
    RETURN(0x39, 2),
    THROW(0x3A, 0),
    THROW_QUICK(0x3B, 2),
    ENTER_TRY(0x3C, 4),
    EXCEPT_N(0x3D, 2),
    FINALLY(0x3E, 0),
    END_TRY(0x3F, 2),
    // Markers
    FUNC_DEF(0x40, 0),
    CLASS_DEF(0x41, 0),
    END_CLASS(0x42, 0),
    // Loop stuff
    FOR_ITER(0x50, 4),
    LIST_CREATE(0x51, 2),
    SET_CREATE(0x52, 2),
    DICT_CREATE(0x53, 2),
    LIST_ADD(0x54, 0),
    SET_ADD(0x55, 0),
    DICT_ADD(0x56, 0),
    DOTIMES(0x57, 4),
    ;

    public final byte value;
    private final int[] operands;
    private final int sum;

    Bytecode(int value, int... operands) {
        this((byte) value, operands);
    }

    @Contract(pure = true)
    Bytecode(byte value, int... operands) {
        this.value = value;
        this.operands = operands;
        this.sum = Util.sum(operands);
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
    static String disassemble(@NotNull List<Byte> bytes) {
        var sb = new StringBuilder();
        for (int i = 0; i < bytes.size();) {
            var op = VALUE_MAP.get(bytes.get(i++));
            if (op.operands.length > 0) {
                 sb.append(String.format("%-7d%-16s", i - 1, op));
                StringJoiner sj = new StringJoiner(", ");
                for (var operandSize : op.operands) {
                    var value = fromBytes(bytes.subList(i, i + operandSize));
                    i += operandSize;
                    sj.add(Integer.toString(value));
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
