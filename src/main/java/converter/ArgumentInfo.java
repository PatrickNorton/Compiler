package main.java.converter;

import main.java.parser.LineInfo;
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
     * @param fnInfo The parent type, for generics
     * @param args The arguments to check for validity
     * @return If they match this function signature
     */
    public boolean matches(FunctionInfo fnInfo, @NotNull Argument... args) {
        try {
            generifyArgs(fnInfo, args);
            return true;
        } catch (CompilerException ignored) {
            return false;
        }
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

    private int argsWithDefaults(Set<String> toExclude) {
        var count = 0;
        for (var arg : this) {
            if (!toExclude.contains(arg.getName()) && arg.getDefaultValue().isPresent()) {
                count++;
            }
        }
        return count;
    }

    private int kwArgsWithDefaults(Set<String> toExclude) {
        var count = 0;
        for (var arg : keywordArgs) {
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

    private int nextEligibleParam(int nextUnused, Argument... params) {
        for (; nextUnused < params.length; nextUnused++) {
            if (params[nextUnused].getName().isEmpty()) {
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
    public ArgPosition[] argPositions(Argument... args) {
        assert varargIsValid();
        if (hasVararg()) {
            return argPositionsWithVararg(args);
        } else {
            return argPositionsNoVararg(args);
        }
    }

    public ArgPosition[] argPositionsNoVararg(Argument... args) {
        var newArgs = expandTuples(args);
        var kwPositions = getKwPositions(args);
        var defaultCount = argsWithDefaults(kwPositions.keySet());
        var nonKeywordCount = newArgs.length - kwPositions.size();
        var unused = size() - kwPositions.size();
        var defaultsUsed = unused - nonKeywordCount;
        var defaultsUnused = defaultCount - defaultsUsed;
        var result = new ArgPosition[size()];
        for (int i = 0, argNo = 0; i < size(); i++) {
            var valueArg = this.get(i);
            if (kwPositions.containsKey(valueArg.getName())) {
                result[i] = new StandardArgPos(kwPositions.get(valueArg.getName()));
            }  else if (valueArg.getDefaultValue().isEmpty()) {
                result[i] = new StandardArgPos(argNo++);
            } else if (defaultsUnused > 0) {
                result[i] = new StandardArgPos(argNo++);
                defaultsUnused -= 1;
            } else {
                var defaultVal = valueArg.getDefaultValue().orElseThrow();
                result[i] = new DefaultArgPos(defaultVal);
            }
        }
        return result;
    }

    public ArgPosition[] argPositionsWithVararg(Argument... args) {
        var newArgs = expandTuples(args);
        var kwPositions = getKwPositions(args);
        assert varargIsValid() && hasVararg();
        // The total number of arguments in the declaration with a possible
        // default parameter, excluding those which are explicitly passed as a
        // kwarg
        var defaultCount = argsWithDefaults(kwPositions.keySet());
        // The total number of keyword-only arguments in the declaration which
        // will be using their default parameter
        var defaultedKwargs = kwArgsWithDefaults(kwPositions.keySet());
        assert defaultedKwargs <= defaultCount;
        // The total number of positionally-passable arguments in the
        // declaration which do not have a matched value yet
        var unused = size() - kwPositions.size() - defaultedKwargs;
        // The total number of non-keyword arguments in the invocation
        var nonKeywordCount = newArgs.length - kwPositions.size();
        // The number of positional arguments which will be given a default
        // parameter
        var defaultsUsed = Math.max(unused - nonKeywordCount - 1, 0);
        // The number of default arguments which will take a positional
        // parameter in the argument list
        var defaultsUnused = defaultCount - defaultsUsed - defaultedKwargs;
        // The size of the variadic argument
        var varargCount = Math.max(nonKeywordCount - unused + 1, 0);
        var result = new ArgPosition[size()];
        for (int i = 0, argNo = 0; i < size(); i++) {
            var valueArg = this.get(i);
            if (kwPositions.containsKey(valueArg.getName())) {
                result[i] = new StandardArgPos(kwPositions.get(valueArg.getName()));
            } else if (valueArg.isVararg()) {
                List<Integer> values = new ArrayList<>(varargCount);
                for (int j = 0; j < varargCount; j++) {
                    values.add(argNo++);
                    argNo = nextEligibleParam(argNo, args);
                }
                result[i] = new VarargPos(values, valueArg.getType());
            } else if (valueArg.getDefaultValue().isEmpty()) {
                result[i] = new StandardArgPos(argNo++);
                argNo = nextEligibleParam(argNo, args);
            } else if (defaultsUnused > 0) {
                result[i] = new StandardArgPos(argNo++);
                defaultsUnused--;
                argNo = nextEligibleParam(argNo, args);
            } else {
                var defaultVal = valueArg.getDefaultValue().orElseThrow();
                result[i] = new DefaultArgPos(defaultVal);
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
     * <p>
     *     Note that this will throw an exception if the parameters do not
     *     match.
     * </p>
     *
     * @param parent The parent info to generify with (should be the case that
     *               {@code parent.getArgs() == this}
     * @param args The arguments passed to the function
     * @return The values
     * @see FunctionInfo#generifyArgs
     */
    @NotNull
    public Pair<Map<Integer, TypeObject>, Set<Integer>> generifyArgs(
            @NotNull FunctionInfo parent, Argument... args
    ) {
        assert parent.getArgs() == this;
        var par = parent.toCallable();
        var newArgs = expandTuples(args);
        var keywordMap = initKeywords(newArgs);
        Map<Integer, TypeObject> result = new HashMap<>();
        Set<Integer> needsMakeOption = new HashSet<>();
        // Check if all given keyword arguments are subclasses of the declaration
        var matchedCount = 0;
        for (var arg : allKeywords()) {
            var name = arg.getName();
            if (keywordMap.containsKey(name)) {
                var keywordType = keywordMap.get(name);
                if (update(indexOf(name), par, result, needsMakeOption, arg, keywordType)) {
                    matchedCount++;
                } else {
                    throw CompilerException.format(
                            "Argument mismatch: Keyword argument %s is of type '%s'," +
                                    " which is not assignable to type '%s'",
                            findName(name, newArgs), arg.getName(), keywordType, arg.getType().name()
                    );
                }
            }
        }
        // Ensure the number of matched keywords is the same as the total number of keywords
        // If it's not, then there are keyword arguments passed that don't have an equivalent
        // in the function definition
        if (matchedCount != keywordMap.size()) {
            for (var keyword : keywordMap.keySet()) {
                if (indexOf(keyword) == -1) {
                    var keywordArg = findName(keyword, newArgs);
                    throw CompilerException.format("Keyword argument %s is unexpected", keywordArg, keyword);
                }
            }
        }
        // Check that all keyword-only arguments are either used or have a default
        for (var arg : keywordArgs) {
            if (!keywordMap.containsKey(arg.getName()) && arg.getDefaultValue().isEmpty()) {
                // FIXME: Get line info for all missing-argument scenarios
                var lineInfo = newArgs.length > 0 ? newArgs[0].getLineInfo() : LineInfo.empty();
                throw CompilerException.format(
                        "Missing value for required keyword-only argument %s", lineInfo, arg.getName()
                );
            }
        }
        assert varargIsValid();
        var defaultCount = argsWithDefaults(keywordMap.keySet());
        var nonKeywordCount = newArgs.length - keywordMap.size();
        var unused = size() - keywordMap.size();
        var defaultsUsed = unused - nonKeywordCount;
        if (defaultsUsed < 0) {
            if (!hasVararg()) {
                var countStr = defaultCount == 0 ? "exactly" : "no more than";
                throw CompilerException.format(
                        "Too many parameters passed: Expected %s %d unnamed parameters, got %d",
                        args[0], countStr, unused, nonKeywordCount
                );
            }
        } else if (defaultsUsed > defaultCount) {
            if (!hasVararg() || defaultsUsed != defaultCount + 1) {
                throw notEnoughArgs(newArgs, keywordMap, defaultCount, defaultsUsed);
            }
        }
        var defaultsUnused = defaultCount - defaultsUsed;
        var nextArg = nextEligibleArg(0, keywordMap.keySet(), defaultsUnused > 0);
        for (var arg : newArgs) {
            if (keywordMap.containsKey(arg.getName())) {
                continue;
            } else if (nextArg >= size()) {
                throw CompilerInternalError.of(
                        "Error in parameter expansion: nextEligibleArg() should never return >= size()\n" +
                        "Note: All cases where this branch is taken should be spotted earlier in the function", arg
                );
            }
            var argValue = get(nextArg);
            if (!update(nextArg, par, result, needsMakeOption, argValue, arg.getType())) {
                throw CompilerException.format(
                        "Argument mismatch: Argument is of type '%s', which is not assignable to type '%s'",
                        arg, arg.getType().name(), argValue.getType().name()
                );
            }
            if (argValue.getDefaultValue().isPresent()) {
                defaultsUnused -= 1;
            }
            // Because the variadic argument is always the last non-keyword one,
            // we don't update it once we reach it, since any other arguments
            // we get from here on out are going to be part of this one.
            if (!argValue.isVararg()) {
                nextArg = nextEligibleArg(nextArg + 1, keywordMap.keySet(), defaultsUnused > 0);
            }
        }
        return Pair.of(result, needsMakeOption);
    }

    private static boolean update(
            int i, TypeObject par, Map<Integer, TypeObject> result,
            Set<Integer> needsMakeOption, @NotNull Argument arg, TypeObject passedType
    ) {
        var argGenerics = arg.getType().generifyAs(par, passedType);
        if (argGenerics.isEmpty()) {
            if (OptionTypeObject.needsAndSuper(arg.getType(), passedType)) {
                needsMakeOption.add(i);
                return true;
            }
            var optionGenerics = arg.getType().generifyAs(par, TypeObject.optional(passedType));
            if (optionGenerics.isEmpty() || !TypeObject.addGenericsToMap(optionGenerics.orElseThrow(), result)) {
                return false;
            } else {
                needsMakeOption.add(i);
                return true;
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

    @NotNull
    private CompilerException notEnoughArgs(
            Argument[] newArgs, Map<String, TypeObject> keywordMap, int defaultCount, int defaultsUsed
    ) {
        var remaining = defaultsUsed - defaultCount;
        var unmatched = new String[remaining];
        remaining--;
        for (int i = size() - 1; i >= 0; i--) {
            var name = get(i).getName();
            if (!keywordMap.containsKey(name)) {
                unmatched[remaining] = name;
                remaining--;
            }
        }
        var argLineInfo = newArgs.length > 0 ? newArgs[0].getLineInfo() : LineInfo.empty();
        if (unmatched.length == 1) {
            return CompilerException.format(
                    "Missing value for positional argument %s", argLineInfo, unmatched[0]
            );
        } else {
            var joinedNames = String.join(", ", unmatched);
            return CompilerException.format("Missing value for positional arguments %s", argLineInfo, joinedNames);
        }
    }

    private boolean varargIsValid() {
        var normal = normalArgs.length == 0 ? null : normalArgs[normalArgs.length - 1];
        for (var arg : this) {
            if (arg != normal && arg.isVararg()) {
                return false;
            }
        }
        return true;
    }

    private boolean hasVararg() {
        assert varargIsValid();
        return normalArgs.length > 0 && normalArgs[normalArgs.length - 1].isVararg();
    }

    private static Map<String, Integer> getKwPositions(Argument... args) {
        Map<String, Integer> result = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            var arg = args[i];
            if (!arg.getName().isEmpty()) {
                result.put(arg.getName(), i);
            }
        }
        return result;
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
            } else if (next < normalArgs.length + keywordArgs.length) {
                return keywordArgs[next++ - normalArgs.length];
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

    private static Argument findName(String name, Argument... args) {
        for (var arg : args) {
            if (arg.getName().equals(name)) {
                return arg;
            }
        }
        throw CompilerInternalError.format("Unknown name %s", LineInfo.empty(), name);
    }
}
