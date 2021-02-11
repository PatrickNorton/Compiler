package main.java.converter;

import main.java.parser.LineInfo;
import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class TypeTypeObject extends TypeObject {
    private final String typedefName;
    private final TypeObject generic;

    public TypeTypeObject() {
        this.typedefName = "";
        this.generic = null;
    }

    private TypeTypeObject(TypeObject generic, String typedefName) {
        this.generic = generic;
        this.typedefName = typedefName;
    }

    public TypeObject representedType() {
        return generic;
    }

    @Override
    public boolean isSuperclass(@NotNull TypeObject other) {
        return other instanceof TypeTypeObject
                || other instanceof ObjectType;
    }

    @Override
    public boolean willSuperRecurse() {
        return false;
    }

    @Override
    protected boolean isSubclass(@NotNull TypeObject other) {
        return other instanceof TypeTypeObject || other instanceof ObjectType;
    }

    @Override
    public String name() {
        return typedefName.isEmpty()
                ? generic == null ? "type" : String.format("type[%s]", generic.name())
                : typedefName;
    }

    @Contract(pure = true)
    @Override
    @NotNull
    public String baseName() {
        return "type";
    }

    @Override
    public List<TypeObject> getGenerics() {
        return generic == null ? Collections.emptyList() : Collections.singletonList(generic);
    }

    @Override
    public boolean sameBaseType(TypeObject other) {
        return other instanceof TypeTypeObject;
    }

    @Override
    public int baseHash() {
        return baseName().hashCode();
    }

    @Contract("_ -> new")
    @Override
    @NotNull
    public TypeObject typedefAs(String name) {
        return new TypeTypeObject(this.generic, name);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TypeTypeObject;
    }

    @Override
    public int hashCode() {
        return Objects.hash(TypeTypeObject.class, "type");
    }

    @Contract("_, _ -> new")
    @Override
    @NotNull
    public TypeObject generify(LineInfo lineInfo, @NotNull TypeObject... args) {
        if (args.length != 1) {
            throw CompilerException.of("Cannot generify object in this manner", lineInfo);
        }
        return new TypeTypeObject(args[0], this.typedefName);
    }

    @Contract(value = "_, _ -> new", pure = true)
    @Override
    @NotNull
    public Optional<TypeObject[]> operatorReturnType(OpSpTypeNode o, AccessLevel access) {
        if (o == OpSpTypeNode.CALL) {
            return Optional.of(new TypeObject[] {generic == null ? Builtins.object() : generic.makeMut()});
        } else {
            return generic == null ? Optional.empty() : generic.staticOperatorReturnType(o);
        }
    }

    @Override
    @NotNull
    public Optional<FunctionInfo> operatorInfo(OpSpTypeNode o, AccessLevel access) {
        if (generic != null) {
            if (o == OpSpTypeNode.CALL) {
                var opInfo = generic.makeMut().operatorInfo(OpSpTypeNode.NEW, access);
                return opInfo.map(functionInfo -> new FunctionInfo(functionInfo.getArgs(), generic.makeMut()));
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    @Override
    @NotNull
    public Optional<TypeObject> attrType(String value, AccessLevel access) {
        return generic == null ? Optional.empty() : generic.staticAttrType(value, access);
    }

    @Override
    public @NotNull TypeObject tryAttrType(LineInfo lineInfo, String value, AccessLevel access) {
        if (generic == null) {
            throw CompilerException.of("Cannot get attribute '%s' from type 'type'", lineInfo);
        } else {
            return generic.tryStaticAttrType(lineInfo, value, access);
        }
    }

    @Override
    public Optional<Iterable<String>> getDefined() {
        return generic == null ? Optional.empty() : generic.staticDefined();
    }
}
