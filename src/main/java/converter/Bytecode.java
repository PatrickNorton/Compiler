package main.java.converter;

import main.java.converter.bytecode.ArgcBytecode;
import main.java.converter.bytecode.BytecodeValue;
import main.java.converter.bytecode.ConstantBytecode;
import main.java.converter.bytecode.FunctionNoBytecode;
import main.java.converter.bytecode.LocationBytecode;
import main.java.converter.bytecode.OperatorBytecode;
import main.java.converter.bytecode.StackPosBytecode;
import main.java.converter.bytecode.SyscallBytecode;
import main.java.converter.bytecode.TableNoBytecode;
import main.java.converter.bytecode.VariableBytecode;
import main.java.converter.bytecode.VariantBytecode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    SWAP_N(0xB, Type.STACK_POS),
    STORE(0xC, Type.VARIABLE),
    STORE_SUBSCRIPT(0xD, Type.ARGC),
    STORE_ATTR(0xE, Type.CONSTANT),
    SWAP_STACK(0xF, Type.STACK_POS, Type.STACK_POS),
    // Binary operators
    PLUS(0x10),
    MINUS(0x11),
    TIMES(0x12),
    DIVIDE(0x13),
    FLOOR_DIV(0x14),
    MOD(0x15),
    SUBSCRIPT(0x16),
    POWER(0x17),
    L_BITSHIFT(0x18),
    R_BITSHIFT(0x19),
    BITWISE_AND(0x1A),
    BITWISE_OR(0x1B),
    BITWISE_XOR(0x1C),
    COMPARE(0x1D),
    DEL_SUBSCRIPT(0x1E),
    U_MINUS(0x1F),
    BITWISE_NOT(0x20),
    BOOL_AND(0x21),
    BOOL_OR(0x22),
    BOOL_NOT(0x23),
    BOOL_XOR(0x24),
    IDENTICAL(0x25),
    INSTANCEOF(0x26),
    CALL_OP(0x27, Type.OPERATOR, Type.ARGC),
    PACK_TUPLE(0x28, Type.ARGC),
    UNPACK_TUPLE(0x29),
    EQUAL(0x2A),
    LESS_THAN(0x2B),
    GREATER_THAN(0x2C),
    LESS_EQUAL(0x2D),
    GREATER_EQUAL(0x2E),
    CONTAINS(0x2F),
    // Jumps, etc.
    JUMP(0x30, Type.LOCATION),
    JUMP_FALSE(0x31, Type.LOCATION),
    JUMP_TRUE(0x32, Type.LOCATION),
    JUMP_NN(0x33, Type.LOCATION),
    JUMP_NULL(0x34, Type.LOCATION),
    CALL_METHOD(0x35, Type.CONSTANT, Type.ARGC),
    CALL_TOS(0x36, Type.ARGC),
    CALL_FN(0x37, Type.FUNCTION_NO, Type.ARGC),
    TAIL_METHOD(0x38, Type.ARGC),
    TAIL_TOS(0x39, Type.ARGC),
    TAIL_FN(0x3A, Type.FUNCTION_NO, Type.ARGC),
    RETURN(0x3B, Type.ARGC),
    YIELD(0x3C, Type.ARGC),
    SWITCH_TABLE(0x3D, Type.TABLE_NO),
    // Exception stuff
    THROW(0x40),
    THROW_QUICK(0x41, Type.ARGC),
    ENTER_TRY(0x42, Type.LOCATION),
    EXCEPT_N(0x43, Type.ARGC),
    FINALLY(0x44),
    END_TRY(0x45, Type.ARGC),
    // Markers
    FUNC_DEF(0x48),
    CLASS_DEF(0x49),
    END_CLASS(0x4A),
    // Loop stuff
    FOR_ITER(0x50, Type.LOCATION, Type.ARGC),
    LIST_CREATE(0x51, Type.ARGC),
    SET_CREATE(0x52, Type.ARGC),
    DICT_CREATE(0x53, Type.ARGC),
    LIST_ADD(0x54),
    SET_ADD(0x55),
    DICT_ADD(0x56),
    DOTIMES(0x57, Type.LOCATION),
    FOR_PARALLEL(0x58, Type.LOCATION, Type.ARGC),
    MAKE_SLICE(0x59),
    LIST_DYN(0x5A),
    SET_DYN(0x5B),
    DICT_DYN(0x5C),
    // Statics
    DO_STATIC(0x60, Type.LOCATION),
    STORE_STATIC(0x61, Type.VARIABLE),
    LOAD_STATIC(0x62, Type.VARIABLE),
    // Union/Option stuff
    GET_VARIANT(0x68, Type.VARIANT),
    MAKE_VARIANT(0x69, Type.VARIANT),
    VARIANT_NO(0x6A),
    MAKE_OPTION(0x6B),
    IS_SOME(0x6C),
    UNWRAP_OPTION(0x6D),
    // Misc.
    MAKE_FUNCTION(0x70, Type.FUNCTION_NO),
    GET_TYPE(0x71),
    GET_SYS(0x72, Type.SYSCALL_NO),
    SYSCALL(0x73, Type.SYSCALL_NO, Type.ARGC),
    // Dups, part 2 (maybe realign?)
    DUP_TOP_2(0x78),
    DUP_TOP_N(0x79, Type.ARGC),
    UNPACK_ITERABLE(0x7A),
    PACK_ITERABLE(0x7B),
    SWAP_DYN(0x7C),
    ;

    private enum Type {
        VARIABLE(2),
        CONSTANT(2),
        LOCATION(4),
        ARGC(2),
        OPERATOR(2),
        FUNCTION_NO(2),
        STACK_POS(2),
        TABLE_NO(2),
        SYSCALL_NO(2),
        VARIANT(2),
        ;
        final byte byteCount;

        Type(int bytes) {
            byteCount = (byte) bytes;
        }

        private boolean matches(BytecodeValue value) {
            return switch (this) {
                case VARIABLE -> value instanceof VariableBytecode;
                case CONSTANT -> value instanceof ConstantBytecode;
                case LOCATION -> value instanceof LocationBytecode;
                case ARGC -> value instanceof ArgcBytecode;
                case OPERATOR -> value instanceof OperatorBytecode;
                case FUNCTION_NO -> value instanceof FunctionNoBytecode;
                case STACK_POS -> value instanceof StackPosBytecode;
                case TABLE_NO -> value instanceof TableNoBytecode;
                case SYSCALL_NO -> value instanceof SyscallBytecode;
                case VARIANT -> value instanceof VariantBytecode;
            };
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

    public int operandCount() {
        return operands.length;
    }

    public boolean operandsMatch() {
        return operands.length == 0;
    }

    public boolean operandsMatch(BytecodeValue firstParam) {
        return operands.length == 1 && operands[0].matches(firstParam);
    }

    public boolean operandsMatch(BytecodeValue firstParam, BytecodeValue secondParam) {
        return operands.length == 2 && operands[0].matches(firstParam) && operands[1].matches(secondParam);
    }

    @NotNull
    public List<Byte> assemble(BytecodeValue firstParam, BytecodeValue secondParam) {
        List<Byte> bytes = new ArrayList<>(size());
        bytes.add(value);
        switch (operands.length) {
            case 0:
                break;
            case 1:
                firstParam.writeBytes(bytes);
                break;
            case 2:
                firstParam.writeBytes(bytes);
                secondParam.writeBytes(bytes);
                break;
            default:
                throw new UnsupportedOperationException("Unknown operand count");
        }
        return bytes;
    }
}
