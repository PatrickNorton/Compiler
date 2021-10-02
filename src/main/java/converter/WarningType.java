package main.java.converter;

import java.util.Optional;

public enum WarningType {
    NO_TYPE,
    DEPRECATED,
    UNUSED,
    TRIVIAL_VALUE,
    UNREACHABLE,
    INFINITE_LOOP,
    ZERO_DIVISION,
    TODO,
    ;

    public Optional<String> annotationName() {
        return switch (this) {
            case DEPRECATED -> Optional.of("deprecated");
            case UNUSED -> Optional.of("unused");
            case TRIVIAL_VALUE -> Optional.of("trivial");
            case UNREACHABLE -> Optional.of("unreachable");
            case INFINITE_LOOP -> Optional.of("infinite");
            case TODO -> Optional.of("todo");
            case ZERO_DIVISION -> Optional.of("zero");
            case NO_TYPE -> Optional.empty();
        };
    }
}
