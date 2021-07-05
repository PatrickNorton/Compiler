package main.java.converter;

import main.java.parser.LineInfo;
import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class Builtins {
    private Builtins() {
        throw new UnsupportedOperationException("No Builtins for you!");
    }

    public static final Version CURRENT_VERSION = new Version(0, 0, 1);

    public static final Set<String> FORBIDDEN_NAMES = Set.of(
            "true",
            "false",
            "__default__",
            "self",
            "cls",
            "super",
            "null"
    );

    private static final ObjectType OBJECT = new ObjectType();

    public static ObjectType object() {
        return OBJECT;
    }

    private static final TemplateParam CONTEXT_PARAM = new TemplateParam("T", 0, OBJECT);

    private static final Map<OpSpTypeNode, MethodInfo> CONTEXT_MAP = Map.of(
            OpSpTypeNode.ENTER, MethodInfo.of(CONTEXT_PARAM),
            OpSpTypeNode.EXIT, MethodInfo.of(TypeObject.list())
    );

    private static final InterfaceType CONTEXT = new InterfaceType(
            "Context", GenericInfo.of(CONTEXT_PARAM), CONTEXT_MAP
    );

    public static InterfaceType context() {
        return CONTEXT;
    }

    private static final TemplateParam CALLABLE_ARGS = new TemplateParam("K", 0, true);

    private static final TemplateParam CALLABLE_RETURN = new TemplateParam("R", 1, TypeObject.list());

    private static final Map<OpSpTypeNode, MethodInfo> CALLABLE_MAP = Map.of(
            OpSpTypeNode.CALL, MethodInfo.of(ArgumentInfo.of(CALLABLE_ARGS), CALLABLE_RETURN)
    );

    private static final InterfaceType CALLABLE = new InterfaceType(
            "Callable", GenericInfo.of(CALLABLE_ARGS, CALLABLE_RETURN), CALLABLE_MAP
    );

    public static InterfaceType callable() {
        return CALLABLE;
    }

    private static final TemplateParam ITERABLE_PARAM = new TemplateParam("K", 0, true);

    private static final InterfaceType ITERABLE = new InterfaceType("Iterable", GenericInfo.of(ITERABLE_PARAM));

    public static InterfaceType iterable() {
        return ITERABLE;
    }

    private static final TemplateParam ITERATOR_PARAM = new TemplateParam("K", 0, true);

    private static final InterfaceType ITERATOR = new InterfaceType(
            "Iterator", GenericInfo.of(ITERATOR_PARAM), List.of(ITERABLE.generify(ITERATOR_PARAM))
    );

    public static InterfaceType iterator() {
        return ITERATOR;
    }

    private static final TypeObject TYPE = new TypeTypeObject();

    public static TypeObject type() {
        return TYPE;
    }

    public static final LangConstant TRUE = new BoolConstant(true);

    public static final LangConstant FALSE = new BoolConstant(false);

    private static final TupleType TUPLE = new TupleType();

    public static TupleType tuple() {
        return TUPLE;
    }

    private static final StdTypeObject THROWS = new StdTypeObject("throws");

    public static TypeObject throwsType() {
        return THROWS;
    }

    private static final LangConstant NULL = new NullConstant();
    private static final StdTypeObject NULL_TYPE = NullConstant.TYPE;

    public static StdTypeObject nullType() {
        return NULL_TYPE;
    }

    // These are all created by the parsing of __builtins__.newlang, which is parsed at runtime

    public static TypeObject range() {
        return Objects.requireNonNull((TypeObject) BUILTIN_MAP.get("range"));
    }

    public static TypeObject str() {
        return Objects.requireNonNull((TypeObject) BUILTIN_MAP.get("str"));
    }

    public static TypeObject intType() {
        return Objects.requireNonNull((TypeObject) BUILTIN_MAP.get("int"));
    }

    public static TypeObject list() {
        return Objects.requireNonNull((TypeObject) BUILTIN_MAP.get("list"));
    }

    public static TypeObject set() {
        return Objects.requireNonNull((TypeObject) BUILTIN_MAP.get("set"));
    }

    public static TypeObject dict() {
        return Objects.requireNonNull((TypeObject) BUILTIN_MAP.get("dict"));
    }

    public static TypeObject slice() {
        return Objects.requireNonNull((TypeObject) BUILTIN_MAP.get("slice"));
    }

    public static TypeObject bytes() {
        return Objects.requireNonNull((TypeObject) BUILTIN_MAP.get("bytes"));
    }

    public static TypeObject charType() {
        return Objects.requireNonNull((TypeObject) BUILTIN_MAP.get("char"));
    }

    public static TypeObject decimal() {
        return Objects.requireNonNull((TypeObject) BUILTIN_MAP.get("dec"));
    }

    public static TypeObject bool() {
        return Objects.requireNonNull((TypeObject) BUILTIN_MAP.get("bool"));
    }

    public static TypeObject nullError() {
        return Objects.requireNonNull((TypeObject) BUILTIN_MAP.get("NullError"));
    }

    public static TypeObject assertError() {
        return Objects.requireNonNull((TypeObject) BUILTIN_MAP.get("AssertionError"));
    }

    public static TypeObject throwable() {
        return Objects.requireNonNull((TypeObject) BUILTIN_MAP.get("Throwable"));
    }

    public static LangObject iter() {
        return Objects.requireNonNull(BUILTIN_MAP.get("iter"));
    }

    public static TypeObject hashable() {
        return Objects.requireNonNull((TypeObject) BUILTIN_MAP.get("Hashable"));
    }

    public static final Set<InterfaceType> DEFAULT_INTERFACES = Set.of(
            CONTEXT, CALLABLE, ITERABLE
    );

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

    static {  // null is const
        NULL_TYPE.isConstClass();
        NULL_TYPE.seal();
    }

    static {  // Set interface parents
        CONTEXT_PARAM.setParent(CONTEXT);

        ITERABLE_PARAM.setParent(ITERABLE);

        ITERATOR_PARAM.setParent(ITERATOR);

        CALLABLE_ARGS.setParent(CALLABLE);
        CALLABLE_RETURN.setParent(CALLABLE);
    }

    private static final List<LangObject> TRUE_BUILTINS = new ArrayList<>(Arrays.asList(
            null,  // print
            CALLABLE,
            null,  // int
            null,  // str
            null,  // bool
            null,  // range
            TYPE,
            null,  // iter
            null,  // repr
            null,  // input
            null,  // list
            null,  // set
            null,  // char
            null,  // open
            null,  // reversed
            null,  // slice
            null,  // id
            null,  // Array
            null,  // enumerate
            null,  // bytes
            null,  // dict
            OBJECT,
            null,  // NotImplemented
            TUPLE,
            null,  // Throwable
            NULL_TYPE,
            null,  // hash
            null,  // ValueError
            null,  // NullError
            ITERABLE,
            null   // AssertionError
    ));

    public static final Map<String, LangObject> BUILTIN_MAP = new HashMap<>(Map.ofEntries(
            Map.entry("type", TYPE),
            Map.entry("true", TRUE),
            Map.entry("false", FALSE),
            Map.entry("Callable", CALLABLE),
            Map.entry("Iterable", ITERABLE),
            Map.entry("object", OBJECT),
            Map.entry("Iterator", ITERATOR),
            Map.entry("tuple", TUPLE),
            Map.entry("null", NULL)
    ));

    public static LangObject constantNo(int index) {
        return TRUE_BUILTINS.get(index);
    }

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

    public static void setBuiltin(String name, int index, LangObject value) {
        assert !BUILTIN_MAP.containsKey(name);
        BUILTIN_MAP.put(name, value);
        if (index != -1) {
            assert index < TRUE_BUILTINS.size() && TRUE_BUILTINS.get(index) == null;
            TRUE_BUILTINS.set(index, value);
        }
    }

    // Prevents initialization until needed, thus preventing loading before types initialized
    private static final class ConstantHolder {
        private static final LangConstant ITER_CONSTANT = new BuiltinConstant(TRUE_BUILTINS.indexOf(Builtins.iter()));
        private static final LangConstant RANGE_CONSTANT = new BuiltinConstant(TRUE_BUILTINS.indexOf(Builtins.range()));
        private static final LangConstant BOOL_CONSTANT = new BuiltinConstant(TRUE_BUILTINS.indexOf(Builtins.bool()));
        private static final LangConstant NULL_TYPE_CONSTANT = new BuiltinConstant(TRUE_BUILTINS.indexOf(NULL_TYPE));
        private static final LangConstant NULL_ERROR_CONSTANT = new BuiltinConstant(TRUE_BUILTINS.indexOf(nullError()));
        private static final LangConstant ASSERTION_CONSTANT = new BuiltinConstant(TRUE_BUILTINS.indexOf(assertError()));
        private static final LangConstant CHAR_CONSTANT = new BuiltinConstant(TRUE_BUILTINS.indexOf(charType()));
    }

    public static LangConstant iterConstant() {
        return ConstantHolder.ITER_CONSTANT;
    }

    public static LangConstant rangeConstant() {
        return ConstantHolder.RANGE_CONSTANT;
    }

    public static LangConstant boolConstant() {
        return ConstantHolder.BOOL_CONSTANT;
    }

    public static LangConstant nullTypeConstant() {
        return ConstantHolder.NULL_TYPE_CONSTANT;
    }

    public static LangConstant nullErrorConstant() {
        return ConstantHolder.NULL_ERROR_CONSTANT;
    }

    public static LangConstant assertionErrorConstant() {
        return ConstantHolder.ASSERTION_CONSTANT;
    }

    public static LangConstant charConstant() {
        return ConstantHolder.CHAR_CONSTANT;
    }

    public static TypeObject[] deIterable(@NotNull TypeObject val) {
        if (val.sameBaseType(Builtins.iterable())) {
            var generics = val.getGenerics().get(0);
            assert generics instanceof ListTypeObject;
            return ((ListTypeObject) generics).getValues();
        } else {
            assert Builtins.iterable().isSuperclass(val);
            return deIterable(val.operatorReturnType(OpSpTypeNode.ITER, AccessLevel.PUBLIC).orElseThrow()[0]);
        }
    }

    public static final Set<String> STABLE_FEATURES = Set.of();
}
