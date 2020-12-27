package main.java.converter;

import main.java.parser.LineInfo;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TypeLoader implements TestConverter {
    private LineInfo lineInfo;
    private TypeObject value;
    private CompilerInfo info;

    public TypeLoader(LineInfo lineInfo, TypeObject value, CompilerInfo info) {
        this.lineInfo = lineInfo;
        this.value = value;
        this.info = info;
    }

    @NotNull
    public Optional<LangConstant> constantReturn() {
        return typeConstant(lineInfo, value, info);
    }

    @Override
    @NotNull
    public List<Byte> convert(int start) {
        var constant = constantReturn();
        if (constant.isPresent()) {
            List<Byte> bytes = new ArrayList<>();
            bytes.add(Bytecode.LOAD_CONST.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(constant.orElseThrow())));
            return bytes;
        } else {
            assert value instanceof TemplateParam;
            return new TypeLoader(lineInfo, ((TemplateParam) value).getBound(), info).convert(start);
            /*
            Commented out until better type inference can come around
            var parent = info.localParent(value).orElseThrow(
                    () -> CompilerException.format("Unknown type '%s'", lineInfo, value.name())
            );
            List<Byte> bytes = new ArrayList<>(new TypeLoader(lineInfo, parent, info).convert(start));
            bytes.add(Bytecode.LOAD_DOT.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(value.baseName()))));
            return bytes;
             */
        }
    }

    @Override
    @NotNull
    public TypeObject[] returnType() {
        return new TypeObject[] {value.getType()};
    }

    /**
     * Gets the constant representing a type.
     * <p>
     *     This is still pretty finicky; in particular there is not support yet
     *     for "complex" types (those with generics, list types, etc.). This
     *     will return {@link Optional#empty()} for a local type, and throw an
     *     error for nonexistent types.
     * </p>
     *
     * @param lineInfo The line information to use in case of an exception
     * @param value The type from which to retrieve a constant
     * @param info The {@link CompilerInfo} to reference
     * @return The constant for the type
     */
    public static Optional<LangConstant> typeConstant(LineInfo lineInfo, TypeObject value, CompilerInfo info) {
        if (value instanceof OptionTypeObject) {
            return optionConstant(lineInfo, (OptionTypeObject) value, info);
        }
        var name = value.baseName();
        if (name.isEmpty()) {
            throw CompilerInternalError.of(
                    "Error in literal conversion: Lists of non-nameable types not complete yet", lineInfo
            );
        }
        if (name.equals("null")) {
            return Optional.of(Builtins.nullTypeConstant());
        } else if (Builtins.BUILTIN_MAP.containsKey(name) && Builtins.BUILTIN_MAP.get(name) instanceof TypeObject) {
            return Optional.of(Builtins.constantOf(name).orElseThrow(
                    () -> CompilerException.format("Type %s not found", lineInfo, name)
            ));
        } else {
            var constants = info.getConstants();
            for (int i = 0; i < constants.size(); i++) {
                var constant = constants.get(i);
                var constType = constant.getType();
                if (constType instanceof TypeTypeObject &&
                        ((TypeTypeObject) constType).representedType().sameBaseType(value)) {
                    return Optional.of(constant);
                }
            }
            if (info.localParent(value).isPresent()) {
                return Optional.empty();
            } else {
                throw CompilerException.format("Type %s not found", lineInfo, name);
            }
        }
    }

    private static Optional<LangConstant> optionConstant(LineInfo lineInfo, OptionTypeObject type, CompilerInfo info) {
        var interiorType = type.getOptionVal();
        var typeConst = typeConstant(lineInfo, interiorType, info);
        if (typeConst.isPresent()) {
            return Optional.of(
                    new OptionTypeConstant(interiorType.name(), info.constIndex(typeConst.orElseThrow()), interiorType)
            );
        } else {
            return Optional.empty();
        }
    }
}
