package main.java.converter;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public final class OptionTypeObject extends TypeObject {  // TODO: Properly make options
    private final String typedefName;
    private final TypeObject optionVal;

    public OptionTypeObject(TypeObject optionVal) {
        this.typedefName = "";
        this.optionVal = optionVal;
    }

    private OptionTypeObject(String typedefName, TypeObject optionVal) {
        this.typedefName = typedefName;
        this.optionVal = optionVal;
    }

    @Override
    protected boolean isSubclass(@NotNull TypeObject other) {
        if (equals(other)) {
            return true;
        }
        if (other instanceof OptionTypeObject) {
            return ((OptionTypeObject) other).optionVal.isSuperclass(optionVal);
        }
        return false;
    }

    @Override
    public String name() {
        return typedefName.isEmpty() ? optionVal.name() + "?" : typedefName;
    }

    @Contract("_ -> new")
    @Override
    @NotNull
    public TypeObject typedefAs(String name) {
        return new OptionTypeObject(name, this.optionVal);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OptionTypeObject that = (OptionTypeObject) o;
        return Objects.equals(typedefName, that.typedefName) &&
                Objects.equals(optionVal, that.optionVal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typedefName, optionVal);
    }

    public static List<Byte> maybeWrapBytes(@NotNull List<Byte> bytes, boolean wrap) {
        if (wrap) {
            return wrapBytes(bytes);
        }
        return bytes;
    }

    @Contract("_ -> param1")
    @NotNull
    public static List<Byte> wrapBytes(@NotNull List<Byte> bytes) {
        bytes.add(Bytecode.MAKE_OPTION.value);
        return bytes;
    }

    public static boolean needsMakeOption(TypeObject maybeOption, TypeObject other) {
        if (maybeOption instanceof OptionTypeObject) {
            var option = (OptionTypeObject) maybeOption;
            return Builtins.NULL_TYPE.isSuperclass(other) || option.optionVal.isSuperclass(other);
        } else {
            return false;
        }
    }
}
