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

    private static final Map<OpSpTypeNode, MethodInfo> HASHABLE_MAP = Map.of(
            OpSpTypeNode.HASH, MethodInfo.of(ArgumentInfo.of(), Builtins.INT)
    );

    public static final InterfaceType HASHABLE = new InterfaceType("Hashable", GenericInfo.empty(), HASHABLE_MAP);

    private static final TemplateParam ITERABLE_PARAM = new TemplateParam("K", 0, true);

    public static final InterfaceType ITERABLE = new InterfaceType("Iterable", GenericInfo.of(ITERABLE_PARAM));

    private static final TemplateParam ITERATOR_PARAM = new TemplateParam("K", 0, true);

    public static final InterfaceType ITERATOR = new InterfaceType(
            "Iterator", GenericInfo.of(ITERATOR_PARAM), List.of(ITERABLE.generify(ITERATOR_PARAM))
    );

    public static final StdTypeObject INT = new StdTypeObject("int", List.of(HASHABLE));

    public static final StdTypeObject CHAR = new StdTypeObject("char", List.of(HASHABLE));

    public static final StdTypeObject STR = new StdTypeObject("str", List.of(ITERABLE.generify(CHAR), HASHABLE));

    public static final StdTypeObject DECIMAL = new StdTypeObject("dec", List.of(HASHABLE));

    public static final StdTypeObject BOOL = new StdTypeObject("bool", List.of(INT));

    public static final StdTypeObject RANGE = new StdTypeObject("range", List.of(ITERABLE.generify(INT)));

    public static final StdTypeObject BYTES = new StdTypeObject("bytes", List.of(ITERABLE.generify(INT)));

    public static final StdTypeObject THROWS = new StdTypeObject("throws");

    public static final StdTypeObject SLICE = new StdTypeObject("slice");

    public static final TypeObject TYPE = new TypeTypeObject();

    private static final FunctionInfo PRINT_INFO = new FunctionInfo("print", ArgumentInfo.of(OBJECT));

    public static final LangObject PRINT = new LangInstance(PRINT_INFO.toCallable());

    private static final FunctionInfo INPUT_INFO = new FunctionInfo("input", ArgumentInfo.of(STR), STR);

    public static final LangObject INPUT = new LangInstance(INPUT_INFO.toCallable());

    private static final TemplateParam ITER_PARAM = new TemplateParam("K", 0, true);

    private static final FunctionInfo ITER_INFO = new FunctionInfo(
            "iter", ArgumentInfo.of(ITERABLE.generify(ITER_PARAM)), ITERATOR.generify(ITER_PARAM).makeMut()
    );

    public static final LangObject ITER = new LangInstance(ITER_INFO.toCallable());

    private static final FunctionInfo REPR_INFO = new FunctionInfo("repr", ArgumentInfo.of(OBJECT), STR);

    public static final LangObject REPR = new LangInstance(REPR_INFO.toCallable());

    private static final TemplateParam REVERSED_PARAM = new TemplateParam("T", 0, Builtins.OBJECT);

    private static final FunctionInfo REVERSED_INFO = new FunctionInfo(
            "reversed", ArgumentInfo.of(REVERSED_PARAM), REVERSED_PARAM
    );

    public static final LangObject REVERSED = new LangInstance(REVERSED_INFO.toCallable());

    private static final TemplateParam LIST_PARAM = new TemplateParam("T", 0, OBJECT);

    public static final StdTypeObject LIST = new StdTypeObject(
            "list", List.of(ITERABLE.generify(LIST_PARAM)), GenericInfo.of(LIST_PARAM)
    );

    private static final TemplateParam SET_PARAM = new TemplateParam("T", 0, OBJECT);

    public static final StdTypeObject SET = new StdTypeObject(
            "set", List.of(ITERABLE.generify(SET_PARAM)), GenericInfo.of(SET_PARAM)
    );

    private static final TemplateParam DICT_KEY = new TemplateParam("K", 0, HASHABLE);

    private static final TemplateParam DICT_VAL = new TemplateParam("V", 1, OBJECT);

    public static final StdTypeObject DICT = new StdTypeObject(
            "dict", List.of(ITERABLE.generify(DICT_KEY, DICT_VAL)), GenericInfo.of(DICT_KEY, DICT_VAL)
    );

    public static final LangConstant TRUE = new BoolConstant(true);

    public static final LangConstant FALSE = new BoolConstant(false);

    public static final LangObject OPEN = new LangInstance(CONTEXT.generify(OBJECT));  // TODO: File object

    public static final FunctionInfo ID_INFO = new FunctionInfo("id", ArgumentInfo.of(Builtins.OBJECT), INT);

    public static final LangObject ID = new LangInstance(ID_INFO.toCallable());

    public static final FunctionInfo HASH_INFO = new FunctionInfo("hash", ArgumentInfo.of(OBJECT), INT);

    public static final LangObject HASH = new LangInstance(HASH_INFO.toCallable());

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
    public static final StdTypeObject VALUE_ERROR = new StdTypeObject("ValueError", List.of(THROWABLE));

    public static final LangConstant NULL = new NullConstant();

    public static final StdTypeObject NULL_TYPE = NullConstant.TYPE;

    public static final Set<InterfaceType> DEFAULT_INTERFACES = Set.of(
            CONTEXT, CALLABLE, HASHABLE, ITERABLE
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

                Map.entry(OpSpTypeNode.NEW, MethodInfo.of(ArgumentInfo.of(OBJECT))),
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
                OpSpTypeNode.IN, MethodInfo.of(ArgumentInfo.of(CHAR), BOOL),
                OpSpTypeNode.NEW, MethodInfo.of(ArgumentInfo.of(OBJECT))
        );
        STR.setOperators(strMap);
        var fromCharsInfo = new FunctionInfo(ArgumentInfo.of(LIST.generify(CHAR)), STR);
        var staticStrMap = Map.of(
                "fromChars", AttributeInfo.method(fromCharsInfo)
        );
        STR.setStaticAttributes(staticStrMap);
        var getInfo = new FunctionInfo(ArgumentInfo.of(INT), TypeObject.optional(CHAR));
        var joinInfo = new FunctionInfo(ArgumentInfo.of(ITERABLE.generify(OBJECT)), STR);
        var startsInfo = new FunctionInfo(ArgumentInfo.of(STR), BOOL);
        var splitInfo = new FunctionInfo(ArgumentInfo.of(STR), LIST.generify(STR));
        var splitLinesInfo = new FunctionInfo(ArgumentInfo.of(), LIST.generify(STR));
        var upperLowerInfo = new FunctionInfo(STR);
        var indexInfo = new FunctionInfo(ArgumentInfo.of(STR), TypeObject.optional(INT));
        var encodeInfo = new FunctionInfo(ArgumentInfo.of(STR), BYTES.makeMut());
        var asIntInfo = new FunctionInfo(ArgumentInfo.of(), TypeObject.optional(INT));
        var strAttrs = Map.ofEntries(
                Map.entry("length", new AttributeInfo(AccessLevel.PUBLIC, INT)),
                Map.entry("chars", new AttributeInfo(AccessLevel.PUBLIC, LIST.generify(CHAR))),
                Map.entry("get", AttributeInfo.method(getInfo)),
                Map.entry("join", AttributeInfo.method(joinInfo)),
                Map.entry("startsWith", AttributeInfo.method(startsInfo)),
                Map.entry("endsWith", AttributeInfo.method(startsInfo)),
                Map.entry("split", AttributeInfo.method(splitInfo)),
                Map.entry("splitLines", AttributeInfo.method(splitLinesInfo)),
                Map.entry("upper", AttributeInfo.method(upperLowerInfo)),
                Map.entry("lower", AttributeInfo.method(upperLowerInfo)),
                Map.entry("indexOf", AttributeInfo.method(indexInfo)),
                Map.entry("lastIndexOf", AttributeInfo.method(indexInfo)),
                Map.entry("encode", AttributeInfo.method(encodeInfo)),
                Map.entry("asInt", AttributeInfo.method(asIntInfo))
        );
        STR.setAttributes(strAttrs);
        STR.seal();
    }

    static {  // Set bytes operators
        var bytesMap = Map.of(
                OpSpTypeNode.ADD, MethodInfo.of(ArgumentInfo.of(BYTES), BYTES.makeMut()),
                OpSpTypeNode.MULTIPLY, MethodInfo.of(ArgumentInfo.of(INT), BYTES.makeMut()),
                OpSpTypeNode.EQUALS, MethodInfo.of(ArgumentInfo.of(BYTES), BOOL),
                OpSpTypeNode.GET_ATTR, MethodInfo.of(ArgumentInfo.of(INT), INT),
                OpSpTypeNode.GET_SLICE, MethodInfo.of(ArgumentInfo.of(SLICE), BYTES.makeMut()),
                OpSpTypeNode.SET_ATTR, MethodInfo.of(ArgumentInfo.of(INT, INT)),
                OpSpTypeNode.ITER, MethodInfo.of(ITERABLE.generify(INT)),
                OpSpTypeNode.IN, MethodInfo.of(ArgumentInfo.of(INT), BOOL),
                OpSpTypeNode.NEW, MethodInfo.of(ArgumentInfo.of(OBJECT))
        );
        BYTES.setOperators(bytesMap);
        var fromHexInfo = new FunctionInfo(ArgumentInfo.of(STR), BYTES);
        var staticBytesAttrs = Map.of(
                "fromHex", AttributeInfo.method(fromHexInfo)
        );
        BYTES.setStaticAttributes(staticBytesAttrs);
        var joinInfo = new FunctionInfo(ArgumentInfo.of(ITERABLE.generify(OBJECT), BYTES));
        var encodeInfo = new FunctionInfo(ArgumentInfo.of(STR), STR);
        var indexInfo = new FunctionInfo(ArgumentInfo.of(INT), TypeObject.optional(INT));
        var addInfo = new FunctionInfo(ArgumentInfo.of(INT));
        var addCharInfo = new FunctionInfo(ArgumentInfo.of(CHAR, STR));
        var getInfo = new FunctionInfo(ArgumentInfo.of(INT), TypeObject.optional(INT));
        var startsInfo = new FunctionInfo(ArgumentInfo.of(BYTES), BOOL);
        var hexInfo = new FunctionInfo(STR);
        var bytesAttrs = Map.ofEntries(
                Map.entry("length", new AttributeInfo(AccessLevel.PUBLIC, INT)),
                Map.entry("join", AttributeInfo.method(joinInfo)),
                Map.entry("encode", AttributeInfo.method(encodeInfo)),
                Map.entry("indexOf", AttributeInfo.method(indexInfo)),
                Map.entry("get", AttributeInfo.method(getInfo)),
                Map.entry("add", AttributeInfo.mutMethod(addInfo)),
                Map.entry("addChar", AttributeInfo.method(addCharInfo)),
                Map.entry("startsWith", AttributeInfo.method(startsInfo)),
                Map.entry("endsWith", AttributeInfo.method(startsInfo)),
                Map.entry("lastIndexOf", AttributeInfo.method(indexInfo)),
                Map.entry("hex", AttributeInfo.method(hexInfo))
        );
        BYTES.setAttributes(bytesAttrs);
        BYTES.seal();
    }

    static {  // Set char operators
        CHAR.isConstClass();
        // TODO: More char operators
        var charMap = Map.of(
                OpSpTypeNode.NEW, MethodInfo.of(ArgumentInfo.of(INT)),
                OpSpTypeNode.ADD, MethodInfo.of(ArgumentInfo.of(CHAR), CHAR),
                OpSpTypeNode.SUBTRACT, MethodInfo.of(ArgumentInfo.of(CHAR), CHAR),
                OpSpTypeNode.EQUALS, MethodInfo.of(ArgumentInfo.of(CHAR), BOOL)
        );
        CHAR.setOperators(charMap);
        var fromIntInfo = new FunctionInfo(ArgumentInfo.of(INT), TypeObject.optional(CHAR));
        var staticCharAttrs = Map.of(
                "fromInt", AttributeInfo.method(fromIntInfo)
        );
        CHAR.setStaticAttributes(staticCharAttrs);
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
        var getInfo = new FunctionInfo(ArgumentInfo.of(INT), TypeObject.optional(INT));
        var rangeAttrs = Map.of(
                "length", new AttributeInfo(AccessLevel.PUBLIC, INT),
                "get", AttributeInfo.method(getInfo)
        );
        RANGE.setAttributes(rangeAttrs);
        RANGE.seal();
    }

    static {  // Set slice operators
        SLICE.isConstClass();
        var endInfo = new AttributeInfo(AccessLevel.PUBLIC, TypeObject.optional(INT));
        var rangeInfo = AttributeInfo.method(new FunctionInfo(ArgumentInfo.of(INT), RANGE));
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
        var listGen = LIST.generify(LIST_PARAM);
        var iterGen = ITERABLE.generify(LIST_PARAM);
        var listMap = Map.ofEntries(
                Map.entry(OpSpTypeNode.NEW, MethodInfo.of(ArgumentInfo.of(iterGen))),
                Map.entry(OpSpTypeNode.GET_ATTR, MethodInfo.of(ArgumentInfo.of(INT), LIST_PARAM)),
                Map.entry(OpSpTypeNode.SET_ATTR, MethodInfo.ofMut(ArgumentInfo.of(INT, LIST_PARAM))),
                Map.entry(OpSpTypeNode.DEL_ATTR, MethodInfo.ofMut(ArgumentInfo.of(INT))),
                Map.entry(OpSpTypeNode.GET_SLICE, MethodInfo.of(ArgumentInfo.of(SLICE), listGen.makeMut())),
                Map.entry(OpSpTypeNode.SET_SLICE, MethodInfo.ofMut(ArgumentInfo.of(SLICE, iterGen))),
                Map.entry(OpSpTypeNode.DEL_SLICE, MethodInfo.ofMut(ArgumentInfo.of(SLICE))),
                Map.entry(OpSpTypeNode.ITER_SLICE, MethodInfo.of(ArgumentInfo.of(SLICE), iterGen)),
                Map.entry(OpSpTypeNode.IN, MethodInfo.of(ArgumentInfo.of(LIST_PARAM), BOOL)),
                Map.entry(OpSpTypeNode.REVERSED, MethodInfo.of(listGen)),
                Map.entry(OpSpTypeNode.ADD, MethodInfo.of(ArgumentInfo.of(listGen), listGen.makeMut())),
                Map.entry(OpSpTypeNode.MULTIPLY, MethodInfo.of(ArgumentInfo.of(INT), listGen.makeMut())),
                Map.entry(OpSpTypeNode.ITER, MethodInfo.of(ITERABLE.generify(LIST_PARAM)))
        );
        LIST.setOperators(listMap);
        var getInfo = new FunctionInfo(ArgumentInfo.of(INT), TypeObject.optional(LIST_PARAM));
        var insertInfo = new FunctionInfo(ArgumentInfo.of(INT, LIST_PARAM));
        var popInfo = new FunctionInfo(TypeObject.optional(LIST_PARAM));
        var popFirstInfo = new FunctionInfo(TypeObject.optional(LIST_PARAM));
        var reverseInfo = new FunctionInfo();
        var countInfo = new FunctionInfo(ArgumentInfo.of(LIST_PARAM), INT);
        var clearInfo = new FunctionInfo();
        var addInfo = new FunctionInfo(ArgumentInfo.of(LIST_PARAM));
        var addAllInfo = new FunctionInfo(ArgumentInfo.of(iterGen));
        var indexOfInfo = new FunctionInfo(ArgumentInfo.of(LIST_PARAM),TypeObject.optional(INT));
        var swapInfo = new FunctionInfo(ArgumentInfo.of(INT, INT));
        var listAttrs = Map.ofEntries(
                Map.entry("length", new AttributeInfo(AccessLevel.PUBLIC, INT)),
                Map.entry("get", AttributeInfo.method(getInfo)),
                Map.entry("insert", AttributeInfo.mutMethod(insertInfo)),
                Map.entry("pop", AttributeInfo.mutMethod(popInfo)),
                Map.entry("reverse", AttributeInfo.method(reverseInfo)),
                Map.entry("count", AttributeInfo.method(countInfo)),
                Map.entry("clear", AttributeInfo.mutMethod(clearInfo)),
                Map.entry("indexOf", AttributeInfo.method(indexOfInfo)),
                Map.entry("add", AttributeInfo.mutMethod(addInfo)),
                Map.entry("addAll", AttributeInfo.mutMethod(addAllInfo)),
                Map.entry("popFirst", AttributeInfo.mutMethod(popFirstInfo)),
                Map.entry("swap", AttributeInfo.mutMethod(swapInfo))
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
        var addInfo = new FunctionInfo(ArgumentInfo.of(SET_PARAM));
        var addAllInfo = new FunctionInfo(ArgumentInfo.of(ITERABLE.generify(SET_PARAM)));
        var clearInfo = new FunctionInfo();
        var setAttrs = Map.of(
                "length", new AttributeInfo(AccessLevel.PUBLIC, INT),
                "add", AttributeInfo.mutMethod(addInfo),
                "addAll", AttributeInfo.mutMethod(addAllInfo),
                "clear", AttributeInfo.mutMethod(clearInfo)
        );
        SET.setAttributes(setAttrs);
        SET.seal();

        SET_PARAM.setParent(SET);
    }

    static {
        var dictContainsInfo = MethodInfo.of(ArgumentInfo.of(DICT_KEY), BOOL);
        var dictGetInfo = MethodInfo.of(ArgumentInfo.of(DICT_KEY), DICT_VAL);
        var dictSetInfo = MethodInfo.ofMut(ArgumentInfo.of(DICT_KEY, DICT_VAL));
        var dictDelInfo = MethodInfo.ofMut(ArgumentInfo.of(DICT_KEY));
        var dictIterInfo = MethodInfo.of(DICT_KEY, Builtins.ITERABLE.generify(DICT_VAL));
        var dictEqInfo = MethodInfo.of(ArgumentInfo.of(DICT), BOOL);

        var dictGetMInfo = new FunctionInfo(ArgumentInfo.of(DICT_KEY), TypeObject.optional(DICT_VAL));
        var dictClearInfo = new FunctionInfo();
        var dictDefaultInfo = new FunctionInfo(ArgumentInfo.of(DICT_KEY, DICT_VAL), DICT_VAL);
        var dictReplaceInfo = new FunctionInfo(ArgumentInfo.of(DICT_KEY, DICT_VAL), TypeObject.optional(DICT_VAL));
        var dictRemoveInfo = new FunctionInfo(ArgumentInfo.of(DICT_KEY), TypeObject.optional(DICT_VAL));

        var dictMap = Map.of(
                OpSpTypeNode.IN, dictContainsInfo,
                OpSpTypeNode.GET_ATTR, dictGetInfo,
                OpSpTypeNode.SET_ATTR, dictSetInfo,
                OpSpTypeNode.DEL_ATTR, dictDelInfo,
                OpSpTypeNode.ITER, dictIterInfo,
                OpSpTypeNode.EQUALS, dictEqInfo
        );
        DICT.setOperators(dictMap);
        var dictAttrs = Map.of(
                "length", new AttributeInfo(AccessLevel.PUBLIC, INT),
                "get", AttributeInfo.method(dictGetMInfo),
                "clear", AttributeInfo.mutMethod(dictClearInfo),
                "setDefault", AttributeInfo.mutMethod(dictDefaultInfo),
                "replace", AttributeInfo.mutMethod(dictReplaceInfo),
                "remove", AttributeInfo.mutMethod(dictRemoveInfo)
        );
        DICT.setAttributes(dictAttrs);
        DICT.seal();

        DICT_KEY.setParent(DICT);
        DICT_VAL.setParent(DICT);
    }

    static {
        var arrayNewInfo = MethodInfo.of(ArgumentInfo.of(ARRAY_PARAM, INT));
        var arrayGetInfo = MethodInfo.of(ArgumentInfo.of(INT), ARRAY_PARAM);
        var arraySetInfo = MethodInfo.of(ArgumentInfo.of(INT, ARRAY_PARAM));
        var arrayIterInfo = MethodInfo.of(ITERABLE.generify(ARRAY_PARAM));
        var arrayContainsInfo = MethodInfo.of(ArgumentInfo.of(ARRAY_PARAM), BOOL);
        var arrayEqInfo = MethodInfo.of(ArgumentInfo.of(ARRAY), BOOL);
        var arraySliceInfo = MethodInfo.of(ArgumentInfo.of(SLICE), LIST.generify(ARRAY_PARAM));
        var arrayReversedInfo = MethodInfo.of(ARRAY);

        var arrayMap = Map.of(
                OpSpTypeNode.NEW, arrayNewInfo,
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
        var boolNewInfo = MethodInfo.of(ArgumentInfo.of(OBJECT));
        var boolBoolInfo = MethodInfo.of(BOOL);

        var boolMap = Map.of(
                OpSpTypeNode.NEW, boolNewInfo,
                OpSpTypeNode.BOOL, boolBoolInfo
        );
        BOOL.setOperators(boolMap);

        BOOL.isConstClass();
        BOOL.seal();

    }

    static {
        var iterInfo = MethodInfo.of(ITERABLE.generify(LineInfo.empty(), ITERABLE_PARAM));

        ITERABLE.setOperators(Map.of(OpSpTypeNode.ITER, iterInfo), Set.of(OpSpTypeNode.ITER));
        ITERABLE.seal();
    }

    static {
        var nextFnInfo = new FunctionInfo("next", ArgumentInfo.of(), TypeObject.optional(ITERATOR_PARAM));
        var peekFnInfo = new FunctionInfo("peek", ArgumentInfo.of(), TypeObject.optional(ITERATOR_PARAM));
        var nextInfo = AttributeInfo.method(nextFnInfo);
        var peekInfo = AttributeInfo.method(peekFnInfo);
        var iterInfo = MethodInfo.of(ITERABLE.generify(LineInfo.empty(), ITERATOR_PARAM));

        ITERATOR.setOperators(Map.of(OpSpTypeNode.ITER, iterInfo), Set.of(OpSpTypeNode.ITER));
        ITERATOR.setAttributes(Map.of("next", nextInfo, "peek", peekInfo), Set.of("next"));
        ITERATOR.seal();
    }

    static {
        var notImplConstructor = MethodInfo.of();
        NOT_IMPLEMENTED.setOperators(Map.of(OpSpTypeNode.NEW, notImplConstructor));
        NOT_IMPLEMENTED.seal();

        var valueErrConstructor = MethodInfo.of(ArgumentInfo.of(STR));
        VALUE_ERROR.setOperators(Map.of(OpSpTypeNode.NEW, valueErrConstructor));
        VALUE_ERROR.seal();
    }

    static {  // null is const
        NULL_TYPE.isConstClass();
        NULL_TYPE.seal();
    }

    static {  // seal everything else
        DECIMAL.isConstClass();
        DECIMAL.seal();

        THROWS.isConstClass();
        THROWS.seal();

        THROWABLE.seal();
    }

    static {  // Set interface parents
        CONTEXT_PARAM.setParent(CONTEXT);

        ITERABLE_PARAM.setParent(ITERABLE);

        ITERATOR_PARAM.setParent(ITERATOR);

        ITER_PARAM.setParent(ITER_INFO.toCallable());

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
            TUPLE,
            THROWABLE,
            NULL_TYPE,
            HASH,
            VALUE_ERROR
    );

    public static final Map<String, LangObject> BUILTIN_MAP = Map.ofEntries(
            Map.entry("print", PRINT),
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
            Map.entry("ValueError", VALUE_ERROR),
            Map.entry("Iterator", ITERATOR),
            Map.entry("hash", HASH),
            Map.entry("Hashable", HASHABLE),
            Map.entry("null", NULL)
    );

    @NotNull
    @Contract("_ -> new")
    public static Optional<LangConstant> constantOf(String name) {
        var builtin = BUILTIN_MAP.get(name);
        if (builtin == null) {
            return Optional.empty();
        }
        var index = TRUE_BUILTINS.indexOf(builtin);
        if (index == -1) {
            return builtin instanceof LangConstant ? Optional.of((LangConstant) builtin) : Optional.empty();
        } else {
            return Optional.of(new BuiltinConstant(index));
        }
    }

    public static Optional<FunctionInfo> functionOf(String name) {
        var builtin = BUILTIN_MAP.get(name);
        if (builtin == null) {
            return Optional.empty();
        }
        return builtin instanceof FunctionInfoType
                ? Optional.of(((FunctionInfoType) builtin).getInfo())
                : Optional.empty();
    }

    private static final LangConstant STR_CONSTANT = new BuiltinConstant(TRUE_BUILTINS.indexOf(STR));
    private static final LangConstant ITER_CONSTANT = new BuiltinConstant(TRUE_BUILTINS.indexOf(ITER));
    private static final LangConstant RANGE_CONSTANT = new BuiltinConstant(TRUE_BUILTINS.indexOf(RANGE));
    private static final LangConstant BOOL_CONSTANT = new BuiltinConstant(TRUE_BUILTINS.indexOf(BOOL));
    private static final LangConstant NULL_TYPE_CONSTANT = new BuiltinConstant(TRUE_BUILTINS.indexOf(NULL_TYPE));

    public static LangConstant strConstant() {
        return STR_CONSTANT;
    }

    public static LangConstant iterConstant() {
        return ITER_CONSTANT;
    }

    public static LangConstant rangeConstant() {
        return RANGE_CONSTANT;
    }

    public static LangConstant boolConstant() {
        return BOOL_CONSTANT;
    }

    public static LangConstant nullTypeConstant() {
        return NULL_TYPE_CONSTANT;
    }

    public static TypeObject[] deIterable(@NotNull TypeObject val) {
        if (val.sameBaseType(Builtins.ITERABLE)) {
            var generics = val.getGenerics().get(0);
            assert generics instanceof ListTypeObject;
            return ((ListTypeObject) generics).getValues();
        } else {
            assert Builtins.ITERABLE.isSuperclass(val);
            return deIterable(val.operatorReturnType(OpSpTypeNode.ITER, AccessLevel.PUBLIC).orElseThrow()[0]);
        }
    }
}
