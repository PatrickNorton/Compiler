package main.java.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CLArgs {
    private final Path target;
    private final boolean isTest;
    private final boolean isDebug;
    private final int optLevel;
    private final Map<String, Boolean> explicitOpts;
    private final Set<String> cfgOptions;
    private final boolean printBytecode;

    private CLArgs(
            Path target, boolean test, boolean isDebug, int optLevel,
            Map<String, Boolean> explicitOpts, Set<String> cfgOptions,
            boolean printBytecode
    ) {
        this.target = target;
        this.isTest = test;
        this.isDebug = isDebug;
        this.optLevel = optLevel;
        this.explicitOpts = explicitOpts;
        this.cfgOptions = cfgOptions;
        this.printBytecode = printBytecode;
    }

    public Path getTarget() {
        return target;
    }

    public boolean isTest() {
        return isTest;
    }

    public boolean isDebug() {
        return isDebug;
    }

    public int getOptLevel() {
        return optLevel;
    }

    public Set<String> getCfgOptions() {
        return cfgOptions;
    }

    public boolean shouldPrintBytecode() {
        return printBytecode;
    }

    public boolean optIsEnabled(String opt) {
        assert ALL_OPTIMIZATIONS.contains(opt);
        if (explicitOpts.containsKey(opt)) {
            return !explicitOpts.get(opt);
        }
        for (int i = 0; i < optLevel; i++) {
            if (OPT_LIST.get(i).contains(opt)) {
                return true;
            }
        }
        return false;
    }

    @Contract("_ -> new")
    @NotNull
    public static CLArgs parse(@NotNull String[] args) {
        var file = Paths.get(args[0]);
        var test = false;
        var debug = true;
        var optLevel = 0;
        Map<String, Boolean> optimizations = new HashMap<>();
        Set<String> cfgOptions = new HashSet<>();
        var printBytecode = false;
        for (int i = 1; i < args.length; i++) {
            var arg = args[i];
            switch (arg) {
                case "--test":
                case "-t":
                    test = true;
                    break;
                case "--ndebug":
                    debug = false;
                    break;
                case "-O0":
                    optLevel = 0;
                    break;
                case "-O1":
                    optLevel = 1;
                    break;
                case "-O2":
                    optLevel = 2;
                    break;
                case "-O3":
                    optLevel = 3;
                    break;
                case "--cfg":
                    var cfgVal = args[i++];
                    cfgOptions.add(cfgVal);
                    break;
                case "-V":
                case "--version":
                    System.out.println("Version: 0.0.1");
                    break;
                case "--print-bytecode":
                    printBytecode = true;
                    break;
                default:
                    if (arg.startsWith("-f")) {
                        updateOptimizations(arg.substring(2), optimizations, false);
                    } else if (arg.startsWith("-F")) {
                        updateOptimizations(arg.substring(2), optimizations, true);
                    } else {
                        var errorMsg = String.format("Illegal argument %s", arg);
                        throw new IllegalArgumentException(errorMsg);
                    }
                    break;
            }
        }
        return new CLArgs(file, test, debug, optLevel, optimizations, cfgOptions, printBytecode);
    }

    private static void updateOptimizations(String name, @NotNull Map<String, Boolean> optimizations, boolean negative) {
        if (optimizations.containsKey(name)) {
            var errorMsg = String.format("Redefinition of optimization option %s", name);
            throw new IllegalArgumentException(errorMsg);
        } else if (!ALL_OPTIMIZATIONS.contains(name)) {
            var errorMsg = String.format("Unknown optimization option %s", name);
            throw new IllegalArgumentException(errorMsg);
        } else {
            optimizations.put(name, negative);
        }
    }

    private static final Set<String> ALL_OPTIMIZATIONS = Set.of(
            "const-bytes-object",
            "dce",      // Dead code elimination
            "dse",      // Dead store elimination
            "gcse",     // Common subexpression elimination
            "inline-functions",
            "inline-functions-called-once",
            "inline-small-functions",
            "pure-const"
    );

    private static final Set<String> O0_OPTIMIZATIONS = Set.of();

    private static final Set<String> O1_OPTIMIZATIONS = Set.of(
            "const-bytes-object",
            "dce",
            "dse",
            "inline-functions-called-once",
            "pure-const"
    );

    private static final Set<String> O2_OPTIMIZATIONS = Set.of(
            "gcse",
            "inline-functions",
            "inline-small-functions"
    );

    private static final Set<String> O3_OPTIMIZATIONS = Set.of();

    private static final List<Set<String>> OPT_LIST = List.of(
            O0_OPTIMIZATIONS,
            O1_OPTIMIZATIONS,
            O2_OPTIMIZATIONS,
            O3_OPTIMIZATIONS
    );
}
