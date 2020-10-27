package main.java.converter;

import main.java.parser.DescriptorNode;
import main.java.parser.FunctionDefinitionNode;
import main.java.parser.TypeNode;
import main.java.parser.TypedArgumentNode;
import main.java.util.Pair;
import org.jetbrains.annotations.Contract;
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
        var name = node.getName().getName();
        var generics = getGenerics();
        var retTypes =  info.typesOf(node.getRetval());
        var isGenerator = node.getDescriptors().contains(DescriptorNode.GENERATOR);
        var trueRet = isGenerator ? new TypeObject[] {Builtins.ITERABLE.generify(retTypes)} : retTypes;
        var predefined = info.getFn(name);
        int index;
        List<Byte> bytes;
        FunctionInfo fnInfo;
        if (predefined.isPresent()) {
            var fn = predefined.orElseThrow();
            fnInfo = fn.getInfo();
            index = info.fnIndex(name);
            bytes = fn.getBytes();
            if (!bytes.isEmpty()) {
                throw CompilerException.doubleDef(name, fn, node);
            }
        } else {
            fnInfo = new FunctionInfo(name, isGenerator, convertArgs(generics), trueRet);
            bytes = new ArrayList<>();
            index = info.addFunction(new Function(node, fnInfo, bytes));
        }
        for (var generic : generics.values()) {
            generic.setParent(fnInfo.toCallable());
        }
        var constVal = new FunctionConstant(name, index);
        info.checkDefinition(name, node);
        info.addVariable(name, fnInfo.toCallable(), constVal, node);
        info.addStackFrame();
        checkGen();
        addGenerics(generics);
        var retInfo = info.getFnReturns();
        retInfo.addFunctionReturns(isGenerator, retTypes);
        addArgs();
        for (var statement : node.getBody()) {
            bytes.addAll(BaseConverter.bytes(bytes.size(), statement, info));
        }
        info.removeStackFrame();
        retInfo.popFnReturns();
        return Collections.emptyList();
    }

    private void addArgs() {
        for (var arg : node.getArgs()) {
            var type = info.getType(arg.getType());
            var argName = arg.getName().getName();
            if (arg.getVararg()) {
                info.addVariable(argName, Builtins.ITERABLE.generify(arg, type), arg);
            } else {
                info.addVariable(argName, type, arg);
            }
        }
    }

    @Contract(" -> new")
    @NotNull
    public Pair<TypeObject, Integer> parseHeader() {
        if (node.getGenerics().length == 0) {
            return innerHeader();
        } else {
            var generics = GenericInfo.parse(info, node.getGenerics());
            Map<String, TypeObject> genericNames = new HashMap<>(generics.size());
            for (var generic : generics) {
                genericNames.put(generic.baseName(), generic);
            }
            info.addLocalTypes(genericNames);
            var result = innerHeader();
            info.removeLocalTypes();
            return result;
        }
    }

    private Pair<TypeObject, Integer> innerHeader() {
        var argInfo = ArgumentInfo.of(node.getArgs(), info);
        var returns = info.typesOf(node.getRetval());
        var isGenerator = node.getDescriptors().contains(DescriptorNode.GENERATOR);
        var trueRet = isGenerator ? new TypeObject[] {Builtins.ITERABLE.generify(returns)} : returns;
        var fnInfo = new FunctionInfo(node.getName().getName(), isGenerator, argInfo, trueRet);
        var func = new Function(node, fnInfo, new ArrayList<>());
        int index = info.addFunction(func);
        return Pair.of(new FunctionInfoType(fnInfo), index);
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
                converted[i] = new Argument(arg.getName().getName(), type.makeConst(), arg.getVararg(), arg.getLineInfo());
            } else {
                converted[i] = new Argument(arg.getName().getName(), type.makeMut(), arg.getVararg(), arg.getLineInfo());
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

    private void checkGen() {
        if (node.getDescriptors().contains(DescriptorNode.GENERATOR) && node.getRetval().length == 0) {
            throw CompilerException.of("Generator functions must have at least one return", node);
        }
    }

    @NotNull
    public static Pair<TypeObject, Integer> parseHeader(CompilerInfo info, FunctionDefinitionNode node) {
        return new FunctionDefinitionConverter(info, node).parseHeader();
    }
}
