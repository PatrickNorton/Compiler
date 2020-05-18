package main.java.converter;

import main.java.parser.TypedArgumentListNode;
import main.java.parser.TypedArgumentNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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

    public boolean matches(@NotNull Argument... args) {
        Map<String, TypeObject> keywordMap = new HashMap<>();
        for (var arg : args) {
            if (!arg.getName().isEmpty()) {
                keywordMap.put(arg.getName(), arg.getType());
            }
        }
        int argNo = 0;
        for (var arg : positionArgs) {
            while (!args[argNo].getName().isEmpty()) {
                argNo++;
            }
            var passedArg = args[argNo++];
            if (!arg.getType().isSuperclass(passedArg.getType())) {
                return false;
            }
        }
        for (var arg : normalArgs) {
            var name = arg.getName();
            if (keywordMap.containsKey(name)) {
                if (arg.getType().isSuperclass(keywordMap.get(name))) {
                    return false;
                }
            } else {
                while (!args[argNo].getName().isEmpty()) {
                    argNo++;
                }
                var passedArg = args[argNo++];
                if (!arg.getType().isSuperclass(passedArg.getType())) {
                    return false;
                }
            }
        }
        for (var arg : keywordArgs) {
            if (keywordMap.containsKey(arg.getName())) {
                if (!arg.getType().isSuperclass(keywordMap.get(arg.getName()))) {
                    return false;
                }
            } else {
                return false;  // TODO: Default values
            }
        }
        return true;
    }

    public Argument[] getKeywordArgs() {
        return keywordArgs;
    }

    public Argument[] getNormalArgs() {
        return normalArgs;
    }

    public Argument[] getPositionArgs() {
        return positionArgs;
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

    public int size() {
        return positionArgs.length + normalArgs.length + keywordArgs.length;
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
