package main.java.converter;

import main.java.converter.bytecode.ArgcBytecode;
import main.java.converter.bytecode.FunctionNoBytecode;
import main.java.parser.LineInfo;
import main.java.parser.Lined;
import main.java.parser.TestNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public final class Argument implements Lined {
    private final LineInfo lineInfo;
    private final String name;
    private final TypeObject type;
    private final boolean isVararg;
    private final DefaultValue defaultValue;

    public Argument(String name, TypeObject type) {
        this.name = name;
        this.type = type;
        this.isVararg = false;
        this.lineInfo = LineInfo.empty();
        this.defaultValue = null;
    }

    public Argument(String name, TypeObject type, boolean isVararg, LineInfo lineInfo) {
        this.name = name;
        this.type = type;
        this.isVararg = isVararg;
        this.lineInfo = lineInfo;
        this.defaultValue = null;
    }

    public Argument(String name, TypeObject type, boolean isVararg, LineInfo lineInfo, TestNode defaultValue) {
        this.name = name;
        this.type = type;
        this.isVararg = isVararg;
        this.lineInfo = lineInfo;
        this.defaultValue = new DefaultValue(defaultValue);
    }

    public void compile(CompilerInfo info) {
        if (defaultValue != null) {
            defaultValue.compile(info, type);
        }
    }

    public String getName() {
        return name;
    }

    public TypeObject getType() {
        return type;
    }

    public boolean isVararg() {
        return isVararg;
    }

    public Optional<DefaultValue> getDefaultValue() {
        return Optional.ofNullable(defaultValue);
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Argument argument = (Argument) o;
        return Objects.equals(name, argument.name) &&
                Objects.equals(type, argument.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @NotNull
    @Contract(pure = true)
    public static TypeObject[] typesOf(@NotNull Argument... args) {
        TypeObject[] result = new TypeObject[args.length];
        for (int i = 0; i < args.length; i++) {
            result[i] = args[i].getType();
        }
        return result;
    }

    public static final class DefaultValue {
        private final TestNode node;
        private BytecodeList bytes;
        private LangConstant constantValue;
        private short bytesIndex;

        private DefaultValue(TestNode node) {
            this.node = node;
            this.bytes = null;
            this.constantValue = null;
            this.bytesIndex = -1;
        }

        public void compile(CompilerInfo info, TypeObject parentType) {
            if (bytes == null) {
                var converter = TestConverter.of(info, node, 1, parentType);
                var returnType = converter.returnType()[0];
                if (!parentType.isSuperclass(returnType)) {
                    if (OptionTypeObject.needsMakeOption(parentType, returnType)) {
                        var constant = converter.constantReturn();
                        if (constant.isPresent()) {
                            var constantVal = constant.orElseThrow();
                            var constIndex = info.constIndex(constantVal);
                            constantValue = new OptionConstant(returnType, constIndex);
                        }
                        bytes = OptionTypeObject.wrapBytes(converter.convert());
                    } else {
                        throw CompilerException.format(
                                "Default value of argument with type '%s' is of type '%s'",
                                node, parentType.name(), returnType.name()
                        );
                    }
                } else {
                    var constant = converter.constantReturn();
                    if (constant.isPresent()) {
                        constantValue = constant.orElseThrow();
                    }
                    bytes = converter.convert();
                }
            }
        }

        public void loadBytes(BytecodeList bytes, CompilerInfo info) {
            if (constantValue != null) {
                bytes.loadConstant(constantValue, info);
            } else {
                var function = saveFunction(info);
                bytes.add(Bytecode.CALL_FN, new FunctionNoBytecode(function), ArgcBytecode.zero());
            }
        }

        private short saveFunction(CompilerInfo info) {
            if (bytesIndex == -1) {
                var byteFn = new BytecodeList(bytes);
                byteFn.add(Bytecode.RETURN, ArgcBytecode.one());
                var fnInfo = new FunctionInfo();  // FIXME: Get return type
                bytesIndex = (short) info.addFunction(new Function(fnInfo, byteFn));
            }
            return bytesIndex;
        }
    }
}
