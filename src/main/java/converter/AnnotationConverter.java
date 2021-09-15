package main.java.converter;

import main.java.parser.AnnotatableNode;
import main.java.parser.ArgumentNode;
import main.java.parser.BaseClassNode;
import main.java.parser.DefinitionNode;
import main.java.parser.EnumDefinitionNode;
import main.java.parser.EscapedOperatorNode;
import main.java.parser.FunctionCallNode;
import main.java.parser.FunctionDefinitionNode;
import main.java.parser.Lined;
import main.java.parser.MethodDefinitionNode;
import main.java.parser.NameNode;
import main.java.parser.NumberNode;
import main.java.parser.OpSpTypeNode;
import main.java.parser.StringNode;
import main.java.parser.TestNode;
import main.java.parser.UnionDefinitionNode;
import main.java.parser.VariableNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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
    public BytecodeList convert() {
        return switch (node.getAnnotations().length) {
            case 0 -> BaseConverter.bytesWithoutAnnotations(node, info);
            case 1 -> convertName(node.getAnnotations()[0]);
            default -> throw CompilerTodoError.of("Multiple annotations on one statement", node);
        };
    }

    private BytecodeList convertName(NameNode name) {
        if (name instanceof VariableNode) {
            return convertVariable((VariableNode) name);
        } else if (name instanceof FunctionCallNode) {
            return convertFunction((FunctionCallNode) name);
        } else {
            throw CompilerTodoError.of("Other annotations", name);
        }
    }

    private BytecodeList convertVariable(@NotNull VariableNode name) {
        switch (name.getName()) {
            case "cfg":
            case "allow":
            case "deny":
            case "forbid":
                throw CompilerException.format("'%s' attributes require arguments", name, name.getName());
            case "hot":
            case "cold":
            case "inline":
                if (!(node instanceof DefinitionNode)) {
                    throw CompilerException.of("Frequency hints may only be used on definitions", node);
                }
                CompilerWarning.warn("Frequency hints do not do anything yet", WarningType.TODO, info, name);
                return BaseConverter.bytesWithoutAnnotations(node, info);
            case "test":
                if (node instanceof FunctionDefinitionNode) {
                    TestFnConverter.convertTestFunction(info, (FunctionDefinitionNode) node);
                    return new BytecodeList();
                } else {
                    throw CompilerException.of("Only functions may be used as tests", node);
                }
            case "nonExhaustive":
                if (node instanceof EnumDefinitionNode || node instanceof UnionDefinitionNode) {
                    CompilerWarning.warn(
                            "Non-exhaustive enums/unions are not yet supported", WarningType.TODO, info, name
                    );
                    return BaseConverter.bytesWithoutAnnotations(node, info);
                } else {
                    throw CompilerException.of("$nonExhaustive is only valid on enums and unions", name);
                }
            case "deprecated":
                if (node instanceof FunctionDefinitionNode) {
                    return new FunctionDefinitionConverter(info, (FunctionDefinitionNode) node).convertDeprecated();
                } else {
                    CompilerWarning.warn("Deprecation notices not yet implemented", WarningType.TODO, info, name);
                    return BaseConverter.bytesWithoutAnnotations(node, info);
                }
            case "native":
                throw CompilerTodoError.of("'native' annotation", node);
            case "mustUse":
                if (node instanceof FunctionDefinitionNode) {
                    return new FunctionDefinitionConverter(info, (FunctionDefinitionNode) node).convertMustUse("");
                } else if ( node instanceof MethodDefinitionNode) {
                    CompilerWarning.warn(
                            "'mustUse' annotation does not work on methods", WarningType.TODO, info, node
                    );
                    return BaseConverter.bytesWithoutAnnotations(node, info);
                } else {
                    throw CompilerException.of("'mustUse' annotation is only valid on function definitions", node);
                }
            default:
                throw CompilerException.format("Unknown annotation '%s'", name, name.getName());
        }
    }

    @NotNull
    private BytecodeList convertIfTest(boolean convert) {
        if (convert) {
            return BaseConverter.bytesWithoutAnnotations(node, info);
        } else {
            return new BytecodeList();
        }
    }

    private BytecodeList convertFunction(@NotNull FunctionCallNode name) {
        switch (name.getVariable().getName()) {
            case "cfg":
                return convertIfTest(new CfgConverter(info, name).convert());
            case "inline":
                return convertInline(name);
            case "deprecated":
                CompilerWarning.warn("Deprecation notices not yet implemented", WarningType.TODO, info, name);
                return BaseConverter.bytesWithoutAnnotations(node, info);
            case "allow":
            case "deny":
            case "forbid":
                var warningHolder = info.warningHolder();
                changeWarnings(name, warningHolder);
                var bytes = BaseConverter.bytesWithoutAnnotations(node, info);
                warningHolder.popWarnings();
                return bytes;
            case "builtin":
                if (!info.permissions().isBuiltin()) {
                    throw CompilerException.of("'builtin' is an internal-only annotation", name);
                } else if (node instanceof BaseClassNode || node instanceof FunctionDefinitionNode) {
                    return new BytecodeList();
                } else {
                    throw CompilerException.of(
                            "'builtin' annotation is only valid on class and function definitions", node
                    );
                }
            case "native":
                if (!info.permissions().isStdlib()) {
                    throw CompilerException.of("'native' is an internal-only annotation", name);
                }
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
                if (node instanceof BaseClassNode) {
                    return BaseConverter.bytesWithoutAnnotations(node, info);
                } else {
                    throw CompilerException.of("'derive' annotation only works on class definitions", node);
                }
            case "stable":
            case "unstable":
                if (info.permissions().isStdlib()) {
                    throw CompilerTodoError.of("Stable/unstable annotations", name);
                } else {
                    throw CompilerException.of(
                            "'stable' and 'unstable' annotations are only available for the standard library", node
                    );
                }
            case "feature":
                var features = getStrings(name.getParameters());
                if (info.permissions().isStdlib()) {
                    info.addFeatures(features);
                    var result = BaseConverter.bytesWithoutAnnotations(node, info);
                    info.removeFeatures(features);
                    return result;
                }
                for (var featureName : features) {
                    if (Builtins.STABLE_FEATURES.contains(featureName)) {
                        CompilerWarning.warnf(
                                "Use of '$feature' attribute for stable feature %s",
                                WarningType.NO_TYPE, info, name, featureName
                        );
                    } else {
                        throw CompilerTodoError.of("Proper unstable feature annotations", name);
                    }
                    info.addFeature(featureName);
                }
                var result = BaseConverter.bytesWithoutAnnotations(node, info);
                info.removeFeatures(features);
                return result;
            case "cfgAttr":
                if (name.getParameters().length != 2) {
                    throw CompilerException.format(
                            "'cfgAttr' annotation takes exactly 2 arguments, not %d",
                            name, name.getParameters().length
                    );
                } else {
                    var config = name.getParameters()[0];
                    var attr = name.getParameters()[1];
                    if (!(attr.getArgument() instanceof NameNode)) {
                        throw CompilerException.of("Invalid format for attribute", attr.getArgument());
                    } else if (CfgConverter.valueOf(config.getArgument(), info)) {
                        return convertName((NameNode) attr.getArgument());
                    } else {
                        return BaseConverter.bytesWithoutAnnotations(node, info);
                    }
                }
            case "extern":
                throw CompilerTodoError.of("External functions are not yet implemented", node);
            case "mustUse":
                if (name.getParameters().length != 1) {
                    throw CompilerException.format(
                            "'mustUse' annotation takes exactly 1 argument, not %d",
                            name, name.getParameters().length
                    );
                } else {
                    var useMsg = name.getParameters()[0];
                    if (!(useMsg.getArgument() instanceof StringNode)) {
                        throw CompilerException.of(
                                "'mustUse' annotation argument must be a string literal", name
                        );
                    }
                    var message = ((StringNode) useMsg.getArgument()).getContents();
                    if (node instanceof FunctionDefinitionNode) {
                        return new FunctionDefinitionConverter(info, (FunctionDefinitionNode) node)
                                .convertMustUse(message);
                    } else if (node instanceof MethodDefinitionNode) {
                        CompilerWarning.warn(
                            "'mustUse' annotation does not work on methods", WarningType.TODO, info, node
                    );
                    return BaseConverter.bytesWithoutAnnotations(node, info);
                    } else {
                        throw CompilerException.of(
                                "'mustUse' annotation is only valid on function definitions", node
                        );
                    }
                }
            default:
                throw CompilerException.format("Unknown annotation '%s'", name, name.getVariable().getName());
        }
    }

    @NotNull
    private BytecodeList convertInline(@NotNull FunctionCallNode inline) {
        assert inline.getVariable().getName().equals("inline");
        if (!(node instanceof DefinitionNode)) {
            throw CompilerException.of("Frequency hints may only be used on definitions", node);
        }
        var parameters = inline.getParameters();
        if (parameters.length == 1) {
            var param = parameters[0];
            var argument = param.getArgument();
            if (!param.isVararg() && param.getVariable().isEmpty() && argument instanceof VariableNode arg) {
                switch (arg.getName()) {
                    case "always", "never" -> {
                        CompilerWarning.warn(
                                "Frequency hints do not do anything yet", WarningType.TODO, info, inline
                        );
                        return BaseConverter.bytesWithoutAnnotations(node, info);
                    }
                }
            }
        }
        throw CompilerException.of(
                "Invalid format for inline attribute:\n" +
                        "The only valid inlines are $inline, $inline(always), and $inline(never)",
                inline
        );
    }

    private static String[] getStrings(ArgumentNode... values) {
        var result = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = getString(values[i].getArgument());
        }
        return result;
    }

    private static String getString(TestNode value) {
        if (value instanceof StringNode) {
            return ((StringNode) value).getContents();
        } else {
            throw CompilerException.of("Expected string literal here", value);
        }
    }

    public static boolean shouldCompile(Lined lineInfo, CompilerInfo info, NameNode... annotations) {
        return switch (annotations.length) {
            case 0 -> true;
            case 1 -> shouldCompileSingle(info, annotations[0]);
            default -> throw CompilerTodoError.of("Multiple annotations on one statement", lineInfo);
        };
    }

    public static Optional<BuiltinInfo> isBuiltin(Lined lineInfo, CompilerInfo info, NameNode... annotations) {
        return switch (annotations.length) {
            case 0 -> Optional.empty();
            case 1 -> builtinValues(lineInfo, annotations[0], info);
            default -> throw CompilerTodoError.of("Multiple annotations on one statement", lineInfo);
        };
    }

    private static boolean shouldCompileSingle(CompilerInfo info, NameNode annotation) {
        if (annotation instanceof FunctionCallNode stmt) {
            switch (stmt.getVariable().getName()) {
                case "test":
                    return info.globalInfo().isTest();
                case "cfg":
                    var converter = new CfgConverter(info, stmt);
                    return converter.convert();
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
                case "all" -> {
                    if (annotation.getParameters().length > 1) {
                        throw CompilerException.format(
                                "'all' used in conjunction with other parameters in '%s' statement", annotation, name
                        );
                    }
                    warnAll(name, warningHolder, annotation);
                    return;
                }
                case "deprecated" -> addWarning(WarningType.DEPRECATED, allowedTypes, annotation, warningHolder);
                case "unused" -> addWarning(WarningType.UNUSED, allowedTypes, annotation, warningHolder);
                case "trivial" -> addWarning(WarningType.TRIVIAL_VALUE, allowedTypes, annotation, warningHolder);
                case "unreachable" -> addWarning(WarningType.UNREACHABLE, allowedTypes, annotation, warningHolder);
                case "infinite" -> addWarning(WarningType.INFINITE_LOOP, allowedTypes, annotation, warningHolder);
                case "zero" -> addWarning(WarningType.ZERO_DIVISION, allowedTypes, annotation, warningHolder);
                default -> throw CompilerException.format("Unknown warning type %s", annotation, argName);
            }
        }
        switch (name) {
            case "allow" -> {
                for (var allowed : allowedTypes) {
                    if (warningHolder.isForbidden(allowed)) {
                        throw CompilerException.format("Cannot allow forbidden warning %s", annotation, allowed);
                    }
                }
                warningHolder.allow(allowedTypes.toArray(new WarningType[0]));
            }
            case "deny" -> warningHolder.deny(allowedTypes.toArray(new WarningType[0]));
            case "forbid" -> warningHolder.forbid(allowedTypes.toArray(new WarningType[0]));
            default -> throw CompilerInternalError.format(
                    "Expected 'allow' or 'deny' for name, got %s", annotation, name
            );
        }
    }

    private static void warnAll(String name, WarningHolder warningHolder, Lined annotation) {
        switch (name) {
            case "allow" -> {
                for (var allowed : WarningType.values()) {
                    if (warningHolder.isForbidden(allowed)) {
                        throw CompilerException.format(
                                "Cannot allow forbidden warning %s%nNote: Warning allowed because of allow(all)",
                                annotation, allowed
                        );
                    }
                }
                warningHolder.allowAll();
            }
            case "deny" -> warningHolder.denyAll();
            case "forbid" -> warningHolder.forbidAll();
            default -> throw CompilerInternalError.format(
                    "Expected 'allow' or 'deny' for name, got %s", annotation, name
            );
        }
    }

    private static Optional<BuiltinInfo> builtinValues(Lined lineInfo, NameNode annotation, CompilerInfo info) {
        if (!(annotation instanceof FunctionCallNode func)) {
            return Optional.empty();
        }
        if (!func.getVariable().getName().equals("builtin")) {
            return Optional.empty();
        }
        if (!info.permissions().isBuiltin()) {
            throw CompilerException.of("'builtin' is an internal-only annotation", lineInfo);
        }
        switch (func.getParameters().length) {
            case 0 -> throw CompilerException.of("'builtin' annotation must have at least 1 parameter", lineInfo);
            case 1 -> {
                var param = func.getParameters()[0];
                if (!(param.getArgument() instanceof StringNode argument)) {
                    throw CompilerException.of("Ill-formed 'builtin' annotation", lineInfo);
                }
                return Optional.of(new BuiltinInfo(argument.getContents(), -1));
            }
            case 2 -> {
                var param1 = func.getParameters()[0];
                if (!(param1.getArgument() instanceof StringNode argument1)) {
                    throw CompilerException.of("Ill-formed 'builtin' annotation", lineInfo);
                }
                var param2 = func.getParameters()[1];
                if (!(param2.getArgument() instanceof NumberNode argument2)) {
                    throw CompilerException.of("Ill-formed 'builtin' annotation", lineInfo);
                }
                return Optional.of(new BuiltinInfo(argument1.getContents(),
                        argument2.getValue().toBigIntegerExact().intValueExact()));
            }
            case 3 -> {
                var p1 = func.getParameters()[0];
                if (!(p1.getArgument() instanceof StringNode a1)) {
                    throw CompilerException.of("Ill-formed 'builtin' annotation", lineInfo);
                }
                var p2 = func.getParameters()[1];
                if (!(p2.getArgument() instanceof NumberNode a2)) {
                    throw CompilerException.of("Ill-formed 'builtin' annotation", lineInfo);
                }
                var p3 = func.getParameters()[2];
                if (!(p3.getArgument() instanceof VariableNode a3)) {
                    throw CompilerException.of("Ill-formed 'builtin' annotation", lineInfo);
                }
                if (!a3.getName().equals("hidden")) {
                    throw CompilerException.of("Ill-formed 'builtin' annotation", lineInfo);
                }
                return Optional.of(
                        new BuiltinInfo(a1.getContents(), a2.getValue().toBigIntegerExact().intValueExact(), true)
                );
            }
            default -> throw CompilerException.format(
                    "Ill-formed 'builtin' annotation (got %d parameters)", lineInfo, func.getParameters().length
            );
        }
    }

    private static void addWarning(WarningType type, Set<WarningType> values, Lined lined, WarningHolder holder) {
        if (!values.add(type)) {
            CompilerWarning.warn("Duplicated allow lint for warnings", WarningType.UNUSED, holder, lined);
        }
    }

    public static List<OpSpTypeNode> deriveOperators(NameNode[] nodes) {
        for (var node : nodes) {
            if (node instanceof FunctionCallNode
                    && ((FunctionCallNode) node).getVariable().getName().equals("derive")) {
                return deriveOperator((FunctionCallNode) node);
            }
        }
        return Collections.emptyList();
    }

    private static List<OpSpTypeNode> deriveOperator(FunctionCallNode node) {
        assert node.getVariable().getName().equals("derive");
        var args = node.getParameters();
        List<OpSpTypeNode> operators = new ArrayList<>(args.length);
        for (var arg : args) {
            if (arg.isVararg()) {
                throw CompilerException.of("Varargs are not allowed in 'derive' attributes", node);
            } else {
                operators.add(deriveOperator(arg.getArgument(), node));
            }
        }
        return operators;
    }

    private static OpSpTypeNode deriveOperator(TestNode op, Lined node) {
        if (op instanceof EscapedOperatorNode escaped) {
            return switch (escaped.getOperator()) {
                case EQUALS -> OpSpTypeNode.EQUALS;
                case COMPARE -> OpSpTypeNode.COMPARE;
                default -> throw invalidOpException(node);
            };
        } else if (op instanceof VariableNode variable) {
            return switch (variable.getName()) {
                case "hash" -> OpSpTypeNode.HASH;
                case "repr" -> OpSpTypeNode.REPR;
                default -> throw invalidOpException(node);
            };
        } else {
            throw CompilerException.of(
                    "Invalid derived operator: " +
                            "Only escaped operators and references are valid in a derive statement",
                    node
            );
        }
    }

    private static CompilerException invalidOpException(Lined node) {
        throw CompilerException.of(
                "Invalid derived operator: Can only derive ==, <=>, repr, and hash operators", node
        );
    }
}
