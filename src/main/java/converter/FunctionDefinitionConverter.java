package main.java.converter;

import main.java.parser.DescriptorNode;
import main.java.parser.FunctionDefinitionNode;
import main.java.parser.TypedArgumentNode;
import main.java.util.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    @NotNull
    @Override
    public BytecodeList convert() {
        convertInner();
        return new BytecodeList();
    }

    @NotNull
    public BytecodeList convertDeprecated() {
        var fn = convertInner();
        fn.getInfo().setDeprecated(true);
        return new BytecodeList();
    }

    @NotNull
    public BytecodeList convertMustUse(String message) {
        var fn = convertInner();
        if (fn.getInfo().getReturns().length == 0) {
            throw CompilerException.of("$mustUse annotation requires function to return a value", node);
        }
        fn.getInfo().setMustUse(message);
        return new BytecodeList();
    }

    @Contract(" -> new")
    @NotNull
    public BytecodeList convertSys() {
        if (!info.permissions().isStdlib()) {
            throw CompilerException.of("'$native(\"sys\")' is only allowed in stdlib files", node);
        }
        assert node.getBody().isEmpty();
        var name = node.getName().getName();
        var fn = info.getFn(name);
        if (fn.isPresent()) {
            var func = fn.orElseThrow();
            var argc = func.getInfo().getArgs().size();
            var bytes = func.getBytes();
            assert bytes.isEmpty();
            bytes.add(Bytecode.SYSCALL, Syscalls.get(name), argc);
            if (func.getReturns().length > 0) {
                bytes.add(Bytecode.RETURN, func.getReturns().length);
            }
            return new BytecodeList();
        } else {
            throw CompilerInternalError.of("System function should always be predefined", node);
        }
    }

    private Function convertInner() {
        var name = node.getName().getName();
        var predefined = info.getFn(name);
        if (predefined.isPresent()) {
            convertPredefined(predefined.orElseThrow(), name);
            return predefined.orElseThrow();
        } else {
            return convertUndefined(name).getKey();
        }
    }

    private FunctionConstant convertWithConstant() {
        var name = node.getName().getName();
        var predefined = info.getFn(name);
        if (predefined.isPresent()) {
            return convertPredefined(predefined.orElseThrow(), name);
        } else {
            return convertUndefined(name).getValue();
        }
    }

    @NotNull
    private FunctionConstant convertPredefined(@NotNull Function fn, String name) {
        var fnInfo = fn.getInfo();
        var generics = fnInfo.getGenerics().getParamMap();
        var index = info.fnIndex(name);
        var bytes = fn.getBytes();
        assert bytes.isEmpty();
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
        return constVal;
    }

    @NotNull
    private Pair<Function, FunctionConstant> convertUndefined(String name) {
        var generics = getGenerics();
        var retTypes = info.typesOf(node.getRetval());
        var isGenerator = node.getDescriptors().contains(DescriptorNode.GENERATOR);
        var trueRet = isGenerator ? new TypeObject[]{iteratorType(retTypes)} : retTypes;
        var fnInfo = new FunctionInfo(name, isGenerator, convertArgs(generics), trueRet);
        var bytes = new BytecodeList();
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
        return Pair.of(fn, constVal);
    }

    private void convertBody(BytecodeList bytes, boolean isGenerator, TypeObject... retTypes) {
        var retInfo = info.getFnReturns();
        retInfo.addFunctionReturns(isGenerator, retTypes);
        addArgs();
        var pair = BaseConverter.bytesWithReturn(node.getBody(), info);
        bytes.addAll(pair.getKey());
        if (!isGenerator && retTypes.length > 0 && !pair.getValue().willReturn()) {
            CompilerWarning.warn("Function ends without returning", WarningType.NO_TYPE, info, node);
        }
        retInfo.popFnReturns();
    }

    private void addArgs() {
        for (var arg : node.getArgs()) {
            var type = info.getType(arg.getType());
            var argName = arg.getName().getName();
            if (arg.getVararg()) {
                info.addVariable(argName, Builtins.iterable().generify(arg, type), arg);
            } else {
                info.addVariable(argName, type, arg);
            }
        }
    }

    @Contract("_ -> new")
    @NotNull
    public Optional<Pair<TypeObject, Integer>> parseHeader(boolean isBuiltin) {
        if (!AnnotationConverter.shouldCompile(node, info, node.getAnnotations())) {
            return Optional.empty();
        }
        if (node.getGenerics().length == 0) {
            return Optional.of(innerHeader(GenericInfo.empty(), isBuiltin));
        } else {
            var generics = GenericInfo.parse(info, node.getGenerics());
            info.addLocalTypes(null, generics.getParamMap());
            var result = innerHeader(generics, isBuiltin);
            info.removeLocalTypes();
            for (var generic : generics) {
                generic.setParent(result.getKey());
            }
            return Optional.of(result);
        }
    }

    private Pair<TypeObject, Integer> innerHeader(@NotNull GenericInfo generics, boolean isBuiltin) {
        var argInfo = ArgumentInfo.of(node.getArgs(), info);
        var returns = info.typesOf(node.getRetval());
        var isGenerator = node.getDescriptors().contains(DescriptorNode.GENERATOR);
        var trueRet = isGenerator ? new TypeObject[] {iteratorType(returns)} : returns;
        var fnInfo = new FunctionInfo(node.getName().getName(), isGenerator, generics, argInfo, trueRet);
        var func = new Function(node, fnInfo, new BytecodeList());
        var previouslyDefined = info.getFn(func.getName());
        if (previouslyDefined.isPresent()) {
            throw CompilerException.doubleDef(func.getName(), previouslyDefined.orElseThrow(), node);
        }
        int index = isBuiltin ? -1 : info.addFunction(func);
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

    /*
    private method convert(dict[str, TemplateParam] generics, list[TypedArgumentNode] args) {
        final var converted = list(capacity=args.length)
        for arg in args {
            var argType = arg.getType()
            TypeObject type = generics.get(argType.strName()) ?? self.info.getType(argType)
            var mutability = MutableType.fromNullable(argType.getMutability())
            var properType = type.makeConst() if  mutability.isConstType() else type.makeMut()
            converted.add(Argument(arg.name.name, properType, arg.vararg, arg.lineInfo))
        }
        return converted
    }
     */

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
            var mutability = MutableType.fromNullable(argType.getMutability().orElse(null));
            var properType = mutability.isConstType() ? type.makeConst() : type.makeMut();
            converted[i] = new Argument(arg.getName().getName(), properType, arg.getVararg(), arg.getLineInfo());
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
    public static Optional<Pair<TypeObject, Integer>> parseHeader(
            CompilerInfo info, FunctionDefinitionNode node, boolean isBuiltin
    ) {
        return new FunctionDefinitionConverter(info, node).parseHeader(isBuiltin);
    }

    public static FunctionConstant convertWithConstant(CompilerInfo info, FunctionDefinitionNode node) {
        return new FunctionDefinitionConverter(info, node).convertWithConstant();
    }

    private static TypeObject iteratorType(TypeObject... params) {
        return Builtins.iterator().makeMut().generify(params);
    }
}
