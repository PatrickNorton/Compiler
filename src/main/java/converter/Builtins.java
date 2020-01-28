package main.java.converter;

import main.java.parser.OpSpTypeNode;
import main.java.parser.OperatorTypeNode;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Builtins {
    private Builtins() {
        throw new UnsupportedOperationException("No Builtins for you!");
    }

    public static final List<String> BUILTIN_INDICES = List.of(
            "print",
            "true",
            "false"
    );

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

    public static final TypeObject TYPE = new TypeTypeObject();

    public static final LangConstant PRINT = new BuiltinConstant(BUILTIN_INDICES.indexOf("print"));

    public static final LangConstant TRUE = new BoolConstant(true);

    public static final LangConstant FALSE = new BoolConstant(false);

    static {  // Set int operators
        var intOpArgInfo = new ArgumentInfo(new Argument("", INT));
        var intOperatorInfo = new FunctionInfo("", intOpArgInfo, INT);
        INT.setOperator(OperatorTypeNode.ADD, intOperatorInfo);
        INT.setOperator(OperatorTypeNode.SUBTRACT, intOperatorInfo);
        INT.setOperator(OperatorTypeNode.MULTIPLY, intOperatorInfo);
        INT.setOperator(OperatorTypeNode.DIVIDE, intOperatorInfo);
    }

    static {  // Set str operators
        var strPlusInfo = new FunctionInfo("", new ArgumentInfo(new Argument("", STR)), STR);
        STR.setOperator(OperatorTypeNode.ADD, strPlusInfo);
        var strTimesInfo = new FunctionInfo("", new ArgumentInfo(new Argument("", INT)), STR);
        STR.setOperator(OperatorTypeNode.MULTIPLY, strTimesInfo);
    }

    public static final Map<String, VariableInfo> NAMES = Map.of(
            "print", new VariableInfo(CALLABLE, PRINT, -1),
            "true", new VariableInfo(BOOL, TRUE, -1),
            "false", new VariableInfo(BOOL, FALSE, -1)
    );

    public static final List<LangConstant> BUILTINS = List.of(
            PRINT,
            TRUE,
            FALSE
    );
}
