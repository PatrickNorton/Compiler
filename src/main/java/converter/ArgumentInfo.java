package main.java.converter;

public final class ArgumentInfo {
    private final Argument[] positionArgs;
    private final Argument[] normalArgs;
    private final Argument[] keywordArgs;

    public ArgumentInfo(Argument[] positionArgs, Argument[] normalArgs, Argument[] keywordArgs) {
        this.positionArgs = positionArgs;
        this.normalArgs = normalArgs;
        this.keywordArgs = keywordArgs;
    }

    public boolean matches(Argument... values) {
        return true;
    }
}