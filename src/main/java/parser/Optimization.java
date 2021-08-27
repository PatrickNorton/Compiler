package main.java.parser;

import main.java.converter.CompilerInternalError;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public enum Optimization {
    CONST_BYTES("const-bytes-object"),
    DEAD_CODE("dce"),
    DEAD_STORE("dse"),
    COMMON_SUBEXPR("gcse"),
    INLINE_FUNCTIONS("inline-functions"),
    INLINE_FN_ONCE("inline-functions-called-once"),
    INLINE_SMALL_FN("inline-small-functions"),
    PURE_CONST("pure-const"),
    ;

    public final String stringValue;

    private static final Set<Optimization> O0_OPTIMIZATIONS =  Collections.unmodifiableSet(EnumSet.noneOf(
            Optimization.class
    ));

    private static final Set<Optimization> O1_OPTIMIZATIONS =  Collections.unmodifiableSet(EnumSet.of(
            CONST_BYTES,
            DEAD_CODE,
            DEAD_STORE,
            INLINE_FN_ONCE,
            PURE_CONST
    ));

    private static final Set<Optimization> O2_OPTIMIZATIONS = Collections.unmodifiableSet(EnumSet.of(
            COMMON_SUBEXPR,
            INLINE_FUNCTIONS,
            INLINE_SMALL_FN
    ));

    private static final Set<Optimization> O3_OPTIMIZATIONS = Set.of();

    public static final List<Set<Optimization>> OPT_LIST = List.of(
            O0_OPTIMIZATIONS,
            O1_OPTIMIZATIONS,
            O2_OPTIMIZATIONS,
            O3_OPTIMIZATIONS
    );

    Optimization(String stringValue) {
        this.stringValue = stringValue;
    }

    private static final Map<String, Optimization> STR_CACHE = new HashMap<>(values().length);

    @NotNull
    public static Optimization fromStr(String value) {
        return STR_CACHE.computeIfAbsent(value, x -> {
            for (var val : values()) {
                if (val.stringValue.equals(x)) {
                    return val;
                }
            }
            throw CompilerInternalError.format("Unknown optimization option %s", LineInfo.empty(), value);
        });
    }
}
