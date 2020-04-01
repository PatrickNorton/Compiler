package main.java.converter;

import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class DefaultInterface extends AbstractDefaultInterface {
    private GenericInfo info;
    private String typedefName;

    public DefaultInterface(String name, GenericInfo info, Map<OpSpTypeNode, FunctionInfo> operators) {
        super(name, operators);
        this.info = info;
        this.typedefName = "";
    }

    public DefaultInterface(String name, Map<OpSpTypeNode, FunctionInfo> operators) {
        super(name, operators);
        this.info = GenericInfo.empty();
        this.typedefName = "";
    }

    private DefaultInterface(String name, GenericInfo info, Map<OpSpTypeNode, FunctionInfo> operators, String typedefName) {
        super(name, operators);
        this.info = info;
        this.typedefName = typedefName;
    }

    @Override
    public TypeObject generify(@NotNull TypeObject... args) {
        return new GenerifiedDefaultInterface(this, info.generify(args));
    }

    @Override
    public TypeObject typedefAs(String name) {
        return new DefaultInterface(super.name(), info, operators, name);
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
