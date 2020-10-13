package main.java.converter;

import main.java.parser.TypedArgumentListNode;
import main.java.parser.TypedArgumentNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

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

    /**
     * Checks if the list of arguments (representing arguments passed to a
     * function) match this function, or if they throw an exception.
     * <p>
     *     Arguments are checked as follows:
     *    <ul>
     *        <li>Args passed with names are logged and their types noted.
     *        This ensures they will not be double-defined later on.</li>
     *        <li>Each argument is checked in order:<br>
     *        Non-keyword arguments (members whose '{@code name}' attribute
     *        is empty) are matched against the next non-keyword item in the
     *        {@link ArgumentInfo}, and keyword items are matched against the
     *        map created in step 1.</li>
     *    </ul>
     * </p>
     *
     * @param args The arguments to check for validity
     * @return If they match this function signature
     */
    public boolean matches(@NotNull Argument... args) {
        var newArgs = expandTuples(args);
        Map<String, TypeObject> keywordMap = initKeywords(newArgs);
        int argNo = 0;
        for (var arg : positionArgs) {
            if (keywordMap.containsKey(arg.getName())) {
                return false;
            }
            while (!newArgs[argNo].getName().isEmpty()) {
                argNo++;
            }
            var passedArg = newArgs[argNo++];
            if (!arg.getType().isSuperclass(passedArg.getType())) {
                return false;
            }
        }
        for (var arg : normalArgs) {
            var name = arg.getName();
            if (keywordMap.containsKey(name)) {
                if (!arg.getType().isSuperclass(keywordMap.get(name))) {
                    return false;
                }
            } else {
                while (!newArgs[argNo].getName().isEmpty()) {
                    argNo++;
                }
                var passedArg = newArgs[argNo++];
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

    @NotNull
    private Argument[] expandTuples(@NotNull Argument... args) {
        List<Argument> result = new ArrayList<>(args.length);
        for (var arg : args) {
            if (arg.isVararg()) {
                if (arg.getType() instanceof TupleType) {
                    for (var generic : ((TupleType) arg.getType()).getGenerics()) {
                        result.add(new Argument("", generic));
                    }
                } else {
                    throw CompilerException.of("Illegal parameter expansion in argument", arg);
                }
            } else {
                result.add(arg);
            }
        }
        return result.toArray(new Argument[0]);
    }

    /**
     * Computes the position in the function's argument list relative to the
     * argument list passed.
     * <p>
     *     Several invariants are associated with this function:
     *     <ul>
     *         <li><code>this.{@link #matches}(args)</code>.</li>
     *         <li>Each number from {@code 0} to {@code arr.length - 1} occurs
     *         <i>exactly</i> once in {@code arr}.</li>
     *         <li>{@code arr[args[i]]} represents the index of where {@code
     *         args[i]} should be in the stack passed to the function at
     *         runtime.</li>
     *     </ul>
     *     In this code, {@code arr} represents the {@code int[]} returned by
     *     this function.
     * </p>
     *
     * @param args The arguments to get the final order
     * @return The array containing that order
     */
    @NotNull
    public int[] argPositions(@NotNull Argument... args) {
        Map<String, Integer> kwPositions = new HashMap<>();
        for (var arg : args) {
            if (!arg.getName().isEmpty()) {
                kwPositions.put(arg.getName(), 0);
            }
        }
        for (int i = 0; i < normalArgs.length; i++) {
            var name = normalArgs[i].getName();
            if (kwPositions.containsKey(name)) {
                kwPositions.put(name, i + positionArgs.length);
            }
        }
        for (int i = 0; i < keywordArgs.length; i++) {
            var name = keywordArgs[i].getName();
            if (kwPositions.containsKey(name)) {
                kwPositions.put(name, i + normalArgs.length + positionArgs.length);
            }
        }
        int[] result = new int[args.length];
        int nonKwPos = 0;
        for (int i = 0; i < args.length; i++) {
            var arg = args[i];
            if (!arg.getName().isEmpty()) {
                result[i] = kwPositions.get(arg.getName());
            } else {
                while (kwPositions.containsKey(get(nonKwPos).getName())) {
                    nonKwPos++;
                }
                result[i] = nonKwPos++;
            }
        }
        return result;
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

    public Argument get(int i) {
        if (i >= size()) {
            throw new IndexOutOfBoundsException(i);
        }
        if (i < positionArgs.length) {
            return positionArgs[i];
        } else if (i - positionArgs.length < normalArgs.length) {
            return normalArgs[i - positionArgs.length];
        } else {
            return keywordArgs[i - normalArgs.length - positionArgs.length];
        }
    }

    public Optional<Map<Integer, TypeObject>> generifyArgs(@NotNull FunctionInfo parent, Argument... args) {
        var par = parent.toCallable();
        var newArgs = expandTuples(args);
        if (size() == 0) {
            return newArgs.length == 0 ? Optional.of(Collections.emptyMap()) : Optional.empty();
        }
        Map<Integer, TypeObject> result = new HashMap<>();
        Map<String, TypeObject> keywordMap = initKeywords(newArgs);
        for (var arg : newArgs) {
            if (!arg.getName().isEmpty()) {
                keywordMap.put(arg.getName(), arg.getType());
            }
        }
        int argNo = 0;
        for (var arg : positionArgs) {
            if (keywordMap.containsKey(arg.getName())) {
                return Optional.empty();
            }
            while (!newArgs[argNo].getName().isEmpty()) {
                argNo++;
            }
            var passedArg = newArgs[argNo++];
            var argGenerics = passedArg.getType().generifyAs(par, arg.getType());
            if (argGenerics.isEmpty() || TypeObject.addGenericsToMap(argGenerics.orElseThrow(), result)) {
                return Optional.empty();
            }
        }
        for (var arg : normalArgs) {
            var name = arg.getName();
            if (keywordMap.containsKey(name)) {
                var argGenerics = keywordMap.get(name).generifyAs(par, arg.getType());
                if (argGenerics.isEmpty() || TypeObject.addGenericsToMap(argGenerics.orElseThrow(), result)) {
                    return Optional.empty();
                }
            } else {
                if (argNo >= newArgs.length) {
                    return Optional.empty();
                }
                while (!newArgs[argNo].getName().isEmpty()) {
                    argNo++;
                    if (argNo >= newArgs.length) {
                        return Optional.empty();
                    }
                }
                var passedArg = newArgs[argNo++];
                var argGenerics = passedArg.getType().generifyAs(par, arg.getType());
                if (argGenerics.isEmpty() || TypeObject.addGenericsToMap(argGenerics.orElseThrow(), result)) {
                    return Optional.empty();
                }
            }
        }
        for (var arg : keywordArgs) {
            if (keywordMap.containsKey(arg.getName())) {
                var argGenerics = keywordMap.get(arg.getName()).generifyAs(par, arg.getType());
                if (argGenerics.isEmpty() || TypeObject.addGenericsToMap(argGenerics.orElseThrow(), result)) {
                    return Optional.empty();
                }
            } else {
                return Optional.empty();  // TODO: Default values
            }
        }
        return Optional.of(result);
    }

    @NotNull
    public static ArgumentInfo of(@NotNull TypedArgumentListNode args, CompilerInfo info) {
        var posArgs = getArgs(info, args.getPositionArgs());
        var normalArgs = getArgs(info, args.getArgs());
        var kwArgs = getArgs(info, args.getNameArgs());
        return new ArgumentInfo(posArgs, normalArgs, kwArgs);
    }

    @NotNull
    public static Argument[] getArgs(CompilerInfo info, @NotNull TypedArgumentNode... args) {
        var result = new Argument[args.length];
        for (int i = 0; i < args.length; i++) {
            var arg = args[i];
            result[i] = new Argument(
                    arg.getName().getName(), info.getType(arg.getType()),
                    arg.getVararg(), arg.getLineInfo()
            );
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

    @NotNull
    private Map<String, TypeObject> initKeywords(@NotNull Argument[] args) {
        Map<String, TypeObject> keywordMap = new HashMap<>();
        for (var arg : args) {
            if (!arg.getName().isEmpty()) {
                keywordMap.put(arg.getName(), arg.getType());
            }
        }
        return keywordMap;
    }

    public String argStr() {
        var joiner = new StringJoiner(", ", "(", ")");
        if (positionArgs.length > 0) {
            for (var arg : positionArgs) {
                if (arg.isVararg()) {
                    joiner.add(String.format("*%s %s", arg.getType().name(), arg.getName()));
                } else {
                    joiner.add(String.format("%s %s", arg.getType().name(), arg.getName()));
                }
            }
            joiner.add("/");
        }
        if (normalArgs.length > 0) {
            for (var arg : normalArgs) {
                if (arg.isVararg()) {
                    joiner.add(String.format("*%s %s", arg.getType().name(), arg.getName()));
                } else {
                    joiner.add(String.format("%s %s", arg.getType().name(), arg.getName()));
                }
            }
        }
        if (keywordArgs.length > 0) {
            joiner.add("*");
            for (var arg : keywordArgs) {
                if (arg.isVararg()) {
                    joiner.add(String.format("*%s %s", arg.getType().name(), arg.getName()));
                } else {
                    joiner.add(String.format("%s %s", arg.getType().name(), arg.getName()));
                }
            }
        }
        return joiner.toString();
    }

    private class ArgIterator implements Iterator<Argument> {
        int next;

        @Override
        public boolean hasNext() {
            return next < size();
        }

        @Override
        public Argument next() {
            return get(next++);
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
