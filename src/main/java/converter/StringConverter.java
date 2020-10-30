package main.java.converter;

import main.java.parser.StringNode;
import main.java.parser.StringPrefix;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StringConverter implements ConstantConverter {
    private final CompilerInfo info;
    private final StringNode node;
    private final int retCount;

    public StringConverter(CompilerInfo info, StringNode node, int retCount) {
        this.info = info;
        this.node = node;
        this.retCount = retCount;
    }

    @Override
    public List<Byte> convert(int start) {
        if (retCount == 0) {
            CompilerWarning.warn("String-like literal unused", node);
            return Collections.emptyList();
        }
        if (node.getPrefixes().contains(StringPrefix.REGEX)) {
            throw CompilerTodoError.of("Regex strings not yet supported", node);
        }
        int constIndex = info.addConstant(constant());
        List<Byte> bytes = new ArrayList<>();
        bytes.add(Bytecode.LOAD_CONST.value);
        bytes.addAll(Util.shortToBytes((short) constIndex));
        return bytes;
    }

    @Override
    public LangConstant constant() {
        if (isBytes()) {
            var contents = node.getContents().getBytes(StandardCharsets.UTF_8);
            List<Byte> bytes = new ArrayList<>(contents.length);
            for (var b : contents) {
                bytes.add(b);
            }
            return new BytesConstant(bytes);
        } else {
            return LangConstant.of(node);
        }
    }

    @Override
    public TypeObject[] returnType() {
        return new TypeObject[] {isBytes() ? Builtins.BYTES : Builtins.STR};
    }

    private boolean isBytes() {
        return node.getPrefixes().contains(StringPrefix.BYTES);
    }
}
