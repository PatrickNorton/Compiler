package main.java.converter;

import main.java.parser.FormattedStringNode;
import main.java.parser.FormattedStringNode.FormatInfo;
import main.java.parser.Lined;
import main.java.parser.OpSpTypeNode;
import main.java.parser.TestNode;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class FormattedStringConverter implements TestConverter {
    private final FormattedStringNode node;
    private final CompilerInfo info;
    private final int retCount;

    public FormattedStringConverter(CompilerInfo info, FormattedStringNode node, int retCount) {
        this.info = info;
        this.node = node;
        this.retCount = retCount;
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        return new TypeObject[] {Builtins.str()};
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        if (retCount == 0) {
            CompilerWarning.warn("Unused formatted string", WarningType.UNUSED, info, node);
        } else if (retCount > 1) {
            throw CompilerException.format("F-string literal only returns one value, not %d", node, retCount);
        }
        List<Byte> bytes = new ArrayList<>();
        var strings = node.getStrings();
        var tests = node.getTests();
        assert strings.length == tests.length || strings.length == tests.length + 1;
        if (tests.length == 0) {
            CompilerWarning.warn("F-string with no formatted arguments", WarningType.TRIVIAL_VALUE, info, node);
        }
        for (int i = 0; i < strings.length; i++) {
            if (i < tests.length) {
                var format = node.getFormats()[i];
                var strValue = argConstant(tests[i], format);
                if (!format.isEmpty() && strValue.isPresent()) {
                    var string = strings[i] + strValue.orElseThrow();
                    bytes.add(Bytecode.LOAD_CONST.value);
                    bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(string))));
                } else {
                    bytes.add(Bytecode.LOAD_CONST.value);
                    var constValue = LangConstant.of(strings[i]);
                    bytes.addAll(Util.shortToBytes(info.constIndex(constValue)));
                    if (i != 0) {
                        bytes.add(Bytecode.PLUS.value);
                    }
                    convertArgument(tests[i], start, bytes, format);
                    bytes.add(Bytecode.PLUS.value);
                }
            } else {
                bytes.add(Bytecode.LOAD_CONST.value);
                var constValue = LangConstant.of(strings[i]);
                bytes.addAll(Util.shortToBytes(info.constIndex(constValue)));
                if (i != 0) {
                    bytes.add(Bytecode.PLUS.value);
                }
            }
        }
        if (retCount == 0) {
            bytes.add(Bytecode.POP_TOP.value);
        }
        return bytes;
    }

    private Optional<String> argConstant(TestNode arg, FormattedStringNode.FormatInfo format) {
        if (!format.onlyType()) {
            return Optional.empty();
        }
        if (!format.isEmpty()) {
            var converter = TestConverter.of(info, arg, retCount);
            switch (format.getType()) {
                case 's':
                    return converter.constantReturn().flatMap(LangConstant::strValue);
                case 'r':
                    return converter.constantReturn().flatMap(LangConstant::reprValue);
                case 'n':
                case 'd':
                    return decimalConstant(converter);
                case 'x':
                    return baseConstant(converter, 16);
                case 'o':
                    return baseConstant(converter, 8);
                case 'b':
                    return baseConstant(converter, 2);
                case 'c':
                    return charConstant(converter);
                case 'X':
                    return upperHexConstant(converter);
                case 'e':
                    return expConstant(converter);
                case 'E':
                    return upperExpConstant(converter);
                case 'f':
                    return fixedConstant(converter);
                case 'F':
                    return upperFixedConstant(converter);
                case 'g':
                    return generalFloatConstant(converter);
                case 'G':
                    return upperGeneralConstant(converter);
                case '%':
                    return percentConstant(converter);
                default:
                    return Optional.empty();
            }
        } else {
            var converter = TestConverter.of(info, arg, retCount);
            return converter.constantReturn().flatMap(LangConstant::strValue);
        }
    }

    private Optional<String> decimalConstant(TestConverter converter) {
        var constant = converter.constantReturn();
        if (constant.isPresent()) {
            var value = constant.orElseThrow();
            if (value instanceof IntConstant || value instanceof BigintConstant) {
                return value.strValue();
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    private Optional<String> baseConstant(TestConverter converter, int base) {
        var constant = converter.constantReturn();
        if (constant.isPresent()) {
            var value = constant.orElseThrow();
            if (value instanceof IntConstant) {
                var intVal = ((IntConstant) value).getValue();
                return Optional.of(Integer.toString(intVal, base));
            } else if (value instanceof BigintConstant) {
                var intVal = ((BigintConstant) value).getValue();
                return Optional.of(intVal.toString(base));
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    private Optional<String> upperHexConstant(TestConverter converter) {
        var constant = converter.constantReturn();
        if (constant.isPresent()) {
            var value = constant.orElseThrow();
            if (value instanceof IntConstant) {
                var intVal = ((IntConstant) value).getValue();
                return Optional.of(Integer.toHexString(intVal).toUpperCase());
            } else if (value instanceof BigintConstant) {
                var intVal = ((BigintConstant) value).getValue();
                return Optional.of(intVal.toString(16).toUpperCase());
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    private Optional<String> expConstant(TestConverter converter) {
        var constant = converter.constantReturn();
        if (constant.isPresent()) {
            var value = constant.orElseThrow();
            if (value instanceof DecimalConstant) {
                var decValue = ((DecimalConstant) value).getValue();
                return Optional.of(expConstant(decValue));
            } else if (value instanceof IntConstant) {
                var intValue = ((IntConstant) value).getValue();
                return Optional.of(expConstant(BigDecimal.valueOf(intValue)));
            } else if (value instanceof BigintConstant) {
                var intValue = ((BigintConstant) value).getValue();
                return Optional.of(expConstant(new BigDecimal(intValue)));
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    private String expConstant(BigDecimal decValue) {
        var shift = decValue.precision() - decValue.scale() - 1;
        var result = decValue.movePointLeft(shift);
        return String.format("%.6fe%+03d", result, shift);
    }

    private Optional<String> upperExpConstant(TestConverter converter) {
        return expConstant(converter).map(String::toUpperCase);
    }

    private Optional<String> fixedConstant(TestConverter converter) {
        var constant = converter.constantReturn();
        if (constant.isPresent()) {
            var value = constant.orElseThrow();
            if (value instanceof DecimalConstant) {
                var decValue = ((DecimalConstant) value).getValue();
                return Optional.of(String.format("%.6f", decValue));
            } else if (value instanceof IntConstant) {
                var intValue = ((IntConstant) value).getValue();
                return Optional.of(String.format("%.6f", (float) intValue));
            } else if (value instanceof BigintConstant) {
                var intValue = ((BigintConstant) value).getValue();
                return Optional.of(String.format("%.6f", new BigDecimal(intValue)));
            }
        }
        return Optional.empty();
    }

    private Optional<String> upperFixedConstant(TestConverter converter) {
        return fixedConstant(converter).map(String::toUpperCase);
    }

    public Optional<String> generalFloatConstant(TestConverter converter) {
        var constant = converter.constantReturn();
        if (constant.isPresent()) {
            var value = constant.orElseThrow();
            if (value instanceof DecimalConstant) {
                var decValue = ((DecimalConstant) value).getValue();
                return Optional.of(decValue.toString());
            } else if (value instanceof IntConstant) {
                var intValue = ((IntConstant) value).getValue();
                return Optional.of(Double.toString(intValue));
            } else if (value instanceof BigintConstant) {
                var intValue = ((BigintConstant) value).getValue();
                return Optional.of(new BigDecimal(intValue).toString());
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    public Optional<String> upperGeneralConstant(TestConverter converter) {
        return generalFloatConstant(converter).map(String::toUpperCase);
    }

    private Optional<String> percentConstant(TestConverter converter) {
        var constant = converter.constantReturn();
        if (constant.isPresent()) {
            var value = constant.orElseThrow();
            if (value instanceof DecimalConstant) {
                var decVal = ((DecimalConstant) value).getValue();
                var adjusted = decVal.movePointRight(2).toString();
                return Optional.of(String.format("%s%%", adjusted));
            } else if (value instanceof IntConstant) {
                var intVal = ((IntConstant) value).getValue();
                return Optional.of(String.format("%d00%%", intVal));
            } else if (value instanceof BigintConstant) {
                var intVal = ((BigintConstant) value).getValue();
                return Optional.of(String.format("%d00%%", intVal));
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    private Optional<String> charConstant(TestConverter converter) {
        var constant = converter.constantReturn();
        if (constant.isPresent()) {
            var value = constant.orElseThrow();
            if (value instanceof CharConstant) {
                var charVal = ((CharConstant) value).getValue();
                return Optional.of(Character.toString(charVal));
            }
            if (value instanceof IntConstant) {
                var intVal = ((IntConstant) value).getValue();
                try {
                    return Optional.of(Character.toString(intVal));
                } catch (IllegalArgumentException badChar) {
                    return Optional.empty();
                }
            } else if (value instanceof BigintConstant) {
                var intVal = ((BigintConstant) value).getValue();
                try {
                    var smallVal = intVal.intValueExact();
                    return Optional.of(Character.toString(smallVal));
                } catch (IllegalArgumentException | ArithmeticException e) {
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    private void convertArgument(TestNode arg, int start, List<Byte> bytes, @NotNull FormatInfo format) {
        if (!format.isEmpty()) {
            convertFormatArgs(arg, start, bytes, format);
        } else {
            convertToStr(arg, start, bytes, format);
        }
    }

    private void convertFormatArgs(TestNode arg, int start, List<Byte> bytes, @NotNull FormatInfo format) {
        assert !format.isEmpty();
        var fType = format.getType();
        switch (fType) {
            case 's':
                convertToStr(arg, start, bytes, format);
                break;
            case 'r':
                convertToRepr(arg, start, bytes, format);
                break;
            case 'd':
                convertToInt(arg, start, bytes, format);
                break;
            case 'x':
                convertToBase(arg, 16, start, bytes, format);
                break;
            case 'X':
                convertToUpperHex(arg, start, bytes, format);
                break;
            case 'o':
                convertToBase(arg, 8, start, bytes, format);
                break;
            case 'b':
                convertToBase(arg, 2, start, bytes, format);
                break;
            case 'c':
                convertToChar(arg, start, bytes);
                break;
            case 'n':
            case 'e':
            case 'E':
            case 'f':
            case 'F':
            case 'g':
            case 'G':
            case '%':
                convertDecimal(arg, start, bytes, format);
            default:
                throw CompilerException.format("Invalid format argument %c", node, fType);
        }
    }

    private void convertToStr(TestNode arg, int start, @NotNull List<Byte> bytes, FormatInfo format) {
        var converter = TestConverter.of(info, arg, 1);
        boolean isNotStr = !Builtins.str().isSuperclass(converter.returnType()[0]);
        if (format.onlyType()) {
            bytes.addAll(converter.convert(start + bytes.size()));
            if (isNotStr) {
                bytes.add(Bytecode.CALL_OP.value);
                bytes.addAll(Util.shortToBytes((short) OpSpTypeNode.STR.ordinal()));
                bytes.addAll(Util.shortToBytes((short) 0));
            }
        } else {
            var fmtArgs = FormatConstant.fromFormatInfo(format);
            checkStrFormat(format);
            bytes.add(Bytecode.LOAD_CONST.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(Builtins.formatConstant())));
            bytes.addAll(converter.convert(start + bytes.size()));
            if (isNotStr) {
                bytes.add(Bytecode.CALL_OP.value);
                bytes.addAll(Util.shortToBytes((short) OpSpTypeNode.STR.ordinal()));
                bytes.addAll(Util.shortToBytes((short) 0));
            }
            bytes.add(Bytecode.LOAD_CONST.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(fmtArgs)));
            bytes.add(Bytecode.CALL_TOS.value);
            bytes.addAll(Util.shortToBytes((short) 2));
        }
    }

    private void convertToRepr(TestNode arg, int start, @NotNull List<Byte> bytes, FormatInfo format) {
        var converter = TestConverter.of(info, arg, 1);
        if (format.onlyType()) {
            bytes.addAll(converter.convert(start + bytes.size()));
            bytes.add(Bytecode.CALL_OP.value);
            bytes.addAll(Util.shortToBytes((short) OpSpTypeNode.REPR.ordinal()));
            bytes.addAll(Util.shortToBytes((short) 0));
        } else {
            var fmtArgs = FormatConstant.fromFormatInfo(format);
            checkStrFormat(format);
            bytes.add(Bytecode.LOAD_CONST.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(new BuiltinConstant(31))));
            bytes.addAll(converter.convert(start + bytes.size()));
            bytes.add(Bytecode.CALL_OP.value);
            bytes.addAll(Util.shortToBytes((short) OpSpTypeNode.REPR.ordinal()));
            bytes.addAll(Util.shortToBytes((short) 0));
            bytes.add(Bytecode.LOAD_CONST.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(fmtArgs)));
            bytes.add(Bytecode.CALL_TOS.value);
            bytes.addAll(Util.shortToBytes((short) 2));
        }
    }

    private void checkStrFormat(FormatInfo format) {
        if (format.getSign() != '\0') {
            throw CompilerException.of(
                    "Sign specifier is invalid in non-numeric format specifiers",
                    format
            );
        } else if (format.getPrecision() != 0) {
            throw CompilerException.of(
                    "Precision is not allowed in non-numeric format specifiers",
                    format
            );
        }
    }

    private void convertToInt(TestNode arg, int start, List<Byte> bytes, FormatInfo format) {
        if (format.onlyType()) {
            var converter = TestConverter.of(info, arg, 1);
            var retType = converter.returnType()[0];
            bytes.addAll(converter.convert(start + bytes.size()));
            makeInt(retType, arg, bytes);
            bytes.add(Bytecode.CALL_OP.value);
            bytes.addAll(Util.shortToBytes((short) OpSpTypeNode.STR.ordinal()));
            bytes.addAll(Util.shortToBytes((short) 0));
        } else {
            convertFmtInt(arg, start, bytes, format);
        }
    }

    private void convertToBase(TestNode arg, int base, int start, List<Byte> bytes, FormatInfo format) {
        if (base == 10) {
            convertToInt(arg, start, bytes, format);
        } else if (format.onlyType()) {
            var converter = TestConverter.of(info, arg, 1);
            var retType = converter.returnType()[0];
            bytes.addAll(converter.convert(start + bytes.size()));
            makeInt(retType, arg, bytes);
            bytes.add(Bytecode.LOAD_CONST.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(base))));
            bytes.add(Bytecode.CALL_METHOD.value);
            bytes.addAll(Util.shortToBytes(info.constIndex("strBase")));
            bytes.addAll(Util.shortToBytes((short) 1));
        } else {
            convertFmtInt(arg, start, bytes, format);
        }
    }

    private void makeInt(TypeObject retType, Lined arg, List<Byte> bytes) {
        if (!Builtins.intType().isSuperclass(retType)) {
            retType.tryOperatorInfo(arg, OpSpTypeNode.INT, info);
            bytes.add(Bytecode.CALL_OP.value);
            bytes.addAll(Util.shortToBytes((short) OpSpTypeNode.INT.ordinal()));
            bytes.addAll(Util.shortToBytes((short) 0));
        }
    }

    private void convertToChar(TestNode arg, int start, List<Byte> bytes) {
        var converter = TestConverter.of(info, arg, 1);
        var retType = converter.returnType()[0];
        if (Builtins.charType().isSuperclass(retType)) {
            bytes.addAll(converter.convert(start + bytes.size()));
            bytes.add(Bytecode.CALL_OP.value);
            bytes.addAll(Util.shortToBytes((short) OpSpTypeNode.STR.ordinal()));
            bytes.addAll(Util.shortToBytes((short) 0));
        } else if (Builtins.intType().isSuperclass(retType)) {
            bytes.add(Bytecode.LOAD_CONST.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(Builtins.charConstant())));
            bytes.addAll(converter.convert(start + bytes.size()));
            bytes.add(Bytecode.CALL_TOS.value);
            bytes.addAll(Util.shortToBytes((short) 1));
        } else {
            throw CompilerException.format(
                    "'c' format argument expects either an int or a char, not %s", arg, retType
            );
        }
    }

    private void convertToUpperHex(TestNode arg, int start, List<Byte> bytes, FormatInfo format) {
        assert format.getType() == 'X';
        convertFmtInt(arg, start, bytes, format);
    }

    private TypeObject convertFmtLoad(TestNode arg, int start, List<Byte> bytes) {
        var converter = TestConverter.of(info, arg, 1);
        var retType = converter.returnType()[0];
        bytes.add(Bytecode.LOAD_CONST.value);
        bytes.addAll(Util.shortToBytes(info.constIndex(Builtins.formatConstant())));
        bytes.addAll(converter.convert(start + bytes.size()));
        return retType;
    }

    private void convertFmtInt(TestNode arg, int start, List<Byte> bytes, FormatInfo format) {
        if (format.getPrecision() != 0) {
            throw CompilerException.of("Precision is not allowed in integer format specifier", format);
        }
        var fmtArgs = FormatConstant.fromFormatInfo(format);
        var retType = convertFmtLoad(arg, start, bytes);
        makeInt(retType, arg, bytes);
        bytes.add(Bytecode.LOAD_CONST.value);
        bytes.addAll(Util.shortToBytes(info.constIndex(fmtArgs)));
        bytes.add(Bytecode.CALL_TOS.value);
        bytes.addAll(Util.shortToBytes((short) 2));
    }

    private void convertDecimal(TestNode arg, int start, List<Byte> bytes, FormatInfo format) {
        var retType = convertFmtLoad(arg, start, bytes);
        if (!Builtins.intType().isSuperclass(retType) && !Builtins.decimal().isSuperclass(retType)) {
            throw CompilerException.format(
                    "Decimal format specifiers require either an integer or decimal argument, not '%s'",
                    format, retType.name()
            );
        }
        bytes.add(Bytecode.LOAD_CONST.value);
        bytes.addAll(Util.shortToBytes(info.constIndex( FormatConstant.fromFormatInfo(format))));
        bytes.add(Bytecode.CALL_TOS.value);
        bytes.addAll(Util.shortToBytes((short) 2));
    }
}
