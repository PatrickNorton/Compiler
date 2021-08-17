package main.java.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public final class CLArgs {
    private final Path target;
    private final boolean isTest;
    private final boolean isDebug;
    private final int optLevel;
    private final Set<String> cfgOptions;

    private CLArgs(Path target, boolean test, boolean isDebug, int optLevel, Set<String> cfgOptions) {
        this.target = target;
        this.isTest = test;
        this.isDebug = isDebug;
        this.optLevel = optLevel;
        this.cfgOptions = cfgOptions;
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

    @Contract("_ -> new")
    @NotNull
    public static CLArgs parse(@NotNull String[] args) {
        var file = Paths.get(args[0]);
        var test = false;
        var debug = true;
        var optLevel = 0;
        Set<String> cfgOptions = new HashSet<>();
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
                default:
                    var errorMsg = String.format("Illegal argument %s", arg);
                    throw new IllegalArgumentException(errorMsg);
            }
        }
        return new CLArgs(file, test, debug, optLevel, cfgOptions);
    }
}
