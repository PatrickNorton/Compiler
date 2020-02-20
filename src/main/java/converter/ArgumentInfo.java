package main.java.converter;

import main.java.parser.TypedArgumentListNode;
import main.java.parser.TypedArgumentNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public final class ArgumentInfo implements Iterable<Argument> {
    private final Argument[] positionArgs;
    private final Argument[] normalArgs;
    private final Argument[] keywordArgs;

    public ArgumentInfo(Argument... normalArgs) {
        this(new Argument[0], normalArgs, new Argument[0]);
    }

    public ArgumentInfo(Argument[] positionArgs, Argument[] normalArgs, Argument[] keywordArgs) {
        this.positionArgs = positionArgs;
        this.normalArgs = normalArgs;
        this.keywordArgs = keywordArgs;
    }

    public boolean matches(Argument... values) {
        return true;
    }

    @NotNull
    public static ArgumentInfo of(@NotNull TypedArgumentListNode args, CompilerInfo info) {
        var posArgs = getArgs(info, args.getPositionArgs());
        var normalArgs = getArgs(info, args.getArgs());
        var kwArgs = getArgs(info, args.getNameArgs());
        return new ArgumentInfo(posArgs, normalArgs, kwArgs);
    }

    @NotNull
    private static Argument[] getArgs(CompilerInfo info, @NotNull TypedArgumentNode... args) {
        var result = new Argument[args.length];
        for (int i = 0; i < args.length; i++) {
            result[i] = new Argument(args[i].getName().getName(), info.getType(args[i].getType()));
        }
        return result;
    }

    @NotNull
    @Override
    public Iterator<Argument> iterator() {
        return new ArgIterator();
    }

    private class ArgIterator implements Iterator<Argument> {
        int next;

        @Override
        public boolean hasNext() {
            return next < positionArgs.length + normalArgs.length + keywordArgs.length;
        }

        @Override
        public Argument next() {
            if (next < positionArgs.length) {
                return positionArgs[next++];
            } else if (next < positionArgs.length + normalArgs.length) {
                return normalArgs[(next++) - positionArgs.length];
            } else {
                return keywordArgs[(next++) - positionArgs.length - normalArgs.length];
            }
        }
    }

    @NotNull
    @Contract("_ -> new")
    public static ArgumentInfo of(@NotNull TypeObject... args) {
        Argument[] resultArgs = new Argument[args.length];
        for (int i = 0; i < args.length; i++) {
            resultArgs[i] = new Argument("", args[i]);
        }
        return new ArgumentInfo(resultArgs);
    }
}
