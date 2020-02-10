package main.java.converter;

import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Builtins {
    private Builtins() {
        throw new UnsupportedOperationException("No Builtins for you!");
    }

    public static final Set<String> FORBIDDEN_NAMES = Set.of(
            "true",
            "false",
            "__default__",
            "self",
            "cls",
            "super",
            "null"
    );

    public static final TypeObject CALLABLE = new DefaultInterface("Callable", OpSpTypeNode.CALL);

    public static final StdTypeObject INT = new StdTypeObject("int");

    public static final StdTypeObject STR = new StdTypeObject("str");

    public static final StdTypeObject DECIMAL = new StdTypeObject("dec");

    public static final StdTypeObject BOOL = new StdTypeObject("bool", List.of(INT), Collections.emptyList());

    public static final StdTypeObject RANGE = new StdTypeObject("range");

    public static final TypeObject TYPE = new TypeTypeObject();

    public static final LangObject PRINT = new LangInstance(CALLABLE);

    public static final LangObject INPUT = new LangInstance(CALLABLE);

    public static final LangObject ITER = new LangInstance(CALLABLE);

    public static final LangConstant TRUE = new BoolConstant(true);

    public static final LangConstant FALSE = new BoolConstant(false);

    static {  // Set int operators
        var intOpArgInfo = new ArgumentInfo(new Argument("", INT));
        var intOperatorInfo = new FunctionInfo("", intOpArgInfo, INT);
        INT.setOperator(OpSpTypeNode.ADD, intOperatorInfo);
        INT.setOperator(OpSpTypeNode.SUBTRACT, intOperatorInfo);
        INT.setOperator(OpSpTypeNode.MULTIPLY, intOperatorInfo);
        INT.setOperator(OpSpTypeNode.DIVIDE, intOperatorInfo);
    }

    static {  // Set str operators
        var strPlusInfo = new FunctionInfo("", new ArgumentInfo(new Argument("", STR)), STR);
        STR.setOperator(OpSpTypeNode.ADD, strPlusInfo);
        var strTimesInfo = new FunctionInfo("", new ArgumentInfo(new Argument("", INT)), STR);
        STR.setOperator(OpSpTypeNode.MULTIPLY, strTimesInfo);
    }

    static {  // Set range operators
        var rangeIterInfo = new FunctionInfo("", new ArgumentInfo(), INT);
        RANGE.setOperator(OpSpTypeNode.ITER, rangeIterInfo);
        RANGE.setOperator(OpSpTypeNode.ITER_SLICE, rangeIterInfo);
    }

    public static final List<LangObject> TRUE_BUILTINS = List.of(
            PRINT,
            CALLABLE,
            INT,
            STR,
            BOOL,
            RANGE,
            TYPE,
            ITER,
            INPUT
    );

    public static final Map<String, LangObject> BUILTIN_MAP;

    static {  // Initialize BUILTIN_MAP
        Map<String, LangObject> temp = new HashMap<>();
        temp.put("print", PRINT);
        temp.put("callable", CALLABLE);
        temp.put("int", INT);
        temp.put("str", STR);
        temp.put("bool", BOOL);
        temp.put("range", RANGE);
        temp.put("type", TYPE);
        temp.put("iter", ITER);
        temp.put("input", INPUT);
        temp.put("true", TRUE);
        temp.put("false", FALSE);
        BUILTIN_MAP = temp;
    }

    @NotNull
    @Contract("_ -> new")
    public static LangConstant constantOf(String name) {
        var builtin = BUILTIN_MAP.get(name);
        var index = TRUE_BUILTINS.indexOf(builtin);
        return index == -1 ? (LangConstant) builtin : new BuiltinConstant(index);
    }
}
