package main.java.converter;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class FunctionInfo implements IntoFnInfo {
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
            var type = boundifyType(arr[i].getType());
            result[i] = new Argument(arr[i].getName(), type, arr[i].isVararg(), arr[i].getLineInfo());
        }
        return result;
    }

    @NotNull
    @Contract(pure = true)
    private TypeObject[] boundifyArray(@NotNull TypeObject[] arr) {
        var result = new TypeObject[arr.length];
        for (int i = 0; i < arr.length; i++) {
            result[i] = boundifyType(arr[i]);
        }
        return result;
    }

    private TypeObject boundifyType(TypeObject val) {
        if (val instanceof TemplateParam) {
            var template = (TemplateParam) val;
            if (template.getParent().sameBaseType(this.toCallable())) {
                return template.getBound();
            } else {
                return template;
            }
        } else {
            return val;
        }
    }

    @NotNull
    public FunctionInfo generify(TypeObject parent, List<TypeObject> args) {
        var posArgs = generifyArray(parent, arguments.getPositionArgs(), args);
        var normArgs = generifyArray(parent, arguments.getNormalArgs(), args);
        var kwArgs = generifyArray(parent, arguments.getKeywordArgs(), args);
        var argInfo = new ArgumentInfo(posArgs, normArgs, kwArgs);
        return new FunctionInfo(name, argInfo, generifyArray(parent, returns, args));
    }

    @NotNull
    private static Argument[] generifyArray(TypeObject parent, @NotNull Argument[] arr, List<TypeObject> generics) {
        var result = new Argument[arr.length];
        for (int i = 0; i < arr.length; i++) {
            var argType = arr[i].getType();
            TypeObject type = generifyType(argType, parent, generics);
            result[i] = new Argument(arr[i].getName(), type, arr[i].isVararg(), arr[i].getLineInfo());
        }
        return result;
    }

    @NotNull
    private static TypeObject[] generifyArray(TypeObject parent, @NotNull TypeObject[] arr, List<TypeObject> generics) {
        var result = new TypeObject[arr.length];
        for (int i = 0; i < arr.length; i++) {
            TypeObject type = generifyType(arr[i], parent, generics);
            result[i] = type;
        }
        return result;
    }

    private static TypeObject generifyType(TypeObject val, TypeObject parent, List<TypeObject> generics) {
        if (val instanceof TemplateParam) {
            var template = (TemplateParam) val;
            if (template.getParent().sameBaseType(parent)) {
                return generics.get(template.getIndex());
            } else {
                return template;
            }
        } else {
            return val.generifyWith(parent, generics);
        }
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
