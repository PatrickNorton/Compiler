package main.java.converter;

import main.java.parser.AnnotatableNode;
import main.java.parser.DefinitionNode;
import main.java.parser.FunctionCallNode;
import main.java.parser.NameNode;
import main.java.parser.VariableNode;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

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
                throw CompilerException.of("'cfg' attributes require arguments", name);
            case "hot":
            case "cold":
            case "inline":
                if (!(node instanceof DefinitionNode)) {
                    throw CompilerException.of("Frequency hints may only be used on definitions", node);
                }
                CompilerWarning.warn("Frequency hints do not do anything yet", name);
                return BaseConverter.bytesWithoutAnnotations(start, node, info);
            case "test":
                CompilerWarning.warn("Test mode is always turned off for now", name);
                return convertIfTest(start, false);
            case "notTest":
                CompilerWarning.warn("Test mode is always turned off for now", name);
                return convertIfTest(start, true);
            case "deprecated":
                CompilerWarning.warn("Deprecation notices not yet implemented", name);
                return BaseConverter.bytesWithoutAnnotations(start, node, info);
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
                throw CompilerTodoError.of("'cfg' attributes not implemented yet", name);
            case "inline":
                return convertInline(start, name);
            case "deprecated":
                CompilerWarning.warn("Deprecation notices not yet implemented", name);
                return BaseConverter.bytesWithoutAnnotations(start, node, info);
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
                        CompilerWarning.warn("Frequency hints do not do anything yet", inline);
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
}
