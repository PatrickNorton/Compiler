package main.java.converter;

import java.util.Optional;

public interface ConstantConverter extends TestConverter {
    LangConstant constant();

    @Override
    default Optional<LangConstant> constantReturn() {
        return Optional.of(constant());
    }
}
