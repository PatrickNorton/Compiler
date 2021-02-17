package main.java.converter;

import main.java.util.BidirectionalMap;

import java.util.Map;

public final class Syscalls {
    private Syscalls() {}

    private static final BidirectionalMap<String, Integer> VALUES = BidirectionalMap.ofEntries(
            Map.entry("getcwd", 79),
            Map.entry("chdir", 80),
            Map.entry("mkdir", 83)
    );

    public static int get(String name) {
        return VALUES.get(stripTrailingUnderscores(name));
    }

    private static String stripTrailingUnderscores(String value) {
        if (!value.endsWith("_")) {
            return value;
        } else {
            for (int i = value.length() - 1; i >= 0; i--) {
                if (value.charAt(i) != '_') {
                    return value.substring(0, i + 1);
                }
            }
            return "";
        }
    }
}
