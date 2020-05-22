package main.java.converter;

import main.java.parser.DescriptorNode;
import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
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

    public static final TypeObject OBJECT = new ObjectType();

    private static final TemplateParam CONTEXT_PARAM = new TemplateParam("T", 0, OBJECT);

    private static final Map<OpSpTypeNode, FunctionInfo> CONTEXT_MAP = Map.of(
            OpSpTypeNode.ENTER, new FunctionInfo(CONTEXT_PARAM),
            OpSpTypeNode.EXIT, new FunctionInfo(TypeObject.list())
    );

    public static final InterfaceType CONTEXT = new InterfaceType(
            "Context", GenericInfo.of(CONTEXT_PARAM), CONTEXT_MAP
    );

    private static final TemplateParam CALLABLE_ARGS = new TemplateParam("K", 0, true);

    private static final TemplateParam CALLABLE_RETURN = new TemplateParam("R", 1, TypeObject.list());

    private static final Map<OpSpTypeNode, FunctionInfo> CALLABLE_MAP = Map.of(
            OpSpTypeNode.CALL, new FunctionInfo(ArgumentInfo.of(CALLABLE_ARGS), CALLABLE_RETURN)
    );

    public static final InterfaceType CALLABLE = new InterfaceType(
            "Callable", GenericInfo.of(CALLABLE_ARGS, CALLABLE_RETURN), CALLABLE_MAP
    );

    private static final TemplateParam ITERABLE_PARAM = new TemplateParam("K", 0, OBJECT);

    private static final Map<OpSpTypeNode, FunctionInfo> ITERABLE_MAP = Map.of(
            OpSpTypeNode.ITER, new FunctionInfo(ITERABLE_PARAM)
    );

    public static final InterfaceType ITERABLE = new InterfaceType(
            "Iterable", GenericInfo.of(ITERABLE_PARAM), ITERABLE_MAP
    );

    public static final StdTypeObject INT = new StdTypeObject("int");

    public static final StdTypeObject STR = new StdTypeObject("str");

    public static final StdTypeObject CHAR = new StdTypeObject("char");

    public static final StdTypeObject DECIMAL = new StdTypeObject("dec");

    public static final StdTypeObject BOOL = new StdTypeObject("bool", List.of(INT));

    public static final StdTypeObject RANGE = new StdTypeObject("range");

    public static final StdTypeObject THROWS = new StdTypeObject("throws");

    public static final TypeObject TYPE = new TypeTypeObject();

    private static final FunctionInfo PRINT_INFO = new FunctionInfo("print", ArgumentInfo.of(OBJECT));

    public static final LangObject PRINT = new LangInstance(PRINT_INFO.toCallable());

    private static final FunctionInfo INPUT_INFO = new FunctionInfo("input", ArgumentInfo.of(STR), STR);

    public static final LangObject INPUT = new LangInstance(INPUT_INFO.toCallable());

    public static final LangObject ITER = new LangInstance(CALLABLE);

    private static final FunctionInfo REPR_INFO = new FunctionInfo("repr", ArgumentInfo.of(OBJECT), STR);

    public static final LangObject REPR = new LangInstance(REPR_INFO.toCallable());

    private static final TemplateParam LIST_PARAM = new TemplateParam("T", 0, OBJECT);

    public static final StdTypeObject LIST = new StdTypeObject("list", GenericInfo.of(LIST_PARAM));

    private static final TemplateParam SET_PARAM = new TemplateParam("T", 0, OBJECT);

    public static final StdTypeObject SET = new StdTypeObject("set", GenericInfo.of(SET_PARAM));

    private static final TemplateParam DICT_KEY = new TemplateParam("K", 0, OBJECT);

    private static final TemplateParam DICT_VAL = new TemplateParam("V", 1, OBJECT);

    public static final StdTypeObject DICT = new StdTypeObject("dict", GenericInfo.of(DICT_KEY, DICT_VAL));

    public static final LangConstant TRUE = new BoolConstant(true);

    public static final LangConstant FALSE = new BoolConstant(false);

    public static final LangObject OPEN = new LangInstance(CONTEXT.generify(OBJECT));  // TODO: File object

    public static final LangConstant NULL = new NullConstant();

    public static final StdTypeObject NULL_TYPE = NullConstant.TYPE;

    public static final Set<InterfaceType> DEFAULT_INTERFACES = Set.of(
            CONTEXT, CALLABLE, ITERABLE
    );

    static {  // Set int operators
        INT.isConstClass();
        var intOperatorInfo = new FunctionInfo(ArgumentInfo.of(INT), INT);
        var intCompInfo = new FunctionInfo(ArgumentInfo.of(INT), BOOL);

        var intMap = Map.ofEntries(
                Map.entry(OpSpTypeNode.ADD, intOperatorInfo),
                Map.entry(OpSpTypeNode.SUBTRACT, intOperatorInfo),
                Map.entry(OpSpTypeNode.MULTIPLY, intOperatorInfo),
                Map.entry(OpSpTypeNode.FLOOR_DIV, intOperatorInfo),
                Map.entry(OpSpTypeNode.POWER, intOperatorInfo),
                Map.entry(OpSpTypeNode.MODULO, intOperatorInfo),
                Map.entry(OpSpTypeNode.LEFT_BITSHIFT, intOperatorInfo),
                Map.entry(OpSpTypeNode.RIGHT_BITSHIFT, intOperatorInfo),
                Map.entry(OpSpTypeNode.BITWISE_AND, intOperatorInfo),
                Map.entry(OpSpTypeNode.BITWISE_OR, intOperatorInfo),
                Map.entry(OpSpTypeNode.BITWISE_XOR, intOperatorInfo),
                Map.entry(OpSpTypeNode.BITWISE_NOT, new FunctionInfo(INT)),

                Map.entry(OpSpTypeNode.EQUALS, intCompInfo),
                Map.entry(OpSpTypeNode.LESS_THAN, intCompInfo),
                Map.entry(OpSpTypeNode.LESS_EQUAL, intCompInfo),
                Map.entry(OpSpTypeNode.GREATER_THAN, intCompInfo),
                Map.entry(OpSpTypeNode.GREATER_EQUAL, intCompInfo),

                Map.entry(OpSpTypeNode.NEW, new FunctionInfo(ArgumentInfo.of(OBJECT))),
                Map.entry(OpSpTypeNode.UNARY_MINUS, new FunctionInfo(INT))
        );

        INT.setOperators(intMap);
        INT.seal();
    }

    static {  // Set str operators
        STR.isConstClass();
        var strMap = Map.of(
                OpSpTypeNode.ADD, new FunctionInfo(ArgumentInfo.of(STR), STR),
                OpSpTypeNode.MULTIPLY, new FunctionInfo(ArgumentInfo.of(INT), STR),
                OpSpTypeNode.EQUALS, new FunctionInfo(ArgumentInfo.of(STR), BOOL),
                OpSpTypeNode.GET_ATTR, new FunctionInfo(ArgumentInfo.of(INT), CHAR),
                OpSpTypeNode.ITER, new FunctionInfo(CHAR),
                OpSpTypeNode.NEW, new FunctionInfo(ArgumentInfo.of(OBJECT))
        );
        STR.setOperators(strMap);
        var joinInfo = new FunctionInfo(ArgumentInfo.of(ITERABLE.generify(OBJECT), STR));
        var upperLowerInfo = new FunctionInfo(STR);
        var strAttrs = Map.of(
                "length", new AttributeInfo(EnumSet.of(DescriptorNode.PUBLIC), INT),
                "join", new AttributeInfo(EnumSet.of(DescriptorNode.PUBLIC), joinInfo.toCallable()),
                "upper", new AttributeInfo(EnumSet.of(DescriptorNode.PUBLIC), upperLowerInfo.toCallable()),
                "lower", new AttributeInfo(EnumSet.of(DescriptorNode.PUBLIC), upperLowerInfo.toCallable())
        );
        STR.setAttributes(strAttrs);
        STR.seal();
    }

    static {  // Set char operators
        CHAR.isConstClass();
        // TODO: More char operators
        var charMap = Map.of(
                OpSpTypeNode.ADD, new FunctionInfo(ArgumentInfo.of(CHAR), CHAR),
                OpSpTypeNode.EQUALS, new FunctionInfo(ArgumentInfo.of(CHAR), BOOL)
        );
        CHAR.setOperators(charMap);
        CHAR.seal();
    }

    static {  // Set range operators
        RANGE.isConstClass();
        var rangeMap = Map.of(
                OpSpTypeNode.ITER, new FunctionInfo(INT),
                OpSpTypeNode.IN, new FunctionInfo(ArgumentInfo.of(INT), BOOL),
                OpSpTypeNode.EQUALS, new FunctionInfo(ArgumentInfo.of(RANGE), BOOL)
        );
        RANGE.setOperators(rangeMap);
        RANGE.seal();
    }

    static {  // Set list operators
        var listMap = Map.of(
                OpSpTypeNode.GET_ATTR, new FunctionInfo(ArgumentInfo.of(INT), LIST_PARAM),
                OpSpTypeNode.SET_ATTR, new FunctionInfo(ArgumentInfo.of(INT, LIST_PARAM)),
                OpSpTypeNode.DEL_ATTR, new FunctionInfo(ArgumentInfo.of(INT)),
                OpSpTypeNode.IN, new FunctionInfo(ArgumentInfo.of(LIST_PARAM), BOOL),
                OpSpTypeNode.ITER, new FunctionInfo(LIST_PARAM)
        );
        LIST.setOperators(listMap);
        var listAttrs = Map.of(
                "length", new AttributeInfo(EnumSet.of(DescriptorNode.PUBLIC), INT)
        );
        LIST.setAttributes(listAttrs);
        LIST.seal();
    }

    static {  // Set set operators
        var setCompInfo = new FunctionInfo(ArgumentInfo.of(SET), BOOL);
        var setMap = Map.of(
                OpSpTypeNode.IN, new FunctionInfo(ArgumentInfo.of(SET_PARAM)),
                OpSpTypeNode.DEL_ATTR, new FunctionInfo(ArgumentInfo.of(SET_PARAM)),
                OpSpTypeNode.EQUALS, setCompInfo,
                OpSpTypeNode.GREATER_THAN, setCompInfo,
                OpSpTypeNode.GREATER_EQUAL, setCompInfo,
                OpSpTypeNode.LESS_THAN, setCompInfo,
                OpSpTypeNode.LESS_EQUAL, setCompInfo,
                OpSpTypeNode.ITER, new FunctionInfo(SET_PARAM)
        );
        SET.setOperators(setMap);
        var setAttrs = Map.of(
                "length", new AttributeInfo(EnumSet.of(DescriptorNode.PUBLIC), INT)
        );
        SET.setAttributes(setAttrs);
        SET.seal();
    }

    static {
        var dictContainsInfo = new FunctionInfo(ArgumentInfo.of(DICT_KEY), BOOL);
        var dictGetInfo = new FunctionInfo(ArgumentInfo.of(DICT_KEY), DICT_VAL);
        var dictDelInfo = new FunctionInfo(ArgumentInfo.of(DICT_KEY));
        var dictIterInfo = new FunctionInfo(DICT_KEY, DICT_VAL);
        var dictEqInfo = new FunctionInfo(ArgumentInfo.of(DICT), BOOL);

        var dictMap = Map.of(
                OpSpTypeNode.IN, dictContainsInfo,
                OpSpTypeNode.GET_ATTR, dictGetInfo,
                OpSpTypeNode.DEL_ATTR, dictDelInfo,
                OpSpTypeNode.ITER, dictIterInfo,
                OpSpTypeNode.EQUALS, dictEqInfo
        );
        DICT.setOperators(dictMap);
        var dictAttrs = Map.of(
                "length", new AttributeInfo(EnumSet.of(DescriptorNode.PUBLIC), INT)
        );
        DICT.setAttributes(dictAttrs);
        DICT.seal();
    }

    static {  // null is const
        NULL_TYPE.isConstClass();
        NULL_TYPE.seal();
    }

    static {  // seal everything else
        DECIMAL.seal();
        BOOL.seal();
        THROWS.seal();
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
            REPR,
            INPUT,
            LIST,
            SET,
            CHAR,
            OPEN
    );

    public static final Map<String, LangObject> BUILTIN_MAP = Map.ofEntries(
            Map.entry("print", PRINT),
            Map.entry("callable", CALLABLE),
            Map.entry("int", INT),
            Map.entry("str", STR),
            Map.entry("char", CHAR),
            Map.entry("bool", BOOL),
            Map.entry("range", RANGE),
            Map.entry("type", TYPE),
            Map.entry("iter", ITER),
            Map.entry("input", INPUT),
            Map.entry("true", TRUE),
            Map.entry("false", FALSE),
            Map.entry("list", LIST),
            Map.entry("set", SET),
            Map.entry("open", OPEN),
            Map.entry("Callable", CALLABLE),
            Map.entry("Iterable", ITERABLE),
            Map.entry("repr", REPR),
            Map.entry("object", OBJECT),
            Map.entry("null", NULL)
    );

    @NotNull
    @Contract("_ -> new")
    public static LangConstant constantOf(String name) {
        var builtin = BUILTIN_MAP.get(name);
        var index = TRUE_BUILTINS.indexOf(builtin);
        return index == -1 ? (LangConstant) builtin : new BuiltinConstant(index);
    }
}
