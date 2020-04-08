package main.java.converter;

import main.java.parser.ClassDefinitionNode;
import main.java.parser.DeclarationNode;
import main.java.parser.DeclaredAssignmentNode;
import main.java.parser.DescriptorNode;
import main.java.parser.LineInfo;
import main.java.parser.MethodDefinitionNode;
import main.java.parser.OpSpTypeNode;
import main.java.parser.OperatorDefinitionNode;
import main.java.parser.PropertyDefinitionNode;
import main.java.parser.StatementBodyNode;
import main.java.parser.VariableNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ClassConverter implements BaseConverter {
    private ClassDefinitionNode node;
    private CompilerInfo info;

    public ClassConverter(CompilerInfo info, ClassDefinitionNode node) {
        this.node = node;
        this.info = info;
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        var supers = info.typesOf(node.getSuperclasses());
        var declarations = new DeclarationConverter(info);
        var methods = new MethodConverter(info);
        var operators = new OperatorConverter(info);
        var properties = new PropertyConverter(info);
        var trueSupers = Arrays.copyOf(supers, supers.length, StdTypeObject[].class);
        var generics = GenericInfo.parse(info, node.getName().getSubtypes());
        var type = new StdTypeObject(node.getName().strName(), List.of(trueSupers), generics);
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
        type.setOperators(operators.getOperatorInfos());
        List<Short> superConstants = new ArrayList<>();
        for (var sup : type.getSupers()) {
            superConstants.add(info.constIndex(sup.name()));
        }
        type.setAttributes(allAttributes(declarations.getVars(), methods.getMethods(), properties.getProperties()));
        type.setStaticAttributes(allAttributes(declarations.getStaticVars(), methods.getMethods(), new HashMap<>()));
        var cls = new ClassInfo.Factory()
                .setType(type)
                .setSuperConstants(superConstants)
                .setVariables(declarations.varsWithInts())
                .setStaticVariables(declarations.staticVarsWithInts())
                .setOperatorDefs(convert(type, operators.getOperators()))
                .setStaticOperators(new HashMap<>())
                .setMethodDefs(convert(type, methods.getMethods()))
                .setStaticMethods(convert(type, methods.getStaticMethods()))
                .setProperties(merge(convert(properties.getGetters(), type, properties.getGetterInfos()),
                        convert(properties.getSetters(), type, properties.getSetterInfos())))
                .create();
        int classIndex = info.addClass(cls);
        var name = node.getName().strName();
        if (Builtins.FORBIDDEN_NAMES.contains(name)) {
            throw CompilerException.format("Illegal name for class '%s'", node.getName(), name);
        }
        info.addVariable(name, Builtins.TYPE.generify(type), new ClassConstant(name, classIndex));
        return Collections.emptyList();
    }

    @NotNull  // TODO: Move properties to the other convert()
    private <T> Map<T, List<Byte>> convert(@NotNull Map<T, StatementBodyNode> functions,
                                           StdTypeObject type, Map<T, FunctionInfo> args) {
        Map<T, List<Byte>> result = new HashMap<>();
        for (var pair : functions.entrySet()) {
            info.addStackFrame();
            info.addVariable("self", type, true);
            info.addVariable("cls", Builtins.TYPE.generify(type), true);
            var fnInfo = args.get(pair.getKey());
            for (var arg : fnInfo.getArgs()) {
                info.addVariable(arg.getName(), arg.getType());
            }
            info.addFunctionReturns(fnInfo.getReturns());
            var bytes = BaseConverter.bytes(0, pair.getValue(), info);
            info.popFnReturns();
            result.put(pair.getKey(), bytes);
            info.removeStackFrame();
        }
        return result;
    }

    @NotNull
    private <T> Map<T, List<Byte>> convert(StdTypeObject type, @NotNull Map<T, MethodInfo> functions) {
        Map<T, List<Byte>> result = new HashMap<>();
        for (var pair : functions.entrySet()) {
            var methodInfo = pair.getValue();
            info.addStackFrame();
            info.addVariable("self", type, !methodInfo.getDescriptors().contains(DescriptorNode.MUT));
            info.addVariable("cls", Builtins.TYPE.generify(type), true);
            var fnInfo = methodInfo.getInfo();
            for (var arg : fnInfo.getArgs()) {
                info.addVariable(arg.getName(), arg.getType());
            }
            info.addFunctionReturns(fnInfo.getReturns());
            var bytes = BaseConverter.bytes(0, methodInfo.getBody(), info);
            info.popFnReturns();
            result.put(pair.getKey(), bytes);
            info.removeStackFrame();
        }
        return result;
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
    private Map<String, TypeObject> allAttributes(Map<String, TypeObject> attrs,
                                                  @NotNull Map<String, MethodInfo> methods,
                                                  Map<String, TypeObject> properties) {
        var finalAttrs = new HashMap<>(attrs);
        for (var pair : methods.entrySet()) {
            finalAttrs.put(pair.getKey(), pair.getValue().getInfo().toCallable());
        }
        finalAttrs.putAll(properties);
        return finalAttrs;
    }

    private static final class MethodInfo {
        private final Set<DescriptorNode> descriptors;
        private final FunctionInfo info;
        private final StatementBodyNode body;

        MethodInfo(Set<DescriptorNode> descriptors, FunctionInfo info, StatementBodyNode body) {
            this.descriptors = descriptors;
            this.info = info;
            this.body = body;
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
    }

    private static final class DeclarationConverter {
        private Map<String, TypeObject> vars;
        private Map<String, TypeObject> staticVars;
        private CompilerInfo info;

        DeclarationConverter(CompilerInfo info) {
            this.info = info;
            vars = new HashMap<>();
            staticVars = new HashMap<>();
        }

        void parse(@NotNull DeclarationNode node) {
            for (var name : node.getNames()) {
                var strName = ((VariableNode) name).getName();
                if (node.getDescriptors().contains(DescriptorNode.STATIC)) {
                    staticVars.put(strName, info.getType(node.getType()));
                } else {
                    vars.put(strName, info.getType(node.getType()));
                }
            }
        }

        void parse(@NotNull DeclaredAssignmentNode node) {
            if (node.getDescriptors().contains(DescriptorNode.STATIC)) {
                staticVars.put(((VariableNode) node.getNames()[0]).getName(), info.getType(node.getTypes()[0].getType()));
            } else {
                vars.put(((VariableNode) node.getNames()[0]).getName(), info.getType(node.getTypes()[0].getType()));
            }
        }

        public Map<String, TypeObject> getVars() {
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

        public Map<String, TypeObject> getStaticVars() {
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
        private CompilerInfo info;

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
            if (!node.getDescriptors().contains(DescriptorNode.STATIC)) {
                methodMap.put(name, new MethodInfo(node.getDescriptors(), fnInfo, node.getBody()));
            } else {
                staticMethods.put(name, new MethodInfo(node.getDescriptors(), fnInfo, node.getBody()));
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
            operatorInfos.put(op, fnInfo);
            operators.put(op, new MethodInfo(node.getDescriptors(), fnInfo, node.getBody()));
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
        private Map<String, TypeObject> properties;
        private Map<String, StatementBodyNode> getters;
        private Map<String, StatementBodyNode> setters;
        private Map<String, LineInfo> lineInfos;
        private CompilerInfo info;

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
            properties.put(name, type);
            getters.put(name, node.getGet());
            setters.put(name, node.getSet());  // TODO: If setter is empty
            lineInfos.put(name, node.getLineInfo());
        }

        public Map<String, StatementBodyNode> getGetters() {
            return getters;
        }

        public Map<String, StatementBodyNode> getSetters() {
            return setters;
        }

        public Map<String, TypeObject> getProperties() {
            return properties;
        }

        @NotNull
        public Map<String, FunctionInfo> getGetterInfos() {
            Map<String, FunctionInfo> result = new HashMap<>();
            for (var key : getters.keySet()) {
                result.put(key, new FunctionInfo(properties.get(key)));
            }
            return result;
        }

        @NotNull
        public Map<String, FunctionInfo> getSetterInfos() {
            Map<String, FunctionInfo> result = new HashMap<>();
            for (var key : setters.keySet()) {
                result.put(key, new FunctionInfo(ArgumentInfo.of(properties.get(key))));
            }
            return result;
        }
    }
}
