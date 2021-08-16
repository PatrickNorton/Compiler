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
        switch (this) {
            case DEPRECATED:
                return Optional.of("deprecated");
            case UNUSED:
                return Optional.of("unused");
            case TRIVIAL_VALUE:
                return Optional.of("trivial");
            case UNREACHABLE:
                return Optional.of("unreachable");
            case INFINITE_LOOP:
                return Optional.of("infinite");
            case TODO:
                return Optional.of("todo");
            case ZERO_DIVISION:
                return Optional.of("zero");
            case NO_TYPE:
            default:
                return Optional.empty();
        }
    }
}
