package main.java.converter;

import main.java.parser.LineInfo;
import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private static final Map<OpSpTypeNode, MethodInfo> CONTEXT_MAP = Map.of(
            OpSpTypeNode.ENTER, MethodInfo.of(CONTEXT_PARAM),
            OpSpTypeNode.EXIT, MethodInfo.of(TypeObject.list())
    );

    public static final InterfaceType CONTEXT = new InterfaceType(
            "Context", GenericInfo.of(CONTEXT_PARAM), CONTEXT_MAP
    );

    private static final TemplateParam CALLABLE_ARGS = new TemplateParam("K", 0, true);

    private static final TemplateParam CALLABLE_RETURN = new TemplateParam("R", 1, TypeObject.list());

    private static final Map<OpSpTypeNode, MethodInfo> CALLABLE_MAP = Map.of(
            OpSpTypeNode.CALL, MethodInfo.of(ArgumentInfo.of(CALLABLE_ARGS), CALLABLE_RETURN)
    );

    public static final InterfaceType CALLABLE = new InterfaceType(
            "Callable", GenericInfo.of(CALLABLE_ARGS, CALLABLE_RETURN), CALLABLE_MAP
    );

    private static final TemplateParam ITERABLE_PARAM = new TemplateParam("K", 0, true);

    public static final InterfaceType ITERABLE = new InterfaceType("Iterable", GenericInfo.of(ITERABLE_PARAM));

    public static final StdTypeObject INT = new StdTypeObject("int");

    public static final StdTypeObject STR = new StdTypeObject("str");

    public static final StdTypeObject CHAR = new StdTypeObject("char");

    public static final StdTypeObject DECIMAL = new StdTypeObject("dec");

    public static final StdTypeObject BOOL = new StdTypeObject("bool", List.of(INT));

    public static final StdTypeObject RANGE = new StdTypeObject("range");

    public static final StdTypeObject BYTES = new StdTypeObject("bytes");

    public static final StdTypeObject THROWS = new StdTypeObject("throws");

    public static final StdTypeObject SLICE = new StdTypeObject("slice");

    public static final TypeObject TYPE = new TypeTypeObject();

    private static final FunctionInfo PRINT_INFO = new FunctionInfo("print", ArgumentInfo.of(OBJECT));

    public static final LangObject PRINT = new LangInstance(PRINT_INFO.toCallable());

    private static final FunctionInfo INPUT_INFO = new FunctionInfo("input", ArgumentInfo.of(STR), STR);

    public static final LangObject INPUT = new LangInstance(INPUT_INFO.toCallable());

    public static final LangObject ITER = new LangInstance(CALLABLE);

    private static final FunctionInfo REPR_INFO = new FunctionInfo("repr", ArgumentInfo.of(OBJECT), STR);

    public static final LangObject REPR = new LangInstance(REPR_INFO.toCallable());

    private static final TemplateParam REVERSED_PARAM = new TemplateParam("T", 0, Builtins.OBJECT);

    private static final FunctionInfo REVERSED_INFO = new FunctionInfo(
            "reversed", ArgumentInfo.of(REVERSED_PARAM), REVERSED_PARAM
    );

    public static final LangObject REVERSED = new LangInstance(REVERSED_INFO.toCallable());

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

    public static final FunctionInfo ID_INFO = new FunctionInfo("id", ArgumentInfo.of(Builtins.OBJECT), INT);

    public static final LangObject ID = new LangInstance(ID_INFO.toCallable());

    private static final TemplateParam ARRAY_PARAM = new TemplateParam("T", 0, OBJECT);

    public static final StdTypeObject ARRAY = new StdTypeObject("Array", GenericInfo.of(ARRAY_PARAM));

    public static final TupleType TUPLE = new TupleType();

    public static final TemplateParam ENUMERATE_PARAM = new TemplateParam("T", 0, OBJECT);

    public static final FunctionInfo ENUMERATE_INFO = new FunctionInfo(
            "enumerate", true,
            ArgumentInfo.of(Builtins.ITERABLE.generify(ENUMERATE_PARAM)),
            Builtins.ITERABLE.generify(INT, ENUMERATE_PARAM)
    );

    public static final LangObject ENUMERATE = new LangInstance(ENUMERATE_INFO.toCallable());

    public static final InterfaceType THROWABLE = new InterfaceType("Throwable", GenericInfo.empty());

    public static final StdTypeObject NOT_IMPLEMENTED = new StdTypeObject("NotImplemented", List.of(THROWABLE));

    public static final LangConstant NULL = new NullConstant();

    public static final StdTypeObject NULL_TYPE = NullConstant.TYPE;

    public static final Set<InterfaceType> DEFAULT_INTERFACES = Set.of(
            CONTEXT, CALLABLE, ITERABLE
    );

    static {  // Set int operators
        INT.isConstClass();
        var intOperatorInfo = MethodInfo.of(ArgumentInfo.of(INT), INT);
        var intCompInfo = MethodInfo.of(ArgumentInfo.of(INT), BOOL);
        var intUopInfo = MethodInfo.of(INT);

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
                Map.entry(OpSpTypeNode.BITWISE_NOT, intUopInfo),

                Map.entry(OpSpTypeNode.EQUALS, intCompInfo),
                Map.entry(OpSpTypeNode.LESS_THAN, intCompInfo),
                Map.entry(OpSpTypeNode.LESS_EQUAL, intCompInfo),
                Map.entry(OpSpTypeNode.GREATER_THAN, intCompInfo),
                Map.entry(OpSpTypeNode.GREATER_EQUAL, intCompInfo),

                Map.entry(OpSpTypeNode.NEW, MethodInfo.of(OBJECT)),
                Map.entry(OpSpTypeNode.UNARY_MINUS, intUopInfo)
        );

        INT.setOperators(intMap);
        INT.seal();
    }

    static {  // Set str operators
        STR.isConstClass();
        var strMap = Map.of(
                OpSpTypeNode.ADD, MethodInfo.of(ArgumentInfo.of(STR), STR),
                OpSpTypeNode.MULTIPLY, MethodInfo.of(ArgumentInfo.of(INT), STR),
                OpSpTypeNode.EQUALS, MethodInfo.of(ArgumentInfo.of(STR), BOOL),
                OpSpTypeNode.GET_ATTR, MethodInfo.of(ArgumentInfo.of(INT), CHAR),
                OpSpTypeNode.GET_SLICE, MethodInfo.of(ArgumentInfo.of(SLICE), STR),
                OpSpTypeNode.ITER, MethodInfo.of(Builtins.ITERABLE.generify(CHAR)),
                OpSpTypeNode.NEW, MethodInfo.of(ArgumentInfo.of(OBJECT))
        );
        STR.setOperators(strMap);
        var fromCharsInfo = new FunctionInfo(ArgumentInfo.of(LIST.generify(CHAR)), STR);
        var staticStrMap = Map.of(
                "fromChars", new AttributeInfo(AccessLevel.PUBLIC, fromCharsInfo.toCallable())
        );
        STR.setStaticAttributes(staticStrMap);
        var joinInfo = new FunctionInfo(ArgumentInfo.of(ITERABLE.generify(OBJECT)), STR);
        var startsInfo = new FunctionInfo(ArgumentInfo.of(STR), BOOL);
        var splitInfo = new FunctionInfo(ArgumentInfo.of(STR), LIST.generify(STR));
        var splitLinesInfo = new FunctionInfo(ArgumentInfo.of(), LIST.generify(STR));
        var upperLowerInfo = new FunctionInfo(STR);
        var strAttrs = Map.of(
                "length", new AttributeInfo(AccessLevel.PUBLIC, INT),
                "chars", new AttributeInfo(AccessLevel.PUBLIC, LIST.generify(CHAR)),
                "join", new AttributeInfo(AccessLevel.PUBLIC, joinInfo.toCallable()),
                "startsWith", new AttributeInfo(AccessLevel.PUBLIC, startsInfo.toCallable()),
                "endsWith", new AttributeInfo(AccessLevel.PUBLIC, startsInfo.toCallable()),
                "split", new AttributeInfo(AccessLevel.PUBLIC, splitInfo.toCallable()),
                "splitLines", new AttributeInfo(AccessLevel.PUBLIC, splitLinesInfo.toCallable()),
                "upper", new AttributeInfo(AccessLevel.PUBLIC, upperLowerInfo.toCallable()),
                "lower", new AttributeInfo(AccessLevel.PUBLIC, upperLowerInfo.toCallable())
        );
        STR.setAttributes(strAttrs);
        STR.seal();
    }

    static {  // Set bytes operators
        var bytesMap = Map.of(
                OpSpTypeNode.ADD, MethodInfo.of(ArgumentInfo.of(BYTES), BYTES),
                OpSpTypeNode.MULTIPLY, MethodInfo.of(ArgumentInfo.of(INT), BYTES),
                OpSpTypeNode.EQUALS, MethodInfo.of(ArgumentInfo.of(BYTES), BOOL),
                OpSpTypeNode.GET_ATTR, MethodInfo.of(ArgumentInfo.of(INT), INT),
                OpSpTypeNode.GET_SLICE, MethodInfo.of(ArgumentInfo.of(SLICE), BYTES),
                OpSpTypeNode.SET_ATTR, MethodInfo.of(ArgumentInfo.of(INT, INT)),
                OpSpTypeNode.ITER, MethodInfo.of(ITERABLE.generify(INT)),
                OpSpTypeNode.NEW, MethodInfo.of(ArgumentInfo.of(OBJECT))
        );
        BYTES.setOperators(bytesMap);
        var joinInfo = new FunctionInfo(ArgumentInfo.of(ITERABLE.generify(OBJECT), BYTES));
        var encodeInfo = new FunctionInfo(ArgumentInfo.of(STR), STR);
        var indexInfo = new FunctionInfo(ArgumentInfo.of(INT), TypeObject.optional(INT));
        var bytesAttrs = Map.of(
                "length", new AttributeInfo(AccessLevel.PUBLIC, INT),
                "join", new AttributeInfo(AccessLevel.PUBLIC, joinInfo.toCallable()),
                "encode", new AttributeInfo(AccessLevel.PUBLIC, encodeInfo.toCallable()),
                "indexOf", new AttributeInfo(AccessLevel.PUBLIC, indexInfo.toCallable())
        );
        BYTES.setAttributes(bytesAttrs);
        BYTES.seal();
    }

    static {  // Set char operators
        CHAR.isConstClass();
        // TODO: More char operators
        var charMap = Map.of(
                OpSpTypeNode.ADD, MethodInfo.of(ArgumentInfo.of(CHAR), CHAR),
                OpSpTypeNode.EQUALS, MethodInfo.of(ArgumentInfo.of(CHAR), BOOL)
        );
        CHAR.setOperators(charMap);
        CHAR.seal();
    }

    static {  // Set range operators
        RANGE.isConstClass();
        var rangeMap = Map.of(
                OpSpTypeNode.ITER, MethodInfo.of(ITERABLE.generify(INT)),
                OpSpTypeNode.IN, MethodInfo.of(ArgumentInfo.of(INT), BOOL),
                OpSpTypeNode.EQUALS, MethodInfo.of(ArgumentInfo.of(RANGE), BOOL)
        );
        RANGE.setOperators(rangeMap);
        RANGE.seal();
    }

    static {  // Set slice operators
        SLICE.isConstClass();
        var endInfo = new AttributeInfo(AccessLevel.PUBLIC, TypeObject.optional(INT));
        var rangeInfo = new AttributeInfo(
                AccessLevel.PUBLIC,
                new FunctionInfo(ArgumentInfo.of(INT), RANGE).toCallable()
        );
        var sliceMap = Map.of(
                "start", endInfo,
                "stop", endInfo,
                "step", endInfo,
                "toRange", rangeInfo
        );
        SLICE.setAttributes(sliceMap);
        SLICE.seal();
    }

    static {  // Set list operators
        var listMap = Map.of(
                OpSpTypeNode.GET_ATTR, MethodInfo.of(ArgumentInfo.of(INT), LIST_PARAM),
                OpSpTypeNode.SET_ATTR, MethodInfo.ofMut(ArgumentInfo.of(INT, LIST_PARAM)),
                OpSpTypeNode.DEL_ATTR, MethodInfo.ofMut(ArgumentInfo.of(INT)),
                OpSpTypeNode.GET_SLICE, MethodInfo.of(ArgumentInfo.of(SLICE), LIST.generify(LIST_PARAM)),
                OpSpTypeNode.IN, MethodInfo.of(ArgumentInfo.of(LIST_PARAM), BOOL),
                OpSpTypeNode.REVERSED, MethodInfo.of(LIST),
                OpSpTypeNode.ADD, MethodInfo.of(ArgumentInfo.of(LIST), LIST),
                OpSpTypeNode.ITER, MethodInfo.of(ITERABLE.generify(LIST_PARAM))
        );
        LIST.setOperators(listMap);
        var getInfo = new FunctionInfo(ArgumentInfo.of(INT), TypeObject.optional(LIST_PARAM));
        var insertInfo = new FunctionInfo(ArgumentInfo.of(INT, LIST_PARAM));
        var reverseInfo = new FunctionInfo();
        var countInfo = new FunctionInfo(ArgumentInfo.of(LIST_PARAM), INT);
        var clearInfo = new FunctionInfo();
        var addInfo = new FunctionInfo(ArgumentInfo.of(LIST_PARAM));
        var indexOfInfo = new FunctionInfo(ArgumentInfo.of(LIST_PARAM),TypeObject.optional(INT));
        var listAttrs = Map.of(
                "length", new AttributeInfo(AccessLevel.PUBLIC, INT),
                "get", new AttributeInfo(AccessLevel.PUBLIC, getInfo.toCallable()),
                "insert", new AttributeInfo(AccessLevel.PUBLIC, MutableType.MUT_METHOD, insertInfo.toCallable()),
                "reverse", new AttributeInfo(AccessLevel.PUBLIC, MutableType.MUT_METHOD, reverseInfo.toCallable()),
                "count", new AttributeInfo(AccessLevel.PUBLIC, countInfo.toCallable()),
                "clear", new AttributeInfo(AccessLevel.PUBLIC, MutableType.MUT_METHOD, clearInfo.toCallable()),
                "indexOf", new AttributeInfo(AccessLevel.PUBLIC, indexOfInfo.toCallable()),
                "add", new AttributeInfo(AccessLevel.PUBLIC, MutableType.MUT_METHOD, addInfo.toCallable())
        );
        LIST.setAttributes(listAttrs);
        LIST.seal();

        LIST_PARAM.setParent(LIST);
    }

    static {  // Set set operators
        var setCompInfo = MethodInfo.of(ArgumentInfo.of(SET), BOOL);
        var setMap = Map.of(
                OpSpTypeNode.IN, MethodInfo.of(ArgumentInfo.of(SET_PARAM)),
                OpSpTypeNode.DEL_ATTR, MethodInfo.of(ArgumentInfo.of(SET_PARAM)),
                OpSpTypeNode.EQUALS, setCompInfo,
                OpSpTypeNode.GREATER_THAN, setCompInfo,
                OpSpTypeNode.GREATER_EQUAL, setCompInfo,
                OpSpTypeNode.LESS_THAN, setCompInfo,
                OpSpTypeNode.LESS_EQUAL, setCompInfo,
                OpSpTypeNode.ITER, MethodInfo.of(ITERABLE.generify(SET_PARAM))
        );
        SET.setOperators(setMap);
        var setAttrs = Map.of(
                "length", new AttributeInfo(AccessLevel.PUBLIC, INT)
        );
        SET.setAttributes(setAttrs);
        SET.seal();

        SET_PARAM.setParent(SET);
    }

    static {
        var dictContainsInfo = MethodInfo.of(ArgumentInfo.of(DICT_KEY), BOOL);
        var dictGetInfo = MethodInfo.of(ArgumentInfo.of(DICT_KEY), DICT_VAL);
        var dictDelInfo = MethodInfo.of(ArgumentInfo.of(DICT_KEY));
        var dictIterInfo = MethodInfo.of(DICT_KEY, Builtins.ITERABLE.generify(DICT_VAL));
        var dictEqInfo = MethodInfo.of(ArgumentInfo.of(DICT), BOOL);

        var dictGetMInfo = new FunctionInfo(ArgumentInfo.of(DICT_KEY), TypeObject.optional(DICT_VAL));

        var dictMap = Map.of(
                OpSpTypeNode.IN, dictContainsInfo,
                OpSpTypeNode.GET_ATTR, dictGetInfo,
                OpSpTypeNode.DEL_ATTR, dictDelInfo,
                OpSpTypeNode.ITER, dictIterInfo,
                OpSpTypeNode.EQUALS, dictEqInfo
        );
        DICT.setOperators(dictMap);
        var dictAttrs = Map.of(
                "length", new AttributeInfo(AccessLevel.PUBLIC, INT),
                "get", new AttributeInfo(AccessLevel.PUBLIC, dictGetMInfo.toCallable())
        );
        DICT.setAttributes(dictAttrs);
        DICT.seal();

        DICT_KEY.setParent(DICT);
        DICT_VAL.setParent(DICT);
    }

    static {
        var arrayGetInfo = MethodInfo.of(ArgumentInfo.of(INT), ARRAY_PARAM);
        var arraySetInfo = MethodInfo.of(ArgumentInfo.of(INT, ARRAY_PARAM));
        var arrayIterInfo = MethodInfo.of(ITERABLE.generify(ARRAY_PARAM));
        var arrayContainsInfo = MethodInfo.of(ArgumentInfo.of(ARRAY_PARAM), BOOL);
        var arrayEqInfo = MethodInfo.of(ArgumentInfo.of(ARRAY), BOOL);
        var arraySliceInfo = MethodInfo.of(ArgumentInfo.of(SLICE), LIST.generify(ARRAY_PARAM));
        var arrayReversedInfo = MethodInfo.of(ARRAY);

        var arrayMap = Map.of(
                OpSpTypeNode.GET_ATTR, arrayGetInfo,
                OpSpTypeNode.SET_ATTR, arraySetInfo,
                OpSpTypeNode.ITER, arrayIterInfo,
                OpSpTypeNode.IN, arrayContainsInfo,
                OpSpTypeNode.EQUALS, arrayEqInfo,
                OpSpTypeNode.GET_SLICE, arraySliceInfo,
                OpSpTypeNode.REVERSED, arrayReversedInfo
        );
        ARRAY.setOperators(arrayMap);
        var arrayAttrs = Map.of(
                "length", new AttributeInfo(AccessLevel.PUBLIC, INT)
        );
        ARRAY.setAttributes(arrayAttrs);
        ARRAY.seal();

        ARRAY_PARAM.setParent(ARRAY);
    }

    static {
        var iterInfo = MethodInfo.of(ITERABLE.generify(LineInfo.empty(), ITERABLE_PARAM));

        ITERABLE.setOperators(Map.of(OpSpTypeNode.ITER, iterInfo), Set.of(OpSpTypeNode.ITER));
        ITERABLE.seal();
    }

    static {
        var notImplConstructor = MethodInfo.of();
        NOT_IMPLEMENTED.setOperators(Map.of(OpSpTypeNode.NEW, notImplConstructor));
        NOT_IMPLEMENTED.seal();
    }

    static {  // null is const
        NULL_TYPE.isConstClass();
        NULL_TYPE.seal();
    }

    static {  // seal everything else
        DECIMAL.isConstClass();
        DECIMAL.seal();

        BOOL.isConstClass();
        BOOL.seal();

        THROWS.isConstClass();
        THROWS.seal();
    }

    static {  // Set interface parents
        CONTEXT_PARAM.setParent(CONTEXT);

        ITERABLE_PARAM.setParent(ITERABLE);

        CALLABLE_ARGS.setParent(CALLABLE);
        CALLABLE_RETURN.setParent(CALLABLE);
    }

    static {  // Set method parents
        REVERSED_PARAM.setParent(REVERSED_INFO.toCallable());
        ENUMERATE_PARAM.setParent(ENUMERATE_INFO.toCallable());
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
            OPEN,
            REVERSED,
            SLICE,
            ID,
            ARRAY,
            ENUMERATE,
            BYTES,
            DICT,
            OBJECT,
            NOT_IMPLEMENTED,
            TUPLE
    );

    public static final Map<String, LangObject> BUILTIN_MAP = Map.ofEntries(
            Map.entry("print", PRINT),
            Map.entry("callable", CALLABLE),
            Map.entry("int", INT),
            Map.entry("str", STR),
            Map.entry("char", CHAR),
            Map.entry("bool", BOOL),
            Map.entry("range", RANGE),
            Map.entry("dict", DICT),
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
            Map.entry("Throwable", THROWABLE),
            Map.entry("repr", REPR),
            Map.entry("object", OBJECT),
            Map.entry("reversed", REVERSED),
            Map.entry("slice", SLICE),
            Map.entry("id", ID),
            Map.entry("dec", DECIMAL),
            Map.entry("tuple", TUPLE),
            Map.entry("Array", ARRAY),
            Map.entry("bytes", BYTES),
            Map.entry("enumerate", ENUMERATE),
            Map.entry("NotImplemented", NOT_IMPLEMENTED),
            Map.entry("null", NULL)
    );

    @NotNull
    @Contract("_ -> new")
    public static Optional<LangConstant> constantOf(String name) {
        var builtin = BUILTIN_MAP.get(name);
        var index = TRUE_BUILTINS.indexOf(builtin);
        if (index == -1) {
            return builtin instanceof LangConstant ? Optional.of((LangConstant) builtin) : Optional.empty();
        } else {
            return Optional.of(new BuiltinConstant(index));
        }
    }

    private static final LangConstant STR_CONSTANT = new BuiltinConstant(TRUE_BUILTINS.indexOf(STR));
    private static final LangConstant ITER_CONSTANT = new BuiltinConstant(TRUE_BUILTINS.indexOf(ITER));
    private static final LangConstant RANGE_CONSTANT = new BuiltinConstant(TRUE_BUILTINS.indexOf(RANGE));

    public static LangConstant strConstant() {
        return STR_CONSTANT;
    }

    public static LangConstant iterConstant() {
        return ITER_CONSTANT;
    }

    public static LangConstant rangeConstant() {
        return RANGE_CONSTANT;
    }

    public static TypeObject[] deIterable(@NotNull TypeObject val) {
        assert val.sameBaseType(Builtins.ITERABLE);
        var generics = val.getGenerics().get(0);
        assert generics instanceof ListTypeObject;
        return ((ListTypeObject) generics).getValues();
    }
}
