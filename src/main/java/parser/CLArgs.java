package main.java.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CLArgs {
    private final Path target;
    private final boolean isTest;
    private final boolean isDebug;
    private final int optLevel;
    private final Map<Optimization, Boolean> explicitOpts;
    private final Set<String> cfgOptions;
    private final boolean printBytecode;
    private final Path bytecodePath;

    private CLArgs(
            Path target, boolean test, boolean isDebug, int optLevel,
            Map<Optimization, Boolean> explicitOpts, Set<String> cfgOptions,
            boolean printBytecode, Path bytecodePath
    ) {
        this.target = target;
        this.isTest = test;
        this.isDebug = isDebug;
        this.optLevel = optLevel;
        this.explicitOpts = explicitOpts;
        this.cfgOptions = cfgOptions;
        this.printBytecode = printBytecode;
        this.bytecodePath = bytecodePath;
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

    public Set<String> getCfgOptions() {
        return cfgOptions;
    }

    public boolean shouldPrintBytecode() {
        return printBytecode;
    }

    public Optional<Path> getBytecodePath() {
        return Optional.ofNullable(bytecodePath);
    }

    public boolean optIsEnabled(Optimization opt) {
        if (explicitOpts.containsKey(opt)) {
            return !explicitOpts.get(opt);
        }
        for (int i = 0; i < optLevel; i++) {
            if (Optimization.OPT_LIST.get(i).contains(opt)) {
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
        Map<Optimization, Boolean> optimizations = new HashMap<>();
        Set<String> cfgOptions = new HashSet<>();
        var printBytecode = false;
        Path bytecodePath = null;
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
                case "-S":
                    var bytePath = args[i++];
                    bytecodePath = Path.of(bytePath);
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
        return new CLArgs(file, test, debug, optLevel, optimizations, cfgOptions, printBytecode, bytecodePath);
    }

    private static void updateOptimizations(String name, @NotNull Map<Optimization, Boolean> optimizations, boolean negative) {
        var optimization = Optimization.fromStr(name);
        if (optimizations.containsKey(optimization)) {
            var errorMsg = String.format("Redefinition of optimization option %s", name);
            throw new IllegalArgumentException(errorMsg);
        } else {
            optimizations.put(optimization, negative);
        }
    }
}
