package main.java.converter;

import main.java.parser.FunctionCallNode;
import main.java.parser.OperatorNode;
import main.java.parser.StringNode;
import main.java.parser.TestNode;
import main.java.parser.VariableNode;

public final class CfgConverter {
    private final CompilerInfo info;
    private final FunctionCallNode node;

    public CfgConverter(CompilerInfo info, FunctionCallNode node) {
        this.info = info;
        this.node = node;
    }

    public boolean convert() {
        assert node.getVariable().getName().equals("cfg");
        var parameters = node.getParameters();
        if (parameters.length == 1) {
            var param = parameters[0];
            var argument = param.getArgument();
            if (!param.isVararg() && param.getVariable().isEmpty()) {
                return valueOf(argument);
            } else {
                throw CompilerException.of("'cfg' annotations do not support named arguments", node);
            }
        } else {
            throw CompilerException.of("'cfg' annotations only support one value", node);
        }
    }

    private boolean valueOf(TestNode value) {
        if (value instanceof VariableNode) {
            switch (((VariableNode) value).getName()) {
                case "true":
                    return true;
                case "false":
                    return false;
                case "test":
                    return info.globalInfo().isTest();
                case "debug":
                    return info.globalInfo().isDebug();
                default:
                    throw CompilerTodoError.of("Unknown cfg value: only true/false allowed so far", value);
            }
        } else if (value instanceof FunctionCallNode) {
            var name = ((FunctionCallNode) value).getVariable().getName();
            switch (name) {
                case "feature":
                    return convertFeature((FunctionCallNode) value);
                case "version":
                    return convertVersion((FunctionCallNode) value);
                case "all":
                    return convertAll((FunctionCallNode) value);
                case "any":
                    return convertAny((FunctionCallNode) value);
                default:
                    throw CompilerException.format("Unknown cfg function predicate %s", value, name);
            }
        } else if (value instanceof OperatorNode) {
            return convertBool((OperatorNode) value);
        } else {
            throw CompilerTodoError.of("Cfg with non-variables not supported", value);
        }
    }

    private boolean convertBool(OperatorNode value) {
        var operands = value.getOperands();
        switch (value.getOperator()) {
            case BOOL_NOT:
                assert operands.length == 1;
                return !valueOf(operands[0].getArgument());
            case BOOL_AND:
                for (var op : operands) {
                    if (!valueOf(op.getArgument())) {
                        return false;
                    }
                }
                return true;
            case BOOL_OR:
                for (var op : operands) {
                    if (valueOf(op.getArgument())) {
                        return true;
                    }
                }
                return false;
            case BOOL_XOR:
                assert operands.length > 0;
                var result = valueOf(operands[0].getArgument());
                for (int i = 1; i < operands.length; i++) {
                    result ^= valueOf(operands[i].getArgument());
                }
                return result;
            default:
                throw CompilerException.of("Non-boolean operands not supported in cfg", value);
        }
    }

    private boolean convertVersion(FunctionCallNode value) {
        assert value.getVariable().getName().equals("version");
        var args = value.getParameters();
        if (args.length != 1) {
            throw CompilerException.of("Invalid format for 'version' cfg attribute", value);
        }
        var arg = args[0];
        var strValue = getString(arg.getArgument());
        var version = Version.parse(strValue);
        if (version == null) {
            throw CompilerException.of("Invalid version format", arg);
        } else {
            return version.compareTo(Builtins.CURRENT_VERSION) <= 0;
        }
    }

    private boolean convertFeature(FunctionCallNode value) {
        assert value.getVariable().getName().equals("feature");
        var args = value.getParameters();
        if (args.length != 1) {
            throw CompilerException.of("Invalid format for 'feature' cfg attribute", value);
        }
        var arg = args[0];
        var strValue = getString(arg.getArgument());
        return Builtins.STABLE_FEATURES.contains(strValue);
    }

    private boolean convertAll(FunctionCallNode value) {
        assert value.getVariable().getName().equals("all");
        var args = value.getParameters();
        for (var arg : args) {
            if (arg.isVararg() || !arg.getVariable().isEmpty()) {
                throw CompilerException.of("Invalid format for cfg(all)", value);
            }
            if (!valueOf(arg.getArgument())) {
                return false;
            }
        }
        return true;
    }

    private boolean convertAny(FunctionCallNode value) {
        assert value.getVariable().getName().equals("any");
        var args = value.getParameters();
        for (var arg : args) {
            if (arg.isVararg() || !arg.getVariable().isEmpty()) {
                throw CompilerException.of("Invalid format for cfg(any)", value);
            }
            if (valueOf(arg.getArgument())) {
                return true;
            }
        }
        return false;
    }

    private static String getString(TestNode value) {
        if (value instanceof StringNode) {
            return ((StringNode) value).getContents();
        } else {
            throw CompilerException.of("Expected string literal here", value);
        }
    }

    public static boolean valueOf(TestNode value, CompilerInfo info) {
        // 'null' is reasonable here because convert() is the only function that
        // uses this.node (and we don't expect that to change with further
        // additions).
        return new CfgConverter(info, null).valueOf(value);
    }
}
