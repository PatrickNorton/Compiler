package main.java.converter;

import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.StringJoiner;

public class GenerifiedDefaultInterface extends AbstractDefaultInterface {
    private List<TypeObject> types;
    private String typedefName;

    public GenerifiedDefaultInterface(@NotNull DefaultInterface parent, List<TypeObject> types) {
        super(parent.name(), parent.operators);
        this.types = types;
        this.typedefName = "";
    }

    private GenerifiedDefaultInterface(@NotNull GenerifiedDefaultInterface parent, String typedefName) {
        super(parent.name(), parent.operators);
        this.types = parent.types;
        this.typedefName = typedefName;
    }

    @Override
    public TypeObject[] operatorReturnType(OpSpTypeNode o) {
        var ret = operators.get(o).getReturns()[0];
        return sanitizeType(ret instanceof TemplateParam ? types.get(((TemplateParam) ret).getIndex()) : ret);
    }

    @Override
    public FunctionInfo operatorInfo(OpSpTypeNode o) {
        return operators.get(o).generify(types);
    }

    @Override
    public String name() {
        if (!typedefName.isEmpty()) {
            return typedefName;
        }
        var sj = new StringJoiner(", ",super.name() + "[",  "]");
        for (var type : types) {
            sj.add(type.name());
        }
        return sj.toString();
    }

    @Override
    public TypeObject typedefAs(String name) {
        return new GenerifiedDefaultInterface(this, name);
    }
}
