package main.java.converter;

import main.java.parser.AnnotatableNode;
import main.java.parser.BaseClassNode;
import main.java.parser.DefinitionNode;
import main.java.parser.EnumDefinitionNode;
import main.java.parser.FunctionCallNode;
import main.java.parser.FunctionDefinitionNode;
import main.java.parser.Lined;
import main.java.parser.NameNode;
import main.java.parser.NumberNode;
import main.java.parser.OperatorNode;
import main.java.parser.StringNode;
import main.java.parser.TestNode;
import main.java.parser.UnionDefinitionNode;
import main.java.parser.VariableNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class AnnotationConverter implements BaseConverter {
    private final AnnotatableNode node;
    private final CompilerInfo info;

    public AnnotationConverter(CompilerInfo info, AnnotatableNode node) {
        this.node = node;
        this.info = info;
    }

    @Override
    @NotNull
    public List<Byte> convert(int start) {
        switch (node.getAnnotations().length) {
            case 0:
                return BaseConverter.bytesWithoutAnnotations(start, node, info);
            case 1:
                return convertName(node.getAnnotations()[0], start);
            default:
                throw CompilerTodoError.of("Multiple annotations on one statement", node);
        }
    }

    private List<Byte> convertName(NameNode name, int start) {
        if (name instanceof VariableNode) {
            return convertVariable((VariableNode) name, start);
        } else if (name instanceof FunctionCallNode) {
            return convertFunction((FunctionCallNode) name, start);
        } else {
            throw CompilerTodoError.of("Other annotations", name);
        }
    }

    private List<Byte> convertVariable(VariableNode name, int start) {
        switch (name.getName()) {
            case "cfg":
            case "allow":
            case "deny":
                throw CompilerException.format("'%s' attributes require arguments", name, name.getName());
            case "hot":
            case "cold":
            case "inline":
                if (!(node instanceof DefinitionNode)) {
                    throw CompilerException.of("Frequency hints may only be used on definitions", node);
                }
                CompilerWarning.warn("Frequency hints do not do anything yet", WarningType.TODO, info, name);
                return BaseConverter.bytesWithoutAnnotations(start, node, info);
            case "test":
                CompilerWarning.warn("Test mode is always turned off for now", WarningType.TODO, info, name);
                return convertIfTest(start, false);
            case "notTest":
                CompilerWarning.warn("Test mode is always turned off for now", WarningType.TODO, info, name);
                return convertIfTest(start, true);
            case "nonExhaustive":
                if (node instanceof EnumDefinitionNode || node instanceof UnionDefinitionNode) {
                    CompilerWarning.warn(
                            "Non-exhaustive enums/unions are not yet supported", WarningType.TODO, info, name
                    );
                    return BaseConverter.bytesWithoutAnnotations(start, node, info);
                } else {
                    throw CompilerException.of("$nonExhaustive is only valid on enums and unions", name);
                }
            case "deprecated":
                if (node instanceof FunctionDefinitionNode) {
                    return new FunctionDefinitionConverter(info, (FunctionDefinitionNode) node).convertDeprecated();
                } else {
                    CompilerWarning.warn("Deprecation notices not yet implemented", WarningType.TODO, info, name);
                    return BaseConverter.bytesWithoutAnnotations(start, node, info);
                }
            case "native":
                throw CompilerTodoError.of("'native' annotation", node);
            default:
                throw CompilerException.format("Unknown annotation '%s'", name, name.getName());
        }
    }

    private List<Byte> convertIfTest(int start, boolean convert) {
        if (convert) {
            return BaseConverter.bytesWithoutAnnotations(start, node, info);
        } else {
            return Collections.emptyList();
        }
    }

    private List<Byte> convertFunction(FunctionCallNode name, int start) {
        switch (name.getVariable().getName()) {
            case "cfg":
                return convertCfg(start, name);
            case "inline":
                return convertInline(start, name);
            case "deprecated":
                CompilerWarning.warn("Deprecation notices not yet implemented", WarningType.TODO, info, name);
                return BaseConverter.bytesWithoutAnnotations(start, node, info);
            case "allow":
            case "deny":
                var warningHolder = info.warningHolder();
                changeWarnings(name, warningHolder);
                var bytes = BaseConverter.bytesWithoutAnnotations(start, node, info);
                warningHolder.popWarnings();
                return bytes;
            case "builtin":
                if (!info.permissions().isBuiltin()) {
                    throw CompilerException.of("'builtin' is an internal-only annotation", name);
                } else if (node instanceof BaseClassNode || node instanceof FunctionDefinitionNode) {
                    return Collections.emptyList();
                } else {
                    throw CompilerException.of(
                            "'builtin' annotation is only valid on class and function definitions", node
                    );
                }
            case "native":
                var native0 = name.getParameters()[0].getArgument();
                if (native0 instanceof StringNode && ((StringNode) native0).getContents().equals("sys")) {
                    if (node instanceof FunctionDefinitionNode) {
                        return new FunctionDefinitionConverter(info, (FunctionDefinitionNode) node).convertSys();
                    } else {
                        throw CompilerTodoError.of("'sys' annotation on non-functions", node);
                    }
                } else {
                    throw CompilerException.of("Unknown native function type", name);
                }
            case "derive":
                throw CompilerTodoError.of("'derive' annotation is unimplemented", name);
            default:
                throw CompilerException.format("Unknown annotation '%s'", name, name.getVariable().getName());
        }
    }

    private List<Byte> convertInline(int start, FunctionCallNode inline) {
        assert inline.getVariable().getName().equals("inline");
        if (!(node instanceof DefinitionNode)) {
            throw CompilerException.of("Frequency hints may only be used on definitions", node);
        }
        var parameters = inline.getParameters();
        if (parameters.length == 1) {
            var param = parameters[0];
            var argument = param.getArgument();
            if (!param.isVararg() && param.getVariable().isEmpty() && argument instanceof VariableNode) {
                switch (((VariableNode) argument).getName()) {
                    case "always":
                    case "never":
                        CompilerWarning.warn(
                                "Frequency hints do not do anything yet", WarningType.TODO, info, inline
                        );
                        return BaseConverter.bytesWithoutAnnotations(start, node, info);
                }
            }
        }
        throw CompilerException.of(
                "Invalid format for inline attribute:\n" +
                        "The only valid inlines are $inline, $inline(always), and $inline(never)",
                inline
        );
    }

    private List<Byte> convertCfg(int start, FunctionCallNode inline) {
        assert inline.getVariable().getName().equals("cfg");
        var parameters = inline.getParameters();
        if (parameters.length == 1) {
            var param = parameters[0];
            var argument = param.getArgument();
            if (!param.isVararg() && param.getVariable().isEmpty()) {
                return convertIfTest(start, cfgValue(argument));
            } else {
                throw CompilerException.of("'cfg' annotations do not support named arguments", inline);
            }
        } else {
            throw CompilerException.of("'cfg' annotations only support one value", inline);
        }
    }

    private static boolean cfgValue(TestNode value) {
        if (value instanceof VariableNode) {
            switch (((VariableNode) value).getName()) {
                case "true":
                    return true;
                case "false":
                    return false;
                default:
                    throw CompilerTodoError.of("Unknown cfg value: only true/false allowed so far", value);
            }
        } else if (value instanceof OperatorNode) {
            return cfgBool((OperatorNode) value);
        } else {
            throw CompilerTodoError.of("Cfg with non-variables not supported", value);
        }
    }

    private static boolean cfgBool(OperatorNode value) {
        var operands = value.getOperands();
        switch (value.getOperator()) {
            case BOOL_NOT:
                assert operands.length == 1;
                return !cfgValue(operands[0].getArgument());
            case BOOL_AND:
                for (var op : operands) {
                    if (!cfgValue(op.getArgument())) {
                        return false;
                    }
                }
                return true;
            case BOOL_OR:
                for (var op : operands) {
                    if (cfgValue(op.getArgument())) {
                        return true;
                    }
                }
                return false;
            case BOOL_XOR:
                assert operands.length > 0;
                var result = cfgValue(operands[0].getArgument());
                for (int i = 1; i < operands.length; i++) {
                    result ^= cfgValue(operands[i].getArgument());
                }
                return result;
            default:
                throw CompilerException.of("Non-boolean operands not supported in cfg", value);
        }
    }

    public static boolean shouldCompile(Lined lineInfo, CompilerInfo info, NameNode... annotations) {
        switch (annotations.length) {
            case 0:
                return true;
            case 1:
                return shouldCompileSingle(info, annotations[0]);
            default:
                throw CompilerTodoError.of("Multiple annotations on one statement", lineInfo);
        }
    }

    public static Optional<Pair<String, Integer>> isBuiltin(Lined lineInfo, CompilerInfo info, NameNode... annotations) {
        switch (annotations.length) {
            case 0:
                return Optional.empty();
            case 1:
                return builtinValues(lineInfo, annotations[0], info);
            default:
                throw CompilerTodoError.of("Multiple annotations on one statement", lineInfo);
        }
    }

    private static boolean shouldCompileSingle(CompilerInfo info, NameNode annotation) {
        if (annotation instanceof FunctionCallNode) {
            var stmt = (FunctionCallNode) annotation;
            switch (stmt.getVariable().getName()) {
                case "test":
                    CompilerWarning.warn("Test mode is always turned off for now", WarningType.TODO, info, stmt);
                    return false;
                case "notTest":
                    CompilerWarning.warn("Test mode is always turned off for now", WarningType.TODO, info, stmt);
                    return true;
                case "cfg":
                    if (stmt.getParameters().length == 1) {
                        return cfgValue(stmt.getParameters()[0].getArgument());
                    } else {
                        return true;
                    }
                default:
                    return true;
            }
        } else {
            return true;
        }
    }

    private static void changeWarnings(FunctionCallNode annotation, WarningHolder warningHolder) {
        var name = annotation.getVariable().getName();
        assert name.equals("allow") || name.equals("deny");
        Set<WarningType> allowedTypes = EnumSet.noneOf(WarningType.class);
        for (var param : annotation.getParameters()) {
            if (!param.getVariable().isEmpty() || !param.getVararg().isEmpty()) {
                throw CompilerException.format("Illegal format for %s annotation", annotation, name);
            }
            var argument = param.getArgument();
            if (!(argument instanceof VariableNode)) {
                throw CompilerException.format("Illegal format for %s annotation", annotation, name);
            }
            var argName = ((VariableNode) argument).getName();
            switch (argName) {
                case "all":
                    warnAll(name, warningHolder, annotation);
                    return;
                case "deprecated":
                    addWarning(WarningType.DEPRECATED, allowedTypes, annotation, warningHolder);
                    break;
                case "unused":
                    addWarning(WarningType.UNUSED, allowedTypes, annotation, warningHolder);
                    break;
                case "trivial":
                    addWarning(WarningType.TRIVIAL_VALUE, allowedTypes, annotation, warningHolder);
                    break;
                case "unreachable":
                    addWarning(WarningType.UNREACHABLE, allowedTypes, annotation, warningHolder);
                    break;
                case "infinite":
                    addWarning(WarningType.INFINITE_LOOP, allowedTypes, annotation, warningHolder);
                    break;
                default:
                    throw CompilerException.format("Unknown warning type %s", annotation, argName);
            }
        }
        switch (name) {
            case "allow":
                warningHolder.allow(allowedTypes.toArray(new WarningType[0]));
                break;
            case "deny":
                warningHolder.deny(allowedTypes.toArray(new WarningType[0]));
                break;
            default:
                throw CompilerInternalError.format("Expected 'allow' or 'deny' for name, got %s", annotation, name);
        }
    }

    private static void warnAll(String name, WarningHolder warningHolder, Lined annotation) {
        switch (name) {
            case "allow":
                warningHolder.allowAll();
                break;
            case "deny":
                warningHolder.denyAll();
                break;
            default:
                throw CompilerInternalError.format("Expected 'allow' or 'deny' for name, got %s", annotation, name);
        }
    }

    private static Optional<Pair<String, Integer>> builtinValues(Lined lineInfo, NameNode annotation, CompilerInfo info) {
        if (!(annotation instanceof FunctionCallNode)) {
            return Optional.empty();
        }
        var func = (FunctionCallNode) annotation;
        if (!func.getVariable().getName().equals("builtin")) {
            return Optional.empty();
        }
        if (!info.permissions().isBuiltin()) {
            throw CompilerException.of("'builtin' is an internal-only annotation", lineInfo);
        }
        switch (func.getParameters().length) {
            case 0:
                throw CompilerException.of("'builtin' annotation must have at least 1 parameter", lineInfo);
            case 1:
                var param = func.getParameters()[0];
                if (!(param.getArgument() instanceof StringNode)) {
                    throw CompilerException.of("Ill-formed 'builtin' annotation", lineInfo);
                }
                var argument = (StringNode) param.getArgument();
                return Optional.of(Pair.of(argument.getContents(), -1));
            case 2:
                var param1 = func.getParameters()[0];
                if (!(param1.getArgument() instanceof StringNode)) {
                    throw CompilerException.of("Ill-formed 'builtin' annotation", lineInfo);
                }
                var argument1 = (StringNode) param1.getArgument();
                var param2 = func.getParameters()[1];
                if (!(param2.getArgument() instanceof NumberNode)) {
                    throw CompilerException.of("Ill-formed 'builtin' annotation", lineInfo);
                }
                var argument2 = (NumberNode) param2.getArgument();
                return Optional.of(Pair.of(argument1.getContents(),
                        argument2.getValue().toBigIntegerExact().intValueExact()));
            default:
                throw CompilerException.of("Ill-formed 'builtin' annotation", lineInfo);
        }
    }

    private static void addWarning(WarningType type, Set<WarningType> values, Lined lined, WarningHolder holder) {
        if (!values.add(type)) {
            CompilerWarning.warn("Duplicated allow lint for warnings", WarningType.UNUSED, holder, lined);
        }
    }
}
