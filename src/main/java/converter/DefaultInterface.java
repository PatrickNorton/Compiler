package main.java.converter;

import main.java.parser.DescriptorNode;
import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class DefaultInterface extends AbstractDefaultInterface {
    private final GenericInfo info;
    private final String typedefName;

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

    @Contract("_ -> new")
    @Override
    @NotNull
    public TypeObject generify(@NotNull TypeObject... args) {
        return new GenerifiedDefaultInterface(this, info.generify(args));
    }

    @Contract("_ -> new")
    @Override
    @NotNull
    public TypeObject typedefAs(String name) {
        return new DefaultInterface(super.name(), info, operators, name);
    }

    @NotNull
    @Override
    public FunctionInfo operatorInfo(OpSpTypeNode o, DescriptorNode access) {
        return operators.get(o).boundify();
    }

    @NotNull
    @Override
    public TypeObject[] operatorReturnType(OpSpTypeNode o, DescriptorNode access) {
        return sanitizeType(internalReturnType(o));
    }

    public String name() {
        return typedefName.isEmpty() ? super.name() : typedefName;
    }

    private TypeObject internalReturnType(OpSpTypeNode o) {
        var result = operators.get(o).getReturns()[0];
        return result instanceof TemplateParam ? ((TemplateParam) result).getBound() : result;
    }

    public Map<OpSpTypeNode, FunctionInfo> getOperators() {
        return operators;
    }
}
