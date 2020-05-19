package main.java.converter;

import main.java.parser.FunctionDefinitionNode;
import main.java.parser.TypeNode;
import main.java.parser.TypedArgumentNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FunctionDefinitionConverter implements BaseConverter {
    private final CompilerInfo info;
    private final FunctionDefinitionNode node;

    public FunctionDefinitionConverter(CompilerInfo info, FunctionDefinitionNode node) {
        this.info = info;
        this.node = node;
    }

    @NotNull
    @Override
    public @Unmodifiable List<Byte> convert(int start) {
        List<Byte> bytes = new ArrayList<>();
        var generics = getGenerics();
        var retTypes =  info.typesOf(node.getRetval());
        var fnInfo = new FunctionInfo(node.getName().getName(), convertArgs(generics), retTypes);
        int index = info.addFunction(new Function(fnInfo, bytes));
        var constVal = new FunctionConstant(node.getName().getName(), index);
        info.checkDefinition(node.getName().getName(), node);
        info.addVariable(node.getName().getName(), fnInfo.toCallable(), constVal, node);
        info.addStackFrame();
        addGenerics(generics);
        info.addFunctionReturns(retTypes);
        for (var arg : node.getArgs()) {
            info.addVariable(arg.getName().getName(), info.getType(arg.getType()), arg);
        }
        for (var statement : node.getBody()) {
            bytes.addAll(BaseConverter.bytes(bytes.size(), statement, info));
        }
        info.removeStackFrame();
        info.popFnReturns();
        return Collections.emptyList();
    }

    @NotNull
    private ArgumentInfo convertArgs(Map<String, TemplateParam> generics) {
        var args = node.getArgs();
        var kwargs = convert(generics, args.getNameArgs());
        var normalArgs = convert(generics, args.getArgs());
        var posArgs = convert(generics, args.getPositionArgs());
        return new ArgumentInfo(kwargs, normalArgs, posArgs);
    }

    @NotNull
    private Argument[] convert(Map<String, TemplateParam> generics, @NotNull TypedArgumentNode[] args) {
        var converted = new Argument[args.length];
        for (int i = 0; i < args.length; i++) {
            var arg = args[i];
            TypeObject type;
            if (generics.containsKey(arg.getType().strName())) {
                type = generics.get(arg.getType().strName());
            } else {
                type = info.getType(arg.getType());
            }
            converted[i] = new Argument(arg.getName().getName(), type);
        }
        return converted;
    }

    @NotNull
    private Map<String, TemplateParam> getGenerics() {
        var generics = node.getGenerics();
        Map<String, TemplateParam> result = new HashMap<>();
        for (int i = 0; i < generics.length; i++) {
            var generic = generics[i];
            if (generic instanceof TypeNode) {
                if (generic.getSubtypes().length == 0) {
                    result.put(generic.strName(), new TemplateParam(generic.strName(), i, false));
                } else {
                    var bounds = TypeObject.union(info.typesOf(generic.getSubtypes()));
                    result.put(generic.strName(), new TemplateParam(generic.strName(), i, bounds));
                }
            } else {
                throw CompilerException.of(
                        "Function template params may not contain unions or intersections", generic
                );
            }
        }
        return result;
    }

    private void addGenerics(@NotNull Map<String, TemplateParam> generics) {
        for (var pair : generics.entrySet()) {
            info.addType(pair.getValue());
            info.addVariable(pair.getKey(), pair.getValue(), node);
        }
    }
}
