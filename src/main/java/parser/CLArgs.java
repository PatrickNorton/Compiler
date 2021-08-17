package main.java.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class CLArgs {
    private final Path target;
    private final boolean isTest;
    private final boolean isDebug;

    private CLArgs(Path target, boolean test, boolean isDebug) {
        this.target = target;
        this.isTest = test;
        this.isDebug = isDebug;
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

    @Contract("_ -> new")
    @NotNull
    public static CLArgs parse(@NotNull String[] args) {
        var file = Paths.get(args[0]);
        var test = false;
        var debug = true;
        for (int i = 1; i < args.length; i++) {
            var arg = args[i];
            switch (arg) {
                case "--test":
                case "-t":
                    test = true;
                    break;
                case "-o":
                    debug = false;
                    break;
                default:
                    var errorMsg = String.format("Illegal argument %s", arg);
                    throw new IllegalArgumentException(errorMsg);
            }
        }
        return new CLArgs(file, test, debug);
    }
}
