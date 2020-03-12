package main.java.converter;

import java.util.ArrayList;
import java.util.List;

public final class FunctionInfo {
    private final String name;
    private final ArgumentInfo arguments;
    private final TypeObject[] returns;

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

    public FunctionInfo boundify() {
        throw new UnsupportedOperationException();
    }

    public FunctionInfo generify(List<TypeObject> generics) {
        throw new UnsupportedOperationException();
    }
}
