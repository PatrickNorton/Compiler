package main.java.converter;

import main.java.parser.FormattedStringNode;
import main.java.parser.Lined;
import main.java.parser.OpSpTypeNode;
import main.java.parser.TestNode;
import org.jetbrains.annotations.NotNull;

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
        return new TypeObject[] {Builtins.STR};
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        if (retCount == 0) {
            CompilerWarning.warn("Unused formatted string", node);
        } else if (retCount > 1) {
            throw CompilerException.format("F-string literal only returns one value, not %d", node, retCount);
        }
        List<Byte> bytes = new ArrayList<>();
        var strings = node.getStrings();
        var tests = node.getTests();
        assert strings.length == tests.length || strings.length == tests.length + 1;
        if (tests.length == 0) {
            CompilerWarning.warn("F-string with no formatted arguments", node);
        }
        for (int i = 0; i < strings.length; i++) {
            if (i < tests.length) {
                var format = node.getFormats()[i];
                var strValue = argConstant(tests[i], format);
                if (format.size() > 0 && strValue.isPresent()) {
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
        if (format.size() > 0) {
            var fStr = format.getSpecifier();
            if (fStr.length() != 1) {
                return Optional.empty();
            }
            var converter = TestConverter.of(info, arg, retCount);
            switch (fStr.charAt(0)) {
                case 's':
                    return converter.constantReturn().flatMap(LangConstant::strValue);
                case 'r':
                    return converter.constantReturn().flatMap(LangConstant::reprValue);
                case 'd':
                    return decimalConstant(converter);
                case 'x':
                    return baseConstant(converter, 16);
                case 'o':
                    return baseConstant(converter, 8);
                case 'b':
                    return baseConstant(converter, 2);
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

    private void convertArgument(TestNode arg, int start, List<Byte> bytes,
                                 @NotNull FormattedStringNode.FormatInfo format) {
        if (format.size() > 0) {
            convertFormatArgs(arg, start, bytes, format);
        } else {
            convertToStr(arg, start, bytes);
        }
    }

    private void convertFormatArgs(TestNode arg, int start, List<Byte> bytes,
                                   @NotNull FormattedStringNode.FormatInfo format) {
        assert format.size() > 0;
        var fStr = format.getSpecifier();
        if (fStr.length() != 1) {
            throw CompilerTodoError.of("Non-trivial f-string specifiers (not !s and !r)", arg);
        }
        switch (fStr.charAt(0)) {
            case 's':
                convertToStr(arg, start, bytes);
                break;
            case 'r':
                convertToRepr(arg, start, bytes);
                break;
            case 'd':
                convertToInt(arg, start, bytes);
                break;
            case 'x':
                convertToBase(arg, 16, start, bytes);
                break;
            case 'o':
                convertToBase(arg, 8, start, bytes);
                break;
            case 'b':
                convertToBase(arg, 2, start, bytes);
                break;
            default:
                throw CompilerException.format("Invalid format argument %c", node, fStr.charAt(0));
        }
    }

    private void convertToStr(TestNode arg, int start, @NotNull List<Byte> bytes) {
        var converter = TestConverter.of(info, arg, 1);
        boolean isNotStr = !Builtins.STR.isSuperclass(converter.returnType()[0]);
        bytes.addAll(converter.convert(start + bytes.size()));
        if (isNotStr) {
            bytes.add(Bytecode.CALL_OP.value);
            bytes.addAll(Util.shortToBytes((short) OpSpTypeNode.STR.ordinal()));
            bytes.addAll(Util.shortToBytes((short) 0));
        }
    }

    private void convertToRepr(TestNode arg, int start, @NotNull List<Byte> bytes) {
        bytes.addAll(TestConverter.bytes(start + bytes.size(), arg, info, 1));
        bytes.add(Bytecode.CALL_OP.value);
        bytes.addAll(Util.shortToBytes((short) OpSpTypeNode.REPR.ordinal()));
        bytes.addAll(Util.shortToBytes((short) 0));
    }

    private void convertToInt(TestNode arg, int start, List<Byte> bytes) {
        var converter = TestConverter.of(info, arg, 1);
        var retType = converter.returnType()[0];
        bytes.addAll(converter.convert(start + bytes.size()));
        makeInt(retType, arg, bytes);
        bytes.add(Bytecode.CALL_OP.value);
        bytes.addAll(Util.shortToBytes((short) OpSpTypeNode.STR.ordinal()));
        bytes.addAll(Util.shortToBytes((short) 0));
    }

    private void convertToBase(TestNode arg, int base, int start, List<Byte> bytes) {
        if (base == 10) {
            convertToInt(arg, start, bytes);
            return;
        }
        var converter = TestConverter.of(info, arg, 1);
        var retType = converter.returnType()[0];
        bytes.addAll(converter.convert(start + bytes.size()));
        makeInt(retType, arg, bytes);
        throw CompilerTodoError.format("Non-10 base conversion (base %d) in f-string", arg, base);
    }

    private void makeInt(TypeObject retType, Lined arg, List<Byte> bytes) {
        if (!Builtins.INT.isSuperclass(retType)) {
            retType.tryOperatorInfo(arg, OpSpTypeNode.INT, info);
            bytes.add(Bytecode.CALL_OP.value);
            bytes.addAll(Util.shortToBytes((short) OpSpTypeNode.INT.ordinal()));
            bytes.addAll(Util.shortToBytes((short) 0));
        }
    }
}
