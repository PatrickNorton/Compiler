package main.java.converter;

import main.java.converter.classbody.ClassDeclarationConverter;
import main.java.converter.classbody.MethodConverter;
import main.java.converter.classbody.MethodInfo;
import main.java.converter.classbody.OperatorDefConverter;
import main.java.converter.classbody.PropertyConverter;
import main.java.parser.ClassDefinitionNode;
import main.java.parser.DeclarationNode;
import main.java.parser.DeclaredAssignmentNode;
import main.java.parser.DescriptorNode;
import main.java.parser.Lined;
import main.java.parser.MethodDefinitionNode;
import main.java.parser.OperatorDefinitionNode;
import main.java.parser.PropertyDefinitionNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClassConverter implements BaseConverter {
    private final ClassDefinitionNode node;
    private final CompilerInfo info;

    public ClassConverter(CompilerInfo info, ClassDefinitionNode node) {
        this.node = node;
        this.info = info;
    }

    @NotNull
    @Override
    @Unmodifiable
    public List<Byte> convert(int start) {
        var supers = info.typesOf(node.getSuperclasses());
        var declarations = new ClassDeclarationConverter(info);
        var methods = new MethodConverter(info);
        var operators = new OperatorDefConverter(info);
        var properties = new PropertyConverter(info);
        var trueSupers = convertSupers(supers);
        var generics = GenericInfo.parse(info, node.getName().getSubtypes());
        var descriptors = node.getDescriptors();
        var isFinal = !descriptors.contains(DescriptorNode.NONFINAL);
        StdTypeObject type;
        if (!info.hasType(node.strName())) {
            type = new StdTypeObject(node.getName().strName(), List.of(trueSupers), generics, isFinal);
            ensureProperInheritance(type, trueSupers);
            info.addType(type);
            parseStatements(declarations, methods, operators, properties);
        } else {
            type = (StdTypeObject) info.getType(node.strName());
        }
        if (type.isFinal() && classIsConstant(declarations, methods, operators, properties)) {
            type.isConstClass();
        }
        type.setOperators(operators.getOperatorInfos());
        List<Short> superConstants = new ArrayList<>();
        for (var sup : trueSupers) {
            superConstants.add(info.constIndex(sup.name()));
        }
        checkAttributes(declarations.getVars(), declarations.getStaticVars(),
                methods.getMethods(), methods.getStaticMethods());
        type.setAttributes(allAttributes(declarations.getVars(), methods.getMethods(), properties.getProperties()));
        type.setStaticAttributes(allAttributes(declarations.getStaticVars(), methods.getStaticMethods(), new HashMap<>()));
        var cls = new ClassInfo.Factory()
                .setType(type)
                .setSuperConstants(superConstants)
                .setVariables(declarations.varsWithInts())
                .setStaticVariables(declarations.staticVarsWithInts())
                .setOperatorDefs(convert(type, operators.getOperators()))
                .setStaticOperators(new HashMap<>())
                .setMethodDefs(convert(type, methods.getMethods()))
                .setStaticMethods(convert(type, methods.getStaticMethods()))
                .setProperties(merge(convert(type, properties.getGetters()), convert(type, properties.getSetters())))
                .create();
        int classIndex = info.addClass(cls);
        var name = node.getName().strName();
        if (Builtins.FORBIDDEN_NAMES.contains(name)) {
            throw CompilerException.format("Illegal name for class '%s'", node.getName(), name);
        }
        info.checkDefinition(name, node);
        info.addVariable(name, Builtins.TYPE.generify(type), new ClassConstant(name, classIndex), node);
        return Collections.emptyList();
    }

    private StdTypeObject[] convertSupers(TypeObject[] supers) {
        try {
            return Arrays.copyOf(supers, supers.length, StdTypeObject[].class);
        } catch (ArrayStoreException e) {
            throw CompilerException.format(
                    "Class '%s' inherits from a non-standard type",
                    node, node.getName()
            );
        }
    }

    private void ensureProperInheritance(StdTypeObject type, @NotNull StdTypeObject... supers) {
        for (var superCls : supers) {
            if (superCls.isFinal()) {
                throw CompilerException.format(
                        "Class '%s' inherits from class '%s', which is not marked 'nonfinal'",
                        node, type.name(), superCls.name()
                );
            }
        }
    }

    @NotNull
    private <T> Map<T, List<Byte>> convert(StdTypeObject type, @NotNull Map<T, MethodInfo> functions) {
        Map<T, List<Byte>> result = new HashMap<>();
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

    private static boolean classIsConstant(@NotNull ClassDeclarationConverter decls, @NotNull MethodConverter methods,
                                    @NotNull OperatorDefConverter ops, @NotNull PropertyConverter props) {
        for (var info : decls.getVars().values()) {
            if (info.getDescriptors().contains(DescriptorNode.MUT)) {
                return false;
            }
        }
        for (var info : methods.getMethods().values()) {
            if (info.getDescriptors().contains(DescriptorNode.MUT)) {
                return false;
            }
        }
        for (var info : ops.getOperators().values()) {
            if (info.getDescriptors().contains(DescriptorNode.MUT)) {
                return false;
            }
        }
        for (var info : props.getProperties().values()) {
            if (info.getDescriptors().contains(DescriptorNode.MUT)) {
                return false;
            }
        }
        return true;
    }

    @NotNull
    private static <T, U> Map<T, Pair<U, U>> merge(@NotNull Map<T, U> first, @NotNull Map<T, U> second) {
        assert first.size() == second.size();
        Map<T, Pair<U, U>> result = new HashMap<>(first.size());
        for (var pair : first.entrySet()) {
            var key = pair.getKey();
            assert second.containsKey(key);
            result.put(key, Pair.of(pair.getValue(), second.get(key)));
        }
        return result;
    }

    @NotNull
    private Map<String, AttributeInfo> allAttributes(Map<String, AttributeInfo> attrs,
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

    private void checkAttributes(@NotNull Map<String, AttributeInfo> vars, Map<String, AttributeInfo> staticVars,
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

    public static void completeType(CompilerInfo info, ClassDefinitionNode node, StdTypeObject obj) {
        new ClassConverter(info, node).completeType(obj);
    }

    private void completeType(@NotNull StdTypeObject obj) {
        var declarations = new ClassDeclarationConverter(info);
        var methods = new MethodConverter(info);
        var operators = new OperatorDefConverter(info);
        var properties = new PropertyConverter(info);
        parseStatements(declarations, methods, operators, properties);
        if (obj.isFinal() && classIsConstant(declarations, methods, operators, properties)) {
            obj.isConstClass();
        }
        obj.setOperators(operators.getOperatorInfos());
        checkAttributes(declarations.getVars(), declarations.getStaticVars(),
                methods.getMethods(), methods.getStaticMethods());
        obj.setAttributes(allAttributes(declarations.getVars(), methods.getMethods(), properties.getProperties()));
        obj.seal();
    }

    private void parseStatements(
            ClassDeclarationConverter declarations,
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
}
