package main.java.converter;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class OptionTypeObject extends TypeObject {
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
}
