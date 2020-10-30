package main.java.converter;

import main.java.parser.OpSpTypeNode;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TemplateParam extends NameableType {
    private final String name;
    private final int index;
    private final TypeObject bound;
    private final boolean isVararg;
    private final String typedefName;
    private TypeObject parent;

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

    private TemplateParam(TemplateParam other, String typedefName) {
        this.name = other.name;
        this.index = other.index;
        this.bound = other.bound;
        this.isVararg = other.isVararg;
        this.parent = other.parent;
        this.typedefName = typedefName;
    }

    @Override
    protected boolean isSubclass(TypeObject other) {
        if (other instanceof TemplateParam) {
            var tp = (TemplateParam) other;
            return tp.parent.sameBaseType(parent) && tp.bound.isSuperclass(bound);
        } else {
            return other.isSuperclass(bound);
        }
    }

    @Override

    public Optional<TypeObject> attrType(String value, AccessLevel access) {
        return bound.attrType(value, access);
    }

    @Override

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

    @Override
    public boolean sameBaseType(TypeObject other) {
        return equals(other);
    }

    @Override
    public int baseHash() {
        return hashCode();
    }

    public TypeObject getParent() {
        return parent;
    }

    public void setParent(TypeObject parent) {
        assert this.parent == null : "Should not set parent of TemplateParam more than once";
        this.parent = parent;
    }

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

    public Optional<Map<Integer, TypeObject>> generifyAs(TypeObject parent, TypeObject other) {
        if (this.equals(other)) {
            return Optional.of(Collections.emptyMap());
        } else if (this.parent.sameBaseType(parent)) {
            return Optional.of(Map.of(index, other));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public TypeObject generifyWith(TypeObject parent, List<TypeObject> values) {
        if (this.parent.sameBaseType(parent)) {
            return values.get(index);
        } else {
            return this;
        }
    }
}
