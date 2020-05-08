package main.java.converter;

import main.java.converter.classbody.AttributeConverter;
import main.java.converter.classbody.MethodConverter;
import main.java.converter.classbody.MethodInfo;
import main.java.converter.classbody.OperatorDefConverter;
import main.java.converter.classbody.PropertyConverter;
import main.java.parser.BaseClassNode;
import main.java.parser.DeclarationNode;
import main.java.parser.DeclaredAssignmentNode;
import main.java.parser.DescriptorNode;
import main.java.parser.Lined;
import main.java.parser.MethodDefinitionNode;
import main.java.parser.OperatorDefinitionNode;
import main.java.parser.PropertyDefinitionNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ClassConverterBase<T extends BaseClassNode> {
    protected final CompilerInfo info;
    protected final T node;

    public ClassConverterBase(CompilerInfo info, T node) {
        this.info = info;
        this.node = node;
    }

    @NotNull
    protected final <U> Map<U, List<Byte>> convert(StdTypeObject type, @NotNull Map<U, MethodInfo> functions) {
        Map<U, List<Byte>> result = new HashMap<>();
        for (var pair : functions.entrySet()) {
            var methodInfo = pair.getValue();
            var isConstMethod = !methodInfo.getDescriptors().contains(DescriptorNode.MUT);
            info.addStackFrame();
            info.addVariable("self", isConstMethod ? type.makeConst() : type.makeMut(), isConstMethod, node);
            info.addVariable("cls", Builtins.TYPE.generify(type), true, node);
            try {
                info.allowPrivateAccess(type);
                recursivelyAllowProtectedAccess(type);
                var fnInfo = methodInfo.getInfo();
                for (var arg : fnInfo.getArgs()) {
                    info.addVariable(arg.getName(), arg.getType(), methodInfo);
                }
                info.addFunctionReturns(fnInfo.getReturns());
                var bytes = BaseConverter.bytes(0, methodInfo.getBody(), info);
                info.popFnReturns();
                result.put(pair.getKey(), bytes);
                info.removeStackFrame();
            } finally {
                info.removePrivateAccess(type);
                recursivelyRemovePrivateAccess(type);
            }
        }
        return result;
    }

    private void recursivelyAllowProtectedAccess(@NotNull StdTypeObject type) {
        for (var superCls : type.getSupers()) {
            info.allowProtectedAccess(superCls);
            if (superCls instanceof StdTypeObject) {
                recursivelyAllowProtectedAccess((StdTypeObject) superCls);
            }
        }
    }

    private void recursivelyRemovePrivateAccess(@NotNull StdTypeObject type) {
        for (var superCls : type.getSupers()) {
            info.removeProtectedAccess(superCls);
            if (superCls instanceof StdTypeObject) {
                recursivelyRemovePrivateAccess((StdTypeObject) superCls);
            }
        }
    }

    protected final StdTypeObject[] convertSupers(TypeObject[] supers) {
        try {
            return Arrays.copyOf(supers, supers.length, StdTypeObject[].class);
        } catch (ArrayStoreException e) {
            throw CompilerException.format(
                    "Class '%s' inherits from a non-standard type",
                    node, node.getName()
            );
        }
    }

    protected final void ensureProperInheritance(StdTypeObject type, @NotNull StdTypeObject... supers) {
        for (var superCls : supers) {
            if (superCls.isFinal()) {
                throw CompilerException.format(
                        "Class '%s' inherits from class '%s', which is not marked 'nonfinal'",
                        node, type.name(), superCls.name()
                );
            }
        }
    }

    protected final void parseStatements(
            AttributeConverter declarations,
            MethodConverter methods,
            OperatorDefConverter operators,
            PropertyConverter properties
    ) {
        for (var stmt : node.getBody()) {
            if (stmt instanceof DeclarationNode) {
                declarations.parse((DeclarationNode) stmt);
            } else if (stmt instanceof DeclaredAssignmentNode) {
                declarations.parse((DeclaredAssignmentNode) stmt);
            } else if (stmt instanceof MethodDefinitionNode) {
                methods.parse((MethodDefinitionNode) stmt);
            } else if (stmt instanceof OperatorDefinitionNode) {
                operators.parse((OperatorDefinitionNode) stmt);
            } else if (stmt instanceof PropertyDefinitionNode) {
                properties.parse((PropertyDefinitionNode) stmt);
            } else {
                throw new UnsupportedOperationException("Node not yet supported");
            }
        }
    }

    protected final void checkAttributes(@NotNull Map<String, AttributeInfo> vars, Map<String, AttributeInfo> staticVars,
                                 Map<String, MethodInfo> methods, Map<String, MethodInfo> staticMethods) {
        checkMaps(vars, methods, staticMethods);
        checkMaps(staticVars, methods, staticMethods);
        checkMaps(methods, vars, staticVars);
        checkMaps(staticMethods, vars, staticVars);
    }

    private void checkMaps(@NotNull Map<String, ? extends Lined> vars, Map<String, ? extends Lined> methods,
                           Map<String, ? extends Lined> staticMethods) {
        for (var pair : vars.entrySet()) {
            if (methods.containsKey(pair.getKey())) {
                throw CompilerException.doubleDef(
                        pair.getKey(),
                        pair.getValue(),
                        methods.get(pair.getKey())
                );
            } else if (staticMethods.containsKey(pair.getKey())) {
                throw CompilerException.doubleDef(
                        pair.getKey(),
                        pair.getValue(),
                        staticMethods.get(pair.getKey())
                );
            }
        }
    }

    @NotNull
    protected final Map<String, AttributeInfo> allAttributes(Map<String, AttributeInfo> attrs,
                                                  @NotNull Map<String, MethodInfo> methods,
                                                  Map<String, AttributeInfo> properties) {
        var finalAttrs = new HashMap<>(attrs);
        for (var pair : methods.entrySet()) {
            var methodInfo = pair.getValue();
            var attrInfo = new AttributeInfo(methodInfo.getDescriptors(), methodInfo.getInfo().toCallable());
            finalAttrs.put(pair.getKey(), attrInfo);
        }
        finalAttrs.putAll(properties);
        return finalAttrs;
    }

    @NotNull
    protected static <T, U> Map<T, Pair<U, U>> merge(@NotNull Map<T, U> first, @NotNull Map<T, U> second) {
        assert first.size() == second.size();
        Map<T, Pair<U, U>> result = new HashMap<>(first.size());
        for (var pair : first.entrySet()) {
            var key = pair.getKey();
            assert second.containsKey(key);
            result.put(key, Pair.of(pair.getValue(), second.get(key)));
        }
        return result;
    }
}
