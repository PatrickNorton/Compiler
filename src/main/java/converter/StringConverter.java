package main.java.converter;

import main.java.parser.StringNode;
import main.java.parser.StringPrefix;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class StringConverter implements ConstantConverter {
    private final CompilerInfo info;
    private final StringNode node;
    private final int retCount;

    public StringConverter(CompilerInfo info, StringNode node, int retCount) {
        this.info = info;
        this.node = node;
        this.retCount = retCount;
    }

    private enum StringType {
        STR,
        BYTES,
        CHAR,
        ;

        public static StringType fromPrefixes(Set<StringPrefix> prefixes) {
            if (prefixes.contains(StringPrefix.BYTES)) {
                return BYTES;
            } else if (prefixes.contains(StringPrefix.CHAR)) {
                return CHAR;
            } else {
                return STR;
            }
        }
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        assert !node.getPrefixes().contains(StringPrefix.FORMATTED);
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

    @NotNull
    @Override
    public LangConstant constant() {
        switch (StringType.fromPrefixes(node.getPrefixes())) {
            case STR:
                return LangConstant.of(node);
            case BYTES:
                var contents = node.getContents().getBytes(StandardCharsets.UTF_8);
                List<Byte> bytes = new ArrayList<>(contents.length);
                for (var b : contents) {
                    bytes.add(b);
                }
                return new BytesConstant(bytes);
            case CHAR:
                if (node.getContents().length() != 1) {
                    throw CompilerException.of("Char literals must have a length of 1", node);
                }
                return new CharConstant(node.getContents().charAt(0));
            default:
                throw new UnsupportedOperationException();
        }
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        switch (StringType.fromPrefixes(node.getPrefixes())) {
            case STR:
                return new TypeObject[] {Builtins.STR};
            case BYTES:
                return new TypeObject[] {Builtins.BYTES};
            case CHAR:
                return new TypeObject[] {Builtins.CHAR};
            default:
                throw new UnsupportedOperationException();
        }
    }
}
