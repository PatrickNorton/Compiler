package main.java.converter;

import main.java.parser.DescriptorNode;
import main.java.parser.FunctionDefinitionNode;
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
    @Unmodifiable
    public List<Byte> convert(int start) {
        convertInner();
        return Collections.emptyList();
    }

    public List<Byte> convertDeprecated() {
        var fn = convertInner();
        fn.getInfo().setDeprecated(true);
        return Collections.emptyList();
    }

    private Function convertInner() {
        var name = node.getName().getName();
        var predefined = info.getFn(name);
        if (predefined.isPresent()) {
            convertPredefined(predefined.orElseThrow(), name);
            return predefined.orElseThrow();
        } else {
            return convertUndefined(name);
        }
    }

    private void convertPredefined(Function fn, String name) {
        var fnInfo = fn.getInfo();
        var generics = fnInfo.getGenerics().getParamMap();
        var index = info.fnIndex(name);
        var bytes = fn.getBytes();
        if (!bytes.isEmpty()) {
            throw CompilerException.doubleDef(name, fn, node);
        }
        var constVal = new FunctionConstant(name, index);
        var fnRets = fnInfo.getReturns();
        var isGen = fnInfo.isGenerator();
        var trueRet = isGen ? Builtins.deIterable(fnRets[0]) : fnRets;
        info.checkDefinition(name, node);
        var varHolder = info.varHolder();
        varHolder.addVariable(name, fnInfo.toCallable(), constVal, node);
        varHolder.addStackFrame();
        checkGen();
        varHolder.addLocalTypes(fnInfo.toCallable(), generics);
        convertBody(bytes, isGen, trueRet);
        varHolder.removeLocalTypes();
        varHolder.removeStackFrame();
        var maxSize = varHolder.resetMax();
        fn.setMax(maxSize);
    }

    private Function convertUndefined(String name) {
        var generics = getGenerics();
        var retTypes = info.typesOf(node.getRetval());
        var isGenerator = node.getDescriptors().contains(DescriptorNode.GENERATOR);
        var trueRet = isGenerator ? new TypeObject[]{Builtins.ITERABLE.generify(retTypes)} : retTypes;
        var fnInfo = new FunctionInfo(name, isGenerator, convertArgs(generics), trueRet);
        List<Byte> bytes = new ArrayList<>();
        var fn = new Function(node, fnInfo, bytes);
        var index = info.addFunction(fn);
        for (var generic : generics.values()) {
            generic.setParent(fnInfo.toCallable());
        }
        var constVal = new FunctionConstant(name, index);
        info.checkDefinition(name, node);
        var varHolder = info.varHolder();
        varHolder.addVariable(name, fnInfo.toCallable(), constVal, node);
        varHolder.addStackFrame();
        checkGen();
        varHolder.addLocalTypes(fnInfo.toCallable(), new HashMap<>(generics));
        convertBody(bytes, isGenerator, retTypes);
        varHolder.removeLocalTypes();
        varHolder.removeStackFrame();
        return fn;
    }

    private void convertBody(List<Byte> bytes, boolean isGenerator, TypeObject... retTypes) {
        var retInfo = info.getFnReturns();
        retInfo.addFunctionReturns(isGenerator, retTypes);
        addArgs();
        for (var statement : node.getBody()) {
            bytes.addAll(BaseConverter.bytes(bytes.size(), statement, info));
        }
        retInfo.popFnReturns();
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
            return innerHeader(GenericInfo.empty());
        } else {
            var generics = GenericInfo.parse(info, node.getGenerics());
            info.addLocalTypes(null, generics.getParamMap());
            var result = innerHeader(generics);
            info.removeLocalTypes();
            for (var generic : generics) {
                generic.setParent(result.getKey());
            }
            return result;
        }
    }

    private Pair<TypeObject, Integer> innerHeader(@NotNull GenericInfo generics) {
        var argInfo = ArgumentInfo.of(node.getArgs(), info);
        var returns = info.typesOf(node.getRetval());
        var isGenerator = node.getDescriptors().contains(DescriptorNode.GENERATOR);
        var trueRet = isGenerator ? new TypeObject[] {Builtins.ITERABLE.generify(returns)} : returns;
        var fnInfo = new FunctionInfo(node.getName().getName(), isGenerator, generics, argInfo, trueRet);
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
        return TemplateParam.parseGenerics(info, node.getGenerics());
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
