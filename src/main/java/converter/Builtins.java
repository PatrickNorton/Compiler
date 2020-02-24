package main.java.converter;

import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

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

    public static final StdTypeObject BOOL = new StdTypeObject("bool", List.of(INT));

    public static final StdTypeObject RANGE = new StdTypeObject("range");

    public static final TypeObject OBJECT = new ObjectType();

    public static final TypeObject TYPE = new TypeTypeObject();

    public static final LangObject PRINT = new LangInstance(CALLABLE);

    public static final LangObject INPUT = new LangInstance(CALLABLE);

    public static final LangObject ITER = new LangInstance(CALLABLE);

    private static final TemplateParam LIST_PARAM = new TemplateParam("T", 0, OBJECT);

    public static final StdTypeObject LIST = new StdTypeObject("list", GenericInfo.of(LIST_PARAM));

    private static final TemplateParam SET_PARAM = new TemplateParam("T", 0, OBJECT);

    public static final StdTypeObject SET = new StdTypeObject("list", GenericInfo.of(SET_PARAM));

    private static final TemplateParam DICT_KEY = new TemplateParam("K", 0, OBJECT);

    private static final TemplateParam DICT_VAL = new TemplateParam("V", 1, OBJECT);

    public static final StdTypeObject DICT = new StdTypeObject("dict", GenericInfo.of(DICT_KEY, DICT_VAL));

    public static final LangConstant TRUE = new BoolConstant(true);

    public static final LangConstant FALSE = new BoolConstant(false);

    static {  // Set int operators
        var intOpArgInfo = new ArgumentInfo(new Argument("", INT));
        var intOperatorInfo = new FunctionInfo("", intOpArgInfo, INT);
        var intUnaryInfo = new FunctionInfo("", new ArgumentInfo(), INT);
        INT.setOperator(OpSpTypeNode.ADD, intOperatorInfo);
        INT.setOperator(OpSpTypeNode.SUBTRACT, intOperatorInfo);
        INT.setOperator(OpSpTypeNode.MULTIPLY, intOperatorInfo);
        INT.setOperator(OpSpTypeNode.DIVIDE, intOperatorInfo);
        INT.setOperator(OpSpTypeNode.POWER, intOperatorInfo);
        INT.setOperator(OpSpTypeNode.MODULO, intOperatorInfo);
        INT.setOperator(OpSpTypeNode.LEFT_BITSHIFT, intOperatorInfo);
        INT.setOperator(OpSpTypeNode.RIGHT_BITSHIFT, intOperatorInfo);
        INT.setOperator(OpSpTypeNode.BITWISE_AND, intOperatorInfo);
        INT.setOperator(OpSpTypeNode.BITWISE_OR, intOperatorInfo);
        INT.setOperator(OpSpTypeNode.BITWISE_XOR, intOperatorInfo);
        INT.setOperator(OpSpTypeNode.BITWISE_NOT, intUnaryInfo);

        var intCompInfo = new FunctionInfo("", ArgumentInfo.of(INT), BOOL);
        INT.setOperator(OpSpTypeNode.EQUALS, intCompInfo);
        INT.setOperator(OpSpTypeNode.LESS_THAN, intCompInfo);
        INT.setOperator(OpSpTypeNode.LESS_EQUAL, intCompInfo);
        INT.setOperator(OpSpTypeNode.GREATER_THAN, intCompInfo);
        INT.setOperator(OpSpTypeNode.GREATER_EQUAL, intCompInfo);
    }

    static {  // Set str operators
        var strPlusInfo = new FunctionInfo("", new ArgumentInfo(new Argument("", STR)), STR);
        STR.setOperator(OpSpTypeNode.ADD, strPlusInfo);
        var strTimesInfo = new FunctionInfo("", new ArgumentInfo(new Argument("", INT)), STR);
        STR.setOperator(OpSpTypeNode.MULTIPLY, strTimesInfo);
        var strEqInfo = new FunctionInfo("", ArgumentInfo.of(STR), BOOL);
        STR.setOperator(OpSpTypeNode.EQUALS, strEqInfo);
    }

    static {  // Set range operators
        var rangeIterInfo = new FunctionInfo("", new ArgumentInfo(), INT);
        RANGE.setOperator(OpSpTypeNode.ITER, rangeIterInfo);
        RANGE.setOperator(OpSpTypeNode.ITER_SLICE, rangeIterInfo);
        var rangeContainsInfo = new FunctionInfo("", ArgumentInfo.of(INT), BOOL);
        RANGE.setOperator(OpSpTypeNode.IN, rangeContainsInfo);
    }

    static {  // Set list operators
        var listIndexInfo = new FunctionInfo("", ArgumentInfo.of(INT), LIST_PARAM);
        LIST.setOperator(OpSpTypeNode.GET_ATTR, listIndexInfo);
        var listSetInfo = new FunctionInfo("", ArgumentInfo.of(INT, LIST_PARAM));
        LIST.setOperator(OpSpTypeNode.SET_ATTR, listSetInfo);
        var listDelInfo = new FunctionInfo("", ArgumentInfo.of(INT));
        LIST.setOperator(OpSpTypeNode.DEL_ATTR, listDelInfo);
        var listContainsInfo = new FunctionInfo("", ArgumentInfo.of(LIST_PARAM), BOOL);
        LIST.setOperator(OpSpTypeNode.IN, listContainsInfo);
    }

    static {  // Set set operators
        var setContainsInfo = new FunctionInfo("", ArgumentInfo.of(SET_PARAM), BOOL);
        SET.setOperator(OpSpTypeNode.IN, setContainsInfo);
        var setDelInfo = new FunctionInfo("", ArgumentInfo.of(SET_PARAM));
        SET.setOperator(OpSpTypeNode.DEL_ATTR, setDelInfo);
        var setCompInfo = new FunctionInfo("", ArgumentInfo.of(SET), BOOL);
        SET.setOperator(OpSpTypeNode.EQUALS, setCompInfo);
        SET.setOperator(OpSpTypeNode.GREATER_THAN, setCompInfo);
        SET.setOperator(OpSpTypeNode.GREATER_EQUAL, setCompInfo);
        SET.setOperator(OpSpTypeNode.LESS_THAN, setCompInfo);
        SET.setOperator(OpSpTypeNode.LESS_EQUAL, setCompInfo);
    }

    static {
        var dictContainsInfo = new FunctionInfo("", ArgumentInfo.of(DICT_KEY), BOOL);
        DICT.setOperator(OpSpTypeNode.IN, dictContainsInfo);
        var dictGetInfo = new FunctionInfo("", ArgumentInfo.of(DICT_KEY), DICT_VAL);
        DICT.setOperator(OpSpTypeNode.GET_ATTR, dictGetInfo);
        var dictDelInfo = new FunctionInfo("", ArgumentInfo.of(DICT_KEY));
        DICT.setOperator(OpSpTypeNode.DEL_ATTR, dictDelInfo);
        var dictIterInfo = new FunctionInfo("", ArgumentInfo.of(), DICT_KEY, DICT_VAL);
        DICT.setOperator(OpSpTypeNode.ITER, dictIterInfo);
        var dictEqInfo = new FunctionInfo("", ArgumentInfo.of(DICT), BOOL);
        DICT.setOperator(OpSpTypeNode.EQUALS, dictEqInfo);
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
            INPUT,
            LIST,
            SET
    );

    public static final Map<String, LangObject> BUILTIN_MAP = Map.ofEntries(
            Map.entry("print", PRINT),
            Map.entry("callable", CALLABLE),
            Map.entry("int", INT),
            Map.entry("str", STR),
            Map.entry("bool", BOOL),
            Map.entry("range", RANGE),
            Map.entry("type", TYPE),
            Map.entry("iter", ITER),
            Map.entry("input", INPUT),
            Map.entry("true", TRUE),
            Map.entry("false", FALSE),
            Map.entry("list", LIST),
            Map.entry("set", SET)
    );

    @NotNull
    @Contract("_ -> new")
    public static LangConstant constantOf(String name) {
        var builtin = BUILTIN_MAP.get(name);
        var index = TRUE_BUILTINS.indexOf(builtin);
        return index == -1 ? (LangConstant) builtin : new BuiltinConstant(index);
    }
}
