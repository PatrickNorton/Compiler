package main.java.converter;

import main.java.parser.ClassDefinitionNode;
import main.java.parser.DeclarationNode;
import main.java.parser.DeclaredAssignmentNode;
import main.java.parser.DescriptorNode;
import main.java.parser.LineInfo;
import main.java.parser.Lined;
import main.java.parser.MethodDefinitionNode;
import main.java.parser.OpSpTypeNode;
import main.java.parser.OperatorDefinitionNode;
import main.java.parser.PropertyDefinitionNode;
import main.java.parser.StatementBodyNode;
import main.java.parser.VariableNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        var declarations = new DeclarationConverter(info);
        var methods = new MethodConverter(info);
        var operators = new OperatorConverter(info);
        var properties = new PropertyConverter(info);
        var trueSupers = convertSupers(supers);
        var generics = GenericInfo.parse(info, node.getName().getSubtypes());
        var descriptors = node.getDescriptors();
        var isFinal = !descriptors.contains(DescriptorNode.NONFINAL);
        var type = new StdTypeObject(node.getName().strName(), List.of(trueSupers), generics, isFinal);
        ensureProperInheritance(type, trueSupers);
        info.addType(type);
        for (var stmt : node.getBody()) {  // FIXME: Get methods taking same type working
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
        if (type.isFinal() && classIsConstant(declarations, methods, operators, properties)) {
            type.isConstClass();
        }
        type.setOperators(operators.getOperatorInfos());
        List<Short> superConstants = new ArrayList<>();
        for (var sup : type.getSupers()) {
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
        info.addVariable(name, Builtins.TYPE.generify(type), new ClassConstant(name, classIndex));
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
            info.addVariable("self", isConstMethod ? type.makeConst() : type.makeMut(), isConstMethod);
            info.addVariable("cls", Builtins.TYPE.generify(type), true);
            try {
                info.allowPrivateAccess(type);
                recursivelyAllowProtectedAccess(type);
                var fnInfo = methodInfo.getInfo();
                for (var arg : fnInfo.getArgs()) {
                    info.addVariable(arg.getName(), arg.getType());
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

    private static boolean classIsConstant(@NotNull DeclarationConverter decls, @NotNull MethodConverter methods,
                                    @NotNull OperatorConverter ops, @NotNull PropertyConverter props) {
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
                        pair.getValue().getLineInfo(),
                        methods.get(pair.getKey()).getLineInfo()
                );
            } else if (staticMethods.containsKey(pair.getKey())) {
                throw CompilerException.doubleDef(
                        pair.getKey(),
                        pair.getValue().getLineInfo(),
                        staticMethods.get(pair.getKey()).getLineInfo()
                );
            }
        }
    }

    private static void checkVars(String strName, Lined name, @NotNull Map<String, ? extends Lined> vars) {
        if (vars.containsKey(strName)) {
            throw CompilerException.doubleDef(strName, name.getLineInfo(), vars.get(strName).getLineInfo());
        }
    }

    private static final class MethodInfo implements Lined {
        private final Set<DescriptorNode> descriptors;
        private final FunctionInfo info;
        private final StatementBodyNode body;
        private final LineInfo lineInfo;

        MethodInfo(Set<DescriptorNode> descriptors, FunctionInfo info, StatementBodyNode body, LineInfo lineInfo) {
            this.descriptors = descriptors;
            this.info = info;
            this.body = body;
            this.lineInfo = lineInfo;
        }

        public Set<DescriptorNode> getDescriptors() {
            return descriptors;
        }

        public FunctionInfo getInfo() {
            return info;
        }

        public StatementBodyNode getBody() {
            return body;
        }

        public LineInfo getLineInfo() {
            return lineInfo;
        }
    }

    private static final class DeclarationConverter {
        private final Map<String, AttributeInfo> vars;
        private final Map<String, AttributeInfo> staticVars;
        private final CompilerInfo info;

        DeclarationConverter(CompilerInfo info) {
            this.info = info;
            vars = new HashMap<>();
            staticVars = new HashMap<>();
        }

        void parse(@NotNull DeclarationNode node) {
            for (var name : node.getNames()) {
                var strName = ((VariableNode) name).getName();
                var descriptors = node.getDescriptors();
                checkVars(strName, name, vars);
                checkVars(strName, name, staticVars);
                var attrInfo = new AttributeInfo(descriptors, info.getType(node.getType()), node.getLineInfo());
                if (descriptors.contains(DescriptorNode.STATIC)) {
                    staticVars.put(strName, attrInfo);
                } else {
                    vars.put(strName, attrInfo);
                }
            }
        }

        void parse(@NotNull DeclaredAssignmentNode node) {
            var attrType = info.getType(node.getTypes()[0].getType());
            var attrInfo = new AttributeInfo(node.getDescriptors(), attrType, node.getLineInfo());
            if (node.getDescriptors().contains(DescriptorNode.STATIC)) {
                staticVars.put(((VariableNode) node.getNames()[0]).getName(), attrInfo);
            } else {
                vars.put(((VariableNode) node.getNames()[0]).getName(), attrInfo);
            }
        }

        public Map<String, AttributeInfo> getVars() {
            return vars;
        }

        @NotNull
        public Map<String, Short> varsWithInts() {
            Map<String, Short> result = new HashMap<>();
            for (var pair : vars.entrySet()) {
                result.put(pair.getKey(), (short) 0);  // TODO: Effectively serialize types (esp. union)
            }
            return result;
        }

        public Map<String, AttributeInfo> getStaticVars() {
            return staticVars;
        }

        @NotNull
        public Map<String, Short> staticVarsWithInts() {
            Map<String, Short> result = new HashMap<>();
            for (var pair : vars.entrySet()) {
                result.put(pair.getKey(), (short) 0);
            }
            return result;
        }
    }

    private static final class MethodConverter {
        private final Map<String, MethodInfo> methodMap;
        private final Map<String, MethodInfo> staticMethods;
        private final CompilerInfo info;

        MethodConverter(CompilerInfo info) {
            this.info = info;
            methodMap = new HashMap<>();
            staticMethods = new HashMap<>();
        }

        void parse(@NotNull MethodDefinitionNode node) {
            var name = methodName(node);
            var args = ArgumentInfo.of(node.getArgs(), info);
            var returns = info.typesOf(node.getRetval());
            var fnInfo = new FunctionInfo(name, args, returns);
            checkVars(name, node, methodMap);
            checkVars(name, node, staticMethods);
            var mInfo = new MethodInfo(node.getDescriptors(), fnInfo, node.getBody(), node.getLineInfo());
            if (!node.getDescriptors().contains(DescriptorNode.STATIC)) {
                methodMap.put(name, mInfo);
            } else {
                staticMethods.put(name, mInfo);
            }
        }

        public Map<String, MethodInfo> getMethods() {
            return methodMap;
        }

        public Map<String, MethodInfo> getStaticMethods() {
            return staticMethods;
        }

        private String methodName(@NotNull MethodDefinitionNode node) {
            return node.getName().getName(); // TODO: Distinguish between args
        }
    }

    private static final class OperatorConverter {
        private final CompilerInfo info;
        private final Map<OpSpTypeNode, FunctionInfo> operatorInfos;
        private final Map<OpSpTypeNode, MethodInfo> operators;

        OperatorConverter(CompilerInfo info) {
            this.operatorInfos = new HashMap<>();
            this.operators = new HashMap<>();
            this.info = info;
        }

        void parse(@NotNull OperatorDefinitionNode node) {
            var op = node.getOpCode().getOperator();
            var args = ArgumentInfo.of(node.getArgs(), info);
            var returns = info.typesOf(node.getRetType());
            FunctionInfo fnInfo;
            if (DEFAULT_RETURNS.containsKey(op)) {
                var lineInfo = node.getRetType().length > 0 ? node.getRetType()[0].getLineInfo() : LineInfo.empty();
                fnInfo = new FunctionInfo("", args, validateReturns(lineInfo, op, returns));
            } else {
                fnInfo = new FunctionInfo("", args, returns);
            }
            if (operators.containsKey(op)) {
                throw CompilerException.doubleDef(op, node.getLineInfo(), operators.get(op).getLineInfo());
            }
            operatorInfos.put(op, fnInfo);
            operators.put(op, new MethodInfo(node.getDescriptors(), fnInfo, node.getBody(), node.getLineInfo()));
        }

        public Map<OpSpTypeNode, FunctionInfo> getOperatorInfos() {
            return operatorInfos;
        }

        public Map<OpSpTypeNode, MethodInfo> getOperators() {
            return operators;
        }

        @NotNull
        private TypeObject[] validateReturns(LineInfo info, OpSpTypeNode op, @NotNull TypeObject... returns) {
            if (returns.length > 0) {
                if (DEFAULT_RETURNS.containsKey(op) && !DEFAULT_RETURNS.get(op).isSuperclass(returns[0])) {
                    throw CompilerException.format(
                            "%s must return '%s', which clashes with the given type '%s'",
                            info, op, returns[0].name(), DEFAULT_RETURNS.get(op).name()
                    );
                }
                return returns;
            } else {
                return DEFAULT_RETURNS.containsKey(op) ? new TypeObject[] {DEFAULT_RETURNS.get(op)} : new TypeObject[0];
            }
        }

        private static final Map<OpSpTypeNode, TypeObject> DEFAULT_RETURNS;

        static {
            var temp = new EnumMap<OpSpTypeNode, TypeObject>(OpSpTypeNode.class);
            // Conversion methods
            temp.put(OpSpTypeNode.STR, Builtins.STR);
            temp.put(OpSpTypeNode.BOOL, Builtins.BOOL);
            temp.put(OpSpTypeNode.REPR, Builtins.STR);
            temp.put(OpSpTypeNode.INT, Builtins.INT);
            temp.put(OpSpTypeNode.HASH, Builtins.INT);
            // Boolean operators
            temp.put(OpSpTypeNode.EQUALS, Builtins.BOOL);
            temp.put(OpSpTypeNode.LESS_THAN, Builtins.BOOL);
            temp.put(OpSpTypeNode.LESS_EQUAL, Builtins.BOOL);
            temp.put(OpSpTypeNode.GREATER_THAN, Builtins.BOOL);
            temp.put(OpSpTypeNode.GREATER_EQUAL, Builtins.BOOL);
            temp.put(OpSpTypeNode.IN, Builtins.BOOL);

            DEFAULT_RETURNS = Collections.unmodifiableMap(temp);
        }
    }

    private static final class PropertyConverter {
        private final Map<String, AttributeInfo> properties;
        private final Map<String, StatementBodyNode> getters;
        private final Map<String, StatementBodyNode> setters;
        private final Map<String, LineInfo> lineInfos;
        private final CompilerInfo info;

        PropertyConverter(CompilerInfo info) {
            this.properties = new HashMap<>();
            this.getters = new HashMap<>();
            this.setters = new HashMap<>();
            this.lineInfos = new HashMap<>();
            this.info = info;
        }

        void parse(@NotNull PropertyDefinitionNode node) {
            var name = node.getName().getName();
            var type = info.getType(node.getType());
            if (properties.containsKey(name)) {
                throw CompilerException.format(
                        "Illegal name: property with name '%s' already defined in this class (see line %d)",
                        node, name, lineInfos.get(name).getLineNumber()
                );
            }
            properties.put(name, new AttributeInfo(node.getDescriptors(), type));
            getters.put(name, node.getGet());
            setters.put(name, node.getSet());  // TODO: If setter is empty
            lineInfos.put(name, node.getLineInfo());
        }

        public Map<String, AttributeInfo> getProperties() {
            return properties;
        }

        @NotNull
        public Map<String, MethodInfo> getGetters() {
            Map<String, MethodInfo> result = new HashMap<>();
            for (var pair : getters.entrySet()) {
                var property = properties.get(pair.getKey());
                var fnInfo = new FunctionInfo(property.getType());
                var mInfo = new MethodInfo(property.getDescriptors(), fnInfo,
                        pair.getValue(), pair.getValue().getLineInfo());
                result.put(pair.getKey(), mInfo);
            }
            return result;
        }

        @NotNull
        public Map<String, MethodInfo> getSetters() {
            Map<String, MethodInfo> result = new HashMap<>();
            for (var pair : setters.entrySet()) {
                var property = properties.get(pair.getKey());
                var fnInfo = new FunctionInfo(ArgumentInfo.of(properties.get(pair.getKey()).getType()));
                var mInfo = new MethodInfo(property.getDescriptors(), fnInfo,
                        pair.getValue(), pair.getValue().getLineInfo());
                result.put(pair.getKey(), mInfo);
            }
            return result;
        }
    }
}
