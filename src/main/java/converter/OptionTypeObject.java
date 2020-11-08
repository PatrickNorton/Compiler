package main.java.converter;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class OptionTypeObject extends TypeObject {
    private static final Map<BaseType, FunctionInfo> MAP_CACHE = new HashMap<>();
    private static final Map<BaseType, FunctionInfo> FLAT_MAP_CACHE = new HashMap<>();

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

    @Contract(pure = true)
    @Override
    @NotNull
    public String baseName() {
        return "";
    }

    @Contract("_ -> new")
    @Override
    @NotNull
    public TypeObject typedefAs(String name) {
        return new OptionTypeObject(name, this.optionVal);
    }

    @Override
    public boolean sameBaseType(TypeObject other) {
        return other instanceof OptionTypeObject;
    }

    @Override
    public int baseHash() {
        return "option".hashCode();
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

    public TypeObject getOptionVal() {
        return optionVal;
    }

    @Override
    public TypeObject stripNull() {
        return optionVal;
    }

    @Override
    public TypeObject makeMut() {
        return new OptionTypeObject(optionVal.makeMut());
    }

    @Override
    public TypeObject makeConst() {
        return new OptionTypeObject(optionVal.makeConst());
    }

    @Override
    public Optional<Map<Integer, TypeObject>> generifyAs(TypeObject parent, TypeObject other) {
        if (other instanceof OptionTypeObject) {
            return optionVal.generifyAs(parent, ((OptionTypeObject) other).optionVal);
        } else if (other instanceof ObjectType) {
            return Optional.of(Collections.emptyMap());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public TypeObject generifyWith(TypeObject parent, List<TypeObject> values) {
        return TypeObject.optional(optionVal.generifyWith(parent, values));
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
        List<Byte> result = new ArrayList<>(bytes);
        result.add(Bytecode.MAKE_OPTION.value);
        return result;
    }

    public static boolean needsMakeOption(TypeObject maybeOption, TypeObject other) {
        if (maybeOption instanceof OptionTypeObject) {
            var option = (OptionTypeObject) maybeOption;
            return Builtins.NULL_TYPE.isSuperclass(other) || option.optionVal.isSuperclass(other);
        } else {
            return false;
        }
    }

    public static boolean superWithOption(TypeObject maybeOption, TypeObject other) {
        assert needsMakeOption(maybeOption, other);
        return other.sameBaseType(Builtins.NULL_TYPE) || maybeOption.isSuperclass(TypeObject.optional(other));
    }

    @Override
    @NotNull
    public Optional<TypeObject> attrType(String value, AccessLevel access) {
        var base = new BaseType(optionVal);
        switch (value) {
            case "map":
                return Optional.of(
                        MAP_CACHE.computeIfAbsent(base, OptionTypeObject::getMap).toCallable()
                );
            case "flatMap":
                return Optional.of(
                        FLAT_MAP_CACHE.computeIfAbsent(base, OptionTypeObject::getFlatMap).toCallable()
                );
            default:
                return Optional.empty();
        }
    }

    private static FunctionInfo getMap(BaseType type) {
        var param = new TemplateParam("T", 0, Builtins.OBJECT);
        var innerFn = new FunctionInfo(ArgumentInfo.of(type.getValue()), param).toCallable();
        var fnInfo = new FunctionInfo("map", false, ArgumentInfo.of(innerFn), TypeObject.optional(param));
        param.setParent(fnInfo.toCallable());
        return fnInfo;
    }

    private static FunctionInfo getFlatMap(BaseType type) {
        var param = new TemplateParam("T", 0, Builtins.OBJECT);
        var optionParam = TypeObject.optional(param);
        var innerFn = new FunctionInfo(ArgumentInfo.of(type.getValue()), optionParam).toCallable();
        var fnInfo = new FunctionInfo("flatMap", false, ArgumentInfo.of(innerFn), optionParam);
        param.setParent(fnInfo.toCallable());
        return fnInfo;
    }
}
