package main.java.converter;

import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.StringJoiner;

public class GenerifiedDefaultInterface extends AbstractDefaultInterface {
    private List<TypeObject> types;

    public GenerifiedDefaultInterface(@NotNull DefaultInterface parent, List<TypeObject> types) {
        super(parent.name(), parent.operators);
        this.types = types;
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
        var sj = new StringJoiner(", ",super.name() + "[",  "]");
        for (var type : types) {
            sj.add(type.name());
        }
        return sj.toString();
    }
}
