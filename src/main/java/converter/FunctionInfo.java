package main.java.converter;

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
}
