package main.java.converter;

import main.java.parser.FormattedStringNode;
import main.java.parser.OpSpTypeNode;
import main.java.parser.TestNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

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
        for (int i = 0; i < node.getStrings().length; i++) {
            bytes.add(Bytecode.LOAD_CONST.value);
            var constValue = LangConstant.of(strings[i]);
            bytes.addAll(Util.shortToBytes(info.constIndex(constValue)));
            if (i != 0) {
                bytes.add(Bytecode.PLUS.value);
            }
            if (i < tests.length) {
                convertArgument(tests[i], start, bytes, node.getFormats()[i]);
                bytes.add(Bytecode.PLUS.value);
            }
        }
        if (retCount == 0) {
            bytes.add(Bytecode.POP_TOP.value);
        }
        return bytes;
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
        assert fStr.length() == 1 : "More complex f-string specifiers are not yet implemented";
        switch (fStr.charAt(0)) {
            case 's':
                convertToStr(arg, start, bytes);
                break;
            case 'r':
                convertToRepr(arg, start, bytes);
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
}
