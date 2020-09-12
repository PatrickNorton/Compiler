package main.java.converter;

import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Map;
import java.util.Optional;

public final class TemplateParam extends NameableType {
    private final String name;
    private final int index;
    private final TypeObject bound;
    private final boolean isVararg;
    private final String typedefName;
    private Template<?> parent;

    public TemplateParam(String name, int index, boolean isVararg) {
        this(name, index, TypeObject.list(), true);
        assert isVararg;
    }

    public TemplateParam(String name, int index, TypeObject bound) {
        this(name, index, bound, false);
    }

    private TemplateParam(String name, int index, TypeObject bound, boolean isVararg) {
        this.name = name;
        this.index = index;
        this.bound = bound;
        this.isVararg = isVararg;
        this.typedefName = "";
    }

    @Contract(pure = true)
    private TemplateParam(@NotNull TemplateParam other, String typedefName) {
        this.name = other.name;
        this.index = other.index;
        this.bound = other.bound;
        this.isVararg = other.isVararg;
        this.typedefName = typedefName;
    }

    @Override
    protected boolean isSubclass(@NotNull TypeObject other) {
        if (other instanceof TemplateParam) {
            return ((TemplateParam) other).bound.isSuperclass(bound);
        } else {
            return other.isSuperclass(bound);
        }
    }

    @Override
    @NotNull
    public Optional<TypeObject> attrType(String value, AccessLevel access) {
        return bound.attrType(value, access);
    }

    @Override
    @NotNull
    public Optional<TypeObject> staticAttrType(String value, AccessLevel access) {
        return bound.staticAttrType(value, access);
    }

    @Override
    public Optional<FunctionInfo> operatorInfo(OpSpTypeNode o, AccessLevel access) {
        return bound.operatorInfo(o, access);
    }

    @Override
    public String name() {
        return typedefName.isEmpty() ? name : typedefName;
    }

    @Override
    public String baseName() {
        return name;
    }

    public Template<?> getParent() {
        return parent;
    }

    public void setParent(Template<?> parent) {
        assert this.parent == null : "Should not set parent of TemplateParam more than once";
        this.parent = parent;
    }

    @NotNull
    @Contract(value = "_ -> new", pure = true)
    @Override
    public TypeObject typedefAs(String name) {
        return new TemplateParam(this, name);
    }

    public int getIndex() {
        return index;
    }

    public TypeObject getBound() {
        return bound;
    }

    public boolean isVararg() {
        return isVararg;
    }

    @Override
    @NotNull
    @Unmodifiable
    public Map<Integer, TypeObject> generifyAs(TypeObject other) {
        return Map.of(index, other);
    }
}
