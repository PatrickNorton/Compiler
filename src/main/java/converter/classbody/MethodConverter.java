package main.java.converter.classbody;

import main.java.converter.AccessLevel;
import main.java.converter.ArgumentInfo;
import main.java.converter.Builtins;
import main.java.converter.CompilerException;
import main.java.converter.CompilerInfo;
import main.java.converter.FunctionInfo;
import main.java.converter.GenericInfo;
import main.java.converter.TypeObject;
import main.java.parser.DescriptorNode;
import main.java.parser.GenericFunctionNode;
import main.java.parser.Lined;
import main.java.parser.MethodDefinitionNode;
import main.java.parser.StatementBodyNode;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class MethodConverter {
    private final Map<String, RawMethod> methodMap;
    private final Map<String, RawMethod> staticMethods;
    private final CompilerInfo info;

    public MethodConverter(CompilerInfo info) {
        this.info = info;
        methodMap = new HashMap<>();
        staticMethods = new HashMap<>();
    }

    public void parse(@NotNull MethodDefinitionNode node) {
        var generics = GenericInfo.parse(info, node.getGenerics());
        info.addLocalTypes(null, generics.getParamMap());
        var name = methodName(node);
        var args = ArgumentInfo.of(node.getArgs(), info);
        var returns = info.typesOf(node.getRetval());
        var isGen = node.getDescriptors().contains(DescriptorNode.GENERATOR);
        var trueRet = isGen ? new TypeObject[] {Builtins.ITERABLE.generify(returns)} : returns;
        var fnInfo = new FunctionInfo(name, isGen, generics, args, trueRet);
        var accessLevel = AccessLevel.fromDescriptors(node.getDescriptors());
        var isMut = node.getDescriptors().contains(DescriptorNode.MUT);
        info.removeLocalTypes();
        generics.setParent(fnInfo.toCallable());
        if (isGen && returns.length == 0) {
            throw generatorError(node);
        }
        checkVars(name, node, methodMap);
        checkVars(name, node, staticMethods);
        var mInfo = new RawMethod(accessLevel, isMut, fnInfo, node.getBody(), node.getLineInfo());
        if (!node.getDescriptors().contains(DescriptorNode.STATIC)) {
            methodMap.put(name, mInfo);
        } else {
            staticMethods.put(name, mInfo);
        }
    }

    public void parse(@NotNull GenericFunctionNode node) {
        var name = node.getName().getName();
        var args = ArgumentInfo.of(node.getArgs(), info);
        var returns = info.typesOf(node.getRetvals());
        var isGen = node.getDescriptors().contains(DescriptorNode.GENERATOR);
        var fnInfo = new FunctionInfo(name, isGen, args, returns);
        var descriptors = node.getDescriptors();
        var accessLevel = AccessLevel.fromDescriptors(descriptors);
        var isMut = descriptors.contains(DescriptorNode.MUT);
        if (isGen && returns.length == 0) {
            throw generatorError(node);
        }
        checkVars(name, node, methodMap);
        checkVars(name, node, staticMethods);
        var mInfo = new RawMethod(accessLevel, isMut, fnInfo, StatementBodyNode.empty(), node.getLineInfo());
        if (!node.getDescriptors().contains(DescriptorNode.STATIC)) {
            methodMap.put(name, mInfo);
        } else {
            staticMethods.put(name, mInfo);
        }
    }

    public void addUnionMethods(@NotNull Map<String, RawMethod> variantMethods) {
        for (var pair : variantMethods.entrySet()) {
            checkVars(pair.getKey(), pair.getValue(), staticMethods);
        }
        staticMethods.putAll(variantMethods);
    }

    public Map<String, RawMethod> getMethods() {
        return methodMap;
    }

    public Map<String, RawMethod> getStaticMethods() {
        return staticMethods;
    }

    private String methodName(@NotNull MethodDefinitionNode node) {
        return node.getName().getName(); // TODO: Distinguish between args
    }

    @NotNull
    private static CompilerException generatorError(Lined node) {
        return CompilerException.of("Generator functions must have at least one return", node);
    }

    private static void checkVars(String strName, Lined name, @NotNull Map<String, ? extends Lined> vars) {
        if (vars.containsKey(strName)) {
            throw CompilerException.doubleDef(strName, name, vars.get(strName));
        }
    }
}
