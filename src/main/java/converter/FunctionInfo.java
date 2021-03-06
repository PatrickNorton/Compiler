package main.java.converter;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class FunctionInfo {
    private final String name;
    private final ArgumentInfo arguments;
    private final TypeObject[] returns;

    public FunctionInfo(TypeObject... returns) {
        this("", ArgumentInfo.of(), returns);
    }

    public FunctionInfo(ArgumentInfo args, TypeObject... returns) {
        this("", args, returns);
    }

    public FunctionInfo(String name, ArgumentInfo args, TypeObject... returns) {
        this.name = name;
        this.arguments = args;
        this.returns = returns;
    }

    public String getName() {
        return name;
    }

    public boolean matches(Argument... args) {
        return arguments.matches(args);
    }

    public TypeObject[] getReturns() {
        return returns;
    }

    public ArgumentInfo getArgs() {
        return arguments;
    }

    public TypeObject toCallable() {
        List<TypeObject> argTypes = new ArrayList<>(arguments.size() + 1);
        for (var arg : arguments) {
            argTypes.add(arg.getType());
        }
        argTypes.add(TypeObject.list(returns));
        return Builtins.CALLABLE.generify(argTypes.toArray(new TypeObject[0]));
    }

    @NotNull
    public FunctionInfo boundify() {
        var posArgs = boundifyArray(arguments.getPositionArgs());
        var normArgs = boundifyArray(arguments.getNormalArgs());
        var kwArgs = boundifyArray(arguments.getKeywordArgs());
        var argInfo = new ArgumentInfo(posArgs, normArgs, kwArgs);
        return new FunctionInfo(name, argInfo, boundifyArray(returns));
    }

    @NotNull
    @Contract(pure = true)
    private static Argument[] boundifyArray(@NotNull Argument[] arr) {
        var result = new Argument[arr.length];
        for (int i = 0; i < arr.length; i++) {
            var argType = arr[i].getType();
            var type = argType instanceof TemplateParam ? ((TemplateParam) argType).getBound() : argType;
            result[i] = new Argument(arr[i].getName(), type);
        }
        return result;
    }

    @NotNull
    @Contract(pure = true)
    private static TypeObject[] boundifyArray(@NotNull TypeObject[] arr) {
        var result = new TypeObject[arr.length];
        for (int i = 0; i < arr.length; i++) {
            result[i] = arr[i] instanceof TemplateParam ? ((TemplateParam) arr[i]).getBound() : arr[i];
        }
        return result;
    }

    @NotNull
    public FunctionInfo generify(List<TypeObject> args) {
        var posArgs = generifyArray(arguments.getPositionArgs(), args);
        var normArgs = generifyArray(arguments.getNormalArgs(), args);
        var kwArgs = generifyArray(arguments.getKeywordArgs(), args);
        var argInfo = new ArgumentInfo(posArgs, normArgs, kwArgs);
        return new FunctionInfo(name, argInfo, generifyArray(returns, args));
    }

    @NotNull
    private static Argument[] generifyArray(@NotNull Argument[] arr, List<TypeObject> generics) {
        var result = new Argument[arr.length];
        for (int i = 0; i < arr.length; i++) {
            var argType = arr[i].getType();
            var type = argType instanceof TemplateParam ? generics.get(((TemplateParam) argType).getIndex()) : argType;
            result[i] = new Argument(arr[i].getName(), type);
        }
        return result;
    }

    @NotNull
    private static TypeObject[] generifyArray(@NotNull TypeObject[] arr, List<TypeObject> generics) {
        var result = new TypeObject[arr.length];
        for (int i = 0; i < arr.length; i++) {
            result[i] = arr[i] instanceof TemplateParam ? generics.get(((TemplateParam) arr[i]).getIndex()) : arr[i];
        }
        return result;
    }
}
