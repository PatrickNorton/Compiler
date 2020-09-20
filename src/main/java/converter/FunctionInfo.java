package main.java.converter;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class FunctionInfo implements IntoFnInfo, Template<FunctionInfo> {
    private final String name;
    private final boolean isGenerator;
    private final ArgumentInfo arguments;
    private final TypeObject[] returns;

    public FunctionInfo(TypeObject... returns) {
        this("", ArgumentInfo.of(), returns);
    }

    public FunctionInfo(String name, TypeObject... returns) {
        this(name, ArgumentInfo.of(), returns);
    }

    public FunctionInfo(ArgumentInfo args, TypeObject... returns) {
        this("", args, returns);
    }

    public FunctionInfo(String name, ArgumentInfo args, TypeObject... returns) {
        this(name, false, args, returns);
    }

    public FunctionInfo(String name, boolean isGenerator, ArgumentInfo args, TypeObject... returns) {
        this.name = name;
        this.isGenerator = isGenerator;
        this.arguments = args;
        this.returns = properReturns(returns);
    }

    public String getName() {
        return name;
    }

    public boolean matches(Argument... args) {
        return arguments.matches(args);
    }

    public TypeObject[] getReturns() {
        if (returns.length == 1 && returns[0] instanceof ListTypeObject) {
            return ((ListTypeObject) returns[0]).toArray();
        } else {
            return returns;
        }
    }

    public ArgumentInfo getArgs() {
        return arguments;
    }

    public boolean isGenerator() {
        return isGenerator;
    }

    @NotNull
    @Contract(value = " -> new", pure = true)
    public TypeObject toCallable() {
        return new FunctionInfoType(this);
    }

    @Override
    public FunctionInfo intoFnInfo() {
        return this;
    }

    @NotNull
    public FunctionInfo boundify() {
        var posArgs = boundifyArray(arguments.getPositionArgs());
        var normArgs = boundifyArray(arguments.getNormalArgs());
        var kwArgs = boundifyArray(arguments.getKeywordArgs());
        var argInfo = new ArgumentInfo(posArgs, normArgs, kwArgs);
        return new FunctionInfo(name, isGenerator, argInfo, boundifyArray(returns));
    }

    @NotNull
    @Contract(pure = true)
    private Argument[] boundifyArray(@NotNull Argument[] arr) {
        var result = new Argument[arr.length];
        for (int i = 0; i < arr.length; i++) {
            var argType = arr[i].getType();
            TypeObject type;
            if (argType instanceof TemplateParam) {
                var template = (TemplateParam) argType;
                if (template.getParent().sameBaseType(this.toCallable())) {
                    type = template.getBound();
                } else {
                    type = template;
                }
            } else {
                type = argType;
            }
            result[i] = new Argument(arr[i].getName(), type, arr[i].isVararg(), arr[i].getLineInfo());
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

    @Override
    @NotNull
    public FunctionInfo generify(TypeObject... generics) {
        return generify(Arrays.asList(generics));
    }

    public FunctionInfo generify(TypeObject parent, List<TypeObject> args) {
        var posArgs = generifyArray(parent, arguments.getPositionArgs(), args);
        var normArgs = generifyArray(parent, arguments.getNormalArgs(), args);
        var kwArgs = generifyArray(parent, arguments.getKeywordArgs(), args);
        var argInfo = new ArgumentInfo(posArgs, normArgs, kwArgs);
        return new FunctionInfo(name, argInfo, generifyArray(parent, returns, args));
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
            var argType = arr[i].getType();  // TODO: Proper generification of subtypes (e.g. list[T] => list[something])
            var type = argType instanceof TemplateParam ? generics.get(((TemplateParam) argType).getIndex()) : argType;
            result[i] = new Argument(arr[i].getName(), type, arr[i].isVararg(), arr[i].getLineInfo());
        }
        return result;
    }

    @NotNull
    private static Argument[] generifyArray(TypeObject parent, @NotNull Argument[] arr, List<TypeObject> generics) {
        var result = new Argument[arr.length];
        for (int i = 0; i < arr.length; i++) {
            var argType = arr[i].getType();
            TypeObject type;
            if (argType instanceof TemplateParam) {
                var template = (TemplateParam) argType;
                if (template.getParent().sameBaseType(parent)) {
                    type = generics.get(template.getIndex());
                } else {
                    type = template;
                }
            } else {
                type = argType.generifyWith(parent, generics);
            }
            result[i] = new Argument(arr[i].getName(), type, arr[i].isVararg(), arr[i].getLineInfo());
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

    @NotNull
    private static TypeObject[] generifyArray(TypeObject parent, @NotNull TypeObject[] arr, List<TypeObject> generics) {
        var result = new TypeObject[arr.length];
        for (int i = 0; i < arr.length; i++) {
            TypeObject type;
            if (arr[i] instanceof TemplateParam) {
                var template = (TemplateParam) arr[i];
                if (template.getParent().sameBaseType(parent)) {
                    type = generics.get(template.getIndex());
                } else {
                    type = template;
                }
            } else {
                type = arr[i].generifyWith(parent, generics);
            }
            result[i] = type;
        }
        return result;
    }

    public Optional<Map<Integer, TypeObject>> generifyArgs(Argument... args) {
        return arguments.generifyArgs(this, args);
    }

    private static TypeObject[] properReturns(@NotNull TypeObject... returns) {
        if (returns.length == 1 && returns[0] instanceof ListTypeObject) {
            return ((ListTypeObject) returns[0]).toArray();
        } else {
            return returns;
        }
    }
}
