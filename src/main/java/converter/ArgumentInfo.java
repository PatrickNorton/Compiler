package main.java.converter;

import main.java.parser.TypedArgumentListNode;
import main.java.parser.TypedArgumentNode;
import main.java.util.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.StringJoiner;

public final class ArgumentInfo implements Iterable<Argument> {
    private final Argument[] positionArgs;
    private final Argument[] normalArgs;
    private final Argument[] keywordArgs;

    public ArgumentInfo(Argument... normalArgs) {
        this(new Argument[0], normalArgs, new Argument[0]);
    }

    public ArgumentInfo(Argument[] positionArgs, Argument[] normalArgs, Argument[] keywordArgs) {
        this.positionArgs = deList(positionArgs);
        this.normalArgs = deList(normalArgs);
        this.keywordArgs = deList(keywordArgs);
    }

    /**
     * Checks if the list of arguments (representing arguments passed to a
     * function) match this function, or if they throw an exception.
     * <p>
     *     Arguments are checked as follows:
     *    <ol>
     *        <li>Tuples with a vararg are expanded.</li>
     *        <li>Named arguments are matched with each other and
     *        type-checked.</li>
     *        <li>Keyword-only arguments are checked to ensure all of them have
     *        either a match or a corresponding default argument.</li>
     *        <li>The number of arguments are counted and the number of
     *        arguments using default values is determined and checked.</li>
     *        <li>All remaining arguments are type-checked.</li>
     *    </ol>
     * </p>
     *
     * @param args The arguments to check for validity
     * @return If they match this function signature
     */
    public boolean matches(@NotNull Argument... args) {
        var newArgs = expandTuples(args);
        var keywordMap = initKeywords(newArgs);
        if (!checkKeywordArgs(keywordMap)) {
            return false;
        }
        var defaultCount = argsWithDefaults(keywordMap.keySet());
        var nonKeywordCount = newArgs.length - keywordMap.size();
        var unused = size() - keywordMap.size();
        var defaultsUsed = unused - nonKeywordCount;
        if (defaultsUsed < 0 || defaultsUsed > defaultCount) {
            return false;
        }
        var defaultsUnused = defaultCount - defaultsUsed;
        var nextArg = nextEligibleArg(0, keywordMap.keySet(), defaultsUnused > 0);
        for (var arg : newArgs) {
            if (keywordMap.containsKey(arg.getName())) {
                continue;
            } else if (nextArg >= size()) {
                return false;
            }
            var argValue = get(nextArg);
            if (!argValue.getType().isSuperclass(arg.getType())) {
                return false;
            }
            if (argValue.getDefaultValue().isPresent()) {
                defaultsUnused -= 1;
            }
            nextArg = nextEligibleArg(nextArg + 1, keywordMap.keySet(), defaultsUnused > 0);
        }
        return true;
    }

    @NotNull
    private Argument[] expandTuples(@NotNull Argument... args) {
        List<Argument> result = new ArrayList<>(args.length);
        for (var arg : args) {
            if (arg.isVararg()) {
                if (!arg.getName().isEmpty()) {
                    throw CompilerException.of(
                            "Illegal parameter expansion in argument: Named arguments cannot be expanded", arg
                    );
                }
                if (arg.getType() instanceof TupleType) {
                    for (var generic : ((TupleType) arg.getType()).getGenerics()) {
                        result.add(new Argument("", generic));
                    }
                } else {
                    throw CompilerException.format(
                            "Illegal parameter expansion in argument: type '%s' is not a tuple",
                            arg, arg.getType().name()
                    );
                }
            } else {
                result.add(arg);
            }
        }
        return result.toArray(new Argument[0]);
    }

