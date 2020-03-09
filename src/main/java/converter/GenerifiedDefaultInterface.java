package main.java.converter;

import main.java.parser.OpSpTypeNode;

import java.util.List;
import java.util.StringJoiner;

public class GenerifiedDefaultInterface implements TypeObject {
    private DefaultInterface parent;
    private List<TypeObject> types;

    public GenerifiedDefaultInterface(DefaultInterface parent, List<TypeObject> types) {
        this.parent = parent;
        this.types = types;
    }

    @Override
    public TypeObject[] operatorReturnType(OpSpTypeNode o) {
        var ret = parent.opRetType(o);
        return parent.sanitizeType(ret instanceof TemplateParam ? types.get(((TemplateParam) ret).getIndex()) : ret);
    }

    @Override
    public boolean isSuperclass(TypeObject other) {
        for (var pair : parent.getOperators().entrySet()) {
            var otherRet = other.operatorReturnType(pair.getKey());
            if (otherRet != null) {  // TODO: Ensure types match
                return false;
            }
        }
        return true;
    }

    @Override
    public String name() {
        var sj = new StringJoiner(parent.name() + "[", ", ", "]");
        for (var type : types) {
            sj.add(type.name());
        }
        return sj.toString();
    }
}
