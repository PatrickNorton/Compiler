package main.java.converter;

import main.java.parser.DescriptorNode;
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
        var isGenerator = node.getDescriptors().contains(DescriptorNode.GENERATOR);
        var trueRet = isGenerator ? new TypeObject[] {Builtins.ITERABLE.generify(retTypes)} : retTypes;
        var fnInfo = new FunctionInfo(node.getName().getName(), isGenerator, convertArgs(generics), trueRet);
        int index = info.addFunction(new Function(fnInfo, bytes, isGenerator));
        var constVal = new FunctionConstant(node.getName().getName(), index);
        info.checkDefinition(node.getName().getName(), node);
        info.addVariable(node.getName().getName(), fnInfo.toCallable(), constVal, node);
        info.addStackFrame();
        addGenerics(generics);
        var retInfo = info.getFnReturns();
        retInfo.addFunctionReturns(isGenerator, retTypes);
        for (var arg : node.getArgs()) {
            info.addVariable(arg.getName().getName(), info.getType(arg.getType()), arg);
        }
        for (var statement : node.getBody()) {
            bytes.addAll(BaseConverter.bytes(bytes.size(), statement, info));
        }
        info.removeStackFrame();
        retInfo.popFnReturns();
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
            var argType = arg.getType();
            TypeObject type;
            if (generics.containsKey(argType.strName())) {
                type = generics.get(argType.strName());
            } else {
                type = info.getType(argType);
            }
            var mutability = argType.getMutability();
            if (mutability.isEmpty() || mutability.get().equals(DescriptorNode.MREF)) {
                converted[i] = new Argument(arg.getName().getName(), type.makeConst());
            } else {
                converted[i] = new Argument(arg.getName().getName(), type.makeMut());
            }
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
                    result.put(generic.strName(), new TemplateParam(generic.strName(), i, Builtins.OBJECT));
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
