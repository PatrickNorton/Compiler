package main.java.converter;

import main.java.parser.FormattedStringNode;
import main.java.parser.TestNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class FormattedStringConverter implements TestConverter {
    private FormattedStringNode node;
    private CompilerInfo info;
    private int retCount;

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
        assert retCount == 0 || retCount == 1;
        List<Byte> bytes = new ArrayList<>();
        var strings = node.getStrings();
        var tests = node.getTests();
        assert strings.length == tests.length || strings.length == tests.length + 1;
        for (int i = 0; i < node.getStrings().length; i++) {
            bytes.add(Bytecode.LOAD_CONST.value);
            var constValue = LangConstant.of(strings[i]);
            bytes.addAll(Util.shortToBytes(info.constIndex(constValue)));
            if (i != 0) {
                bytes.add(Bytecode.PLUS.value);
            }
            if (i < tests.length) {
                convertArgument(tests[i], start, bytes);
            }
        }
        if (retCount == 0) {
            CompilerWarning.warn("Unused f-string literal", node);
            bytes.add(Bytecode.POP_TOP.value);
        }
        return bytes;
    }

    private void convertArgument(TestNode arg, int start, List<Byte> bytes) {
        var converter = TestConverter.of(info, arg, 1);
        boolean isNotStr = !Builtins.STR.isSuperclass(converter.returnType()[0]);
        if (isNotStr) {
            bytes.add(Bytecode.LOAD_CONST.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(Builtins.constantOf("str"))));
        }
        bytes.addAll(converter.convert(start + bytes.size()));
        if (isNotStr) {
            bytes.add(Bytecode.CALL_TOS.value);
            bytes.addAll(Util.shortToBytes((short) 1));
        }
        bytes.add(Bytecode.PLUS.value);
    }
}
