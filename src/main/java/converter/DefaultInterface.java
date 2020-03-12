package main.java.converter;

import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class DefaultInterface extends AbstractDefaultInterface {
    private GenericInfo info;

    public DefaultInterface(String name, GenericInfo info, Map<OpSpTypeNode, FunctionInfo> operators) {
        super(name, operators);
        this.info = info;
    }

    public DefaultInterface(String name, Map<OpSpTypeNode, FunctionInfo> operators) {
        super(name, operators);
        this.info = GenericInfo.empty();
    }

    @Override
    public TypeObject generify(@NotNull TypeObject... args) {
        return new GenerifiedDefaultInterface(this, info.generify(args));
    }

    @Override
    public FunctionInfo operatorInfo(OpSpTypeNode o) {
        return operators.get(o).boundify();
    }

    @Override
    public TypeObject[] operatorReturnType(OpSpTypeNode o) {
        return sanitizeType(internalReturnType(o));
    }

    private TypeObject internalReturnType(OpSpTypeNode o) {
        var result = operators.get(o).getReturns()[0];
        return result instanceof TemplateParam ? ((TemplateParam) result).getBound() : result;
    }

    public Map<OpSpTypeNode, FunctionInfo> getOperators() {
        return operators;
    }
}