    private boolean checkKeywordArgs(Map<String, TypeObject> keywords) {
        // Check if all given keyword arguments are subclasses of the declaration
        var matchedCount = 0;
        for (var arg : allKeywords()) {
            if (keywords.containsKey(arg.getName())) {
                var keywordType = keywords.get(arg.getName());
                if (!arg.getType().isSuperclass(keywordType)) {
                    return false;
                } else {
                    matchedCount++;
                }
            }
        }
        // Ensure the number of matched keywords is the same as the total number of keywords
        if (matchedCount != keywords.size()) {
            return false;
        }
        // Check that all keyword-only arguments are either used or have a default
        for (var arg : keywordArgs) {
            if (!keywords.containsKey(arg.getName()) && arg.getDefaultValue().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private int argsWithDefaults(Set<String> toExclude) {
        var count = 0;
        for (var arg : this) {
            if (!toExclude.contains(arg.getName()) && arg.getDefaultValue().isPresent()) {
                count++;
            }
        }
        return count;
    }

    private int nextEligibleArg(int nextUnused, Set<String> keywordArgs, boolean defaultsRemaining) {
        for (; nextUnused < size(); nextUnused++) {
            var currentArg = get(nextUnused);
            if (keywordArgs.contains(currentArg.getName())) {
                continue;
            }
            if (defaultsRemaining || currentArg.getDefaultValue().isEmpty()) {
                return nextUnused;
            }
        }
        return nextUnused;
    }

    /**
     * Computes the position in the function's argument list relative to the
     * argument list passed.
     * <p>
     *     Several invariants are associated with this function:
     *     <ul>
     *         <li><code>this.{@link #matches}(args)</code>.</li>
     *         <li>Each number from {@code 0} to {@code args.length - 1} occurs
     *         <i>exactly</i> once in {@code arr}.</li>
     *         <li>If {@code arr[i]} is a number, it contains the index of the
     *         value such that {@code args[arr[i]]} should be moved to position
     *         {@code i} before the call is made.</li>
     *         <li>Otherwise, {@code arr[i]} contains the default value that
     *         should be added in that position when setting up for the
     *         call.</li>
     *     </ul>
     *     In this code, {@code arr} represents the {@code int[]} returned by
     *     this function.
     * </p>
     *
     * @param args The arguments to get the final order
     * @return The array containing that order
     */
    public ArgPosition[] argPositions(@NotNull Argument... args) {
        var newArgs = expandTuples(args);
        Map<String, Integer> kwPositions = new HashMap<>();
        for (int i = 0; i < newArgs.length; i++) {
            var arg = newArgs[i];
            if (!arg.getName().isEmpty()) {
                kwPositions.put(arg.getName(), i);
            }
        }
        var defaultCount = argsWithDefaults(kwPositions.keySet());
        var nonKeywordCount = newArgs.length - kwPositions.size();
        var unused = size() - kwPositions.size();
        var defaultsUsed = unused - nonKeywordCount;
        var defaultsUnused = defaultCount - defaultsUsed;
        ArgPosition[] result = new ArgPosition[size()];
        for (int i = 0, argNo = 0; i < size(); i++) {
            var valueArg = this.get(i);
            if (kwPositions.containsKey(valueArg.getName())) {
                result[i] = new ArgPosition(kwPositions.get(valueArg.getName()));
            }  else if (valueArg.getDefaultValue().isEmpty()) {
                result[i] = new ArgPosition(argNo++);
            } else if (defaultsUnused > 0) {
                result[i] = new ArgPosition(argNo++);
                defaultsUnused -= 1;
            } else {
                var defaultVal = valueArg.getDefaultValue().orElseThrow();
                result[i] = new ArgPosition(defaultVal);
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

    /**
     * Computes the generics necessary for the given parameters to call the
     * function.
     * <p>
     *     This returns two things: First, a {@code Map} for use in {@link
     *     TypeObject#generifyWith(TypeObject, List)}, and a {@code Set}
     *     containing the argument indices which will require a {@link
     *     Bytecode#MAKE_OPTION} before passed.
     * </p>
     *
     * @param parent The parent info to generify with (should be the case that
     *               {@code parent.getArgs() == this}
     * @param args The arguments passed to the function
     * @return The values, or {@link Optional#empty()} if {@code
     *         !this.matches(args)}
     */
    public Optional<Pair<Map<Integer, TypeObject>, Set<Integer>>> generifyArgs(
            @NotNull FunctionInfo parent, Argument... args
    ) {
        var par = parent.toCallable();
        var newArgs = expandTuples(args);
        var keywordMap = initKeywords(newArgs);
        Map<Integer, TypeObject> result = new HashMap<>();
        Set<Integer> needsMakeOption = new HashSet<>();
        // Check if all given keyword arguments are subclasses of the declaration
        var matchedCount = 0;
        for (var arg : allKeywords()) {
            if (keywordMap.containsKey(arg.getName())) {
                var keywordType = keywordMap.get(arg.getName());
                if (!arg.getType().isSuperclass(keywordType)) {
                    return Optional.empty();
                } else if (update(indexOf(arg.getName()), par, result, needsMakeOption, arg, keywordType)) {
                    return Optional.empty();
                } else {
                    matchedCount++;
                }
            }
        }
        // Ensure the number of matched keywords is the same as the total number of keywords
        if (matchedCount != keywordMap.size()) {
            return Optional.empty();
        }
        // Check that all keyword-only arguments are either used or have a default
        for (var arg : keywordArgs) {
            if (!keywordMap.containsKey(arg.getName()) && arg.getDefaultValue().isEmpty()) {
                return Optional.empty();
            }
        }
        var defaultCount = argsWithDefaults(keywordMap.keySet());
        var nonKeywordCount = newArgs.length - keywordMap.size();
        var unused = size() - keywordMap.size();
        var defaultsUsed = unused - nonKeywordCount;
        if (defaultsUsed < 0 || defaultsUsed > defaultCount) {
            return Optional.empty();
        }
        var defaultsUnused = defaultCount - defaultsUsed;
        var nextArg = nextEligibleArg(0, keywordMap.keySet(), defaultsUnused > 0);
        for (var arg : newArgs) {
            if (keywordMap.containsKey(arg.getName())) {
                continue;
            } else if (nextArg >= size()) {
                return Optional.empty();
            }
            var argValue = get(nextArg);
            if (update(indexOf(arg.getName()), par, result, needsMakeOption, argValue, argValue.getType())) {
                return Optional.empty();
            }
            if (argValue.getDefaultValue().isPresent()) {
                defaultsUnused -= 1;
            }
            nextArg = nextEligibleArg(nextArg + 1, keywordMap.keySet(), defaultsUnused > 0);
        }
        return Optional.of(Pair.of(result, needsMakeOption));
    }

    private static boolean update(
            int i, TypeObject par, Map<Integer, TypeObject> result,
            Set<Integer> needsMakeOption, @NotNull Argument arg, TypeObject passedType
    ) {
        var argGenerics = arg.getType().generifyAs(par, passedType);
        if (argGenerics.isEmpty()) {
            if (OptionTypeObject.needsAndSuper(arg.getType(), passedType)) {
                needsMakeOption.add(i);
                return false;
            }
            var optionGenerics = arg.getType().generifyAs(par, TypeObject.optional(passedType));
            if (optionGenerics.isEmpty() || TypeObject.addGenericsToMap(optionGenerics.orElseThrow(), result)) {
                return true;
            } else {
                needsMakeOption.add(i);
                return false;
            }
        } else {
            return TypeObject.addGenericsToMap(argGenerics.orElseThrow(), result);
        }
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
            if (arg.getDefaultVal().isEmpty()) {
                result[i] = new Argument(
                        arg.getName().getName(), info.getType(arg.getType()),
                        arg.getVararg(), arg.getLineInfo()
                );
            } else {
                var argument  = new Argument(
                        arg.getName().getName(), info.getType(arg.getType()),
                        arg.getVararg(), arg.getLineInfo(), arg.getDefaultVal()
                );
                info.addDefaultArgument(argument);
                result[i] = argument;
            }
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
                if (keywordMap.containsKey(arg.getName())) {
                    throw CompilerException.format(
                            "Keyword argument %s defined twice in parameter list", arg, arg.getName()
                    );
                }
                keywordMap.put(arg.getName(), arg.getType());
            }
        }
        return keywordMap;
    }

    public String argStr() {
        var joiner = new StringJoiner(", ", "(", ")");
        if (positionArgs.length > 0) {
            for (var arg : positionArgs) {
                joiner.add(argStr(arg));
            }
            joiner.add("/");
        }
        if (normalArgs.length > 0) {
            for (var arg : normalArgs) {
                joiner.add(argStr(arg));
            }
        }
        if (keywordArgs.length > 0) {
            joiner.add("*");
            for (var arg : keywordArgs) {
                joiner.add(argStr(arg));
            }
        }
        return joiner.toString();
    }

    private String argStr(Argument arg) {
        if (arg.getName().isBlank()) {
            if (arg.isVararg()) {
                return "*" + arg.getType().name();
            } else {
                return arg.getType().name();
            }
        } else {
            if (arg.isVararg()) {
                return String.format("*%s %s", arg.getType().name(), arg.getName());
            } else {
                return String.format("%s %s", arg.getType().name(), arg.getName());
            }
        }
    }

    private int indexOf(String name) {
        for (int i = 0; i < size(); i++) {
            if (name.equals(get(i).getName())) {
                return i;
            }
        }
        return -1;
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

    @Contract(pure = true)
    @NotNull
    private Iterable<Argument> allKeywords() {
        return AllKwdIterator::new;
    }

    private final class AllKwdIterator implements Iterator<Argument> {
        int next = 0;

        @Override
        public boolean hasNext() {
            return next < normalArgs.length + keywordArgs.length;
        }

        @Override
        public Argument next() {
            if (next < normalArgs.length) {
                return normalArgs[next++];
            } else if (next < keywordArgs.length) {
                return keywordArgs[next++];
            } else {
                throw new NoSuchElementException();
            }
        }
    }

    @NotNull
    @Contract("_ -> new")
    public static ArgumentInfo of(@NotNull TypeObject... args) {
        Argument[] resultArgs = new Argument[args.length];
        for (int i = 0; i < args.length; i++) {
            resultArgs[i] = new Argument(String.format("anonymous$%d", i), args[i]);
        }
        return new ArgumentInfo(resultArgs);
    }

    private static Argument[] deList(Argument... args) {
        if (args.length == 1 && args[0].getType() instanceof ListTypeObject) {
            var argTypes = ((ListTypeObject) args[0].getType()).toArray();
            var result = new Argument[argTypes.length];
            for (int i = 0; i < result.length; i++) {
                result[i] = new Argument(String.format("%s$%d", args[0].getName(), i), argTypes[i]);
            }
            return result;
        } else {
            return args;
        }
    }

    public static final class ArgPosition {
        private final int position;
        private final Argument.DefaultValue defaultValue;

        private ArgPosition(int position) {
            this.position = position;
            this.defaultValue = null;
        }

        private ArgPosition(Argument.DefaultValue defaultValue) {
            this.position = -1;
            this.defaultValue = defaultValue;
        }

        public OptionalInt getPosition() {
            assert isValid();
            return defaultValue == null ? OptionalInt.of(position) : OptionalInt.empty();
        }

        public Optional<Argument.DefaultValue> getDefaultValue() {
            assert isValid();
            return Optional.ofNullable(defaultValue);
        }

        private boolean isValid() {
            return defaultValue == null ^ position == -1;
        }
    }
}
