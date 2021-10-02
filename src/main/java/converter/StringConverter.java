package main.java.converter;

import main.java.parser.StringNode;
import main.java.parser.StringPrefix;
import main.java.util.StringEscape;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
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
        BYTE,
        ;

        public static StringType fromPrefixes(Set<StringPrefix> prefixes) {
            if (prefixes.contains(StringPrefix.BYTES)) {
                return BYTES;
            } else if (prefixes.contains(StringPrefix.CHAR)) {
                return CHAR;
            } else if (prefixes.contains(StringPrefix.BYTE)) {
                return BYTE;
            } else {
                return STR;
            }
        }
    }

    @NotNull
    @Override
    public BytecodeList convert() {
        assert !node.getPrefixes().contains(StringPrefix.FORMATTED);
        if (retCount == 0) {
            CompilerWarning.warn("String-like literal unused", WarningType.UNUSED, info, node);
            return new BytecodeList();
        }
        if (node.getPrefixes().contains(StringPrefix.REGEX)) {
            throw CompilerTodoError.of("Regex strings not yet supported", node);
        }
        var bytes = new BytecodeList();
        bytes.loadConstant(constant(), info);
        return bytes;
    }

    @NotNull
    @Override
    public LangConstant constant() {
        switch (StringType.fromPrefixes(node.getPrefixes())) {
            case STR:
                return LangConstant.of(node.getContents());
            case BYTES:
                var contents = Util.toByteList(node.getContents().getBytes(StandardCharsets.UTF_8));
                return new BytesConstant(contents);
            case CHAR:
                checkLen("Char");
                return new CharConstant(node.getContents().codePointAt(0));
            case BYTE:
                checkLen("Byte");
                var cp = node.getContents().codePointAt(0);
                if (cp >= 0 && cp < 0x80) {
                    return new IntConstant(cp);
                } else {
                    throw CompilerException.format(
                            "Byte literals only support ASCII values, not '%s' (value 0x%x)",
                            node, StringEscape.escaped(cp), cp
                    );
                }
            default:
                throw new UnsupportedOperationException();
        }
    }

    private void checkLen(String litType) {
        if (node.getContents().codePointCount(0, node.getContents().length()) != 1) {
            throw CompilerException.format("%s literals must have a length of 1", node, litType);
        }
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        return switch (StringType.fromPrefixes(node.getPrefixes())) {
            case STR -> new TypeObject[]{Builtins.str()};
            case BYTES -> new TypeObject[]{Builtins.bytes()};
            case CHAR -> new TypeObject[]{Builtins.charType()};
            case BYTE -> new TypeObject[]{Builtins.intType()};
        };
    }
}
