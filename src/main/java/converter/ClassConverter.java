package main.java.converter;

import main.java.parser.ClassDefinitionNode;
import main.java.parser.DeclarationNode;
import main.java.parser.DeclaredAssignmentNode;
import main.java.parser.DescriptorNode;
import main.java.parser.MethodDefinitionNode;
import main.java.parser.OpSpTypeNode;
import main.java.parser.OperatorDefinitionNode;
import main.java.parser.StatementBodyNode;
import main.java.parser.VariableNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        assert node.getName().getSubtypes().length == 0;
        var trueSupers = Arrays.copyOf(supers, supers.length, StdTypeObject[].class);
        var type = new StdTypeObject(node.getName().strName(), List.of(trueSupers));
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
            } else {
                throw new UnsupportedOperationException("Node not yet supported");
            }
        }
        for (var opPair : operators.getOperators().entrySet()) {
            type.setOperator(opPair.getKey(), opPair.getValue());
        }
        List<Integer> superConstants = new ArrayList<>();
        for (var sup : type.getSupers()) {
            superConstants.add(info.constIndex(sup.name()));
        }
        var cls = new ClassInfo.Factory()
                .setType(type)
                .setSuperConstants(superConstants)
                .setVariables(declarations.getVars().keySet())
                .setStaticVariables(declarations.getStaticVars().keySet())
                .setOperatorDefs(convert(operators.getOpNodes(), type))
                .setStaticOperators(new HashMap<>())
                .setMethodDefs(convert(methods.getNodes(), type))
                .setStaticMethods(convert(methods.getStaticNodes(), type))
                .create();
        int classIndex = info.addClass(cls);
        info.addVariable(node.getName().strName(), Builtins.TYPE, new ClassConstant(classIndex));
        return Collections.emptyList();
    }

    @NotNull
    private <T> Map<T, List<Byte>> convert(@NotNull Map<T, StatementBodyNode> functions, StdTypeObject type) {
        Map<T, List<Byte>> result = new HashMap<>();
        for (var pair : functions.entrySet()) {
            info.addStackFrame();
            info.addVariable("self", type, true);
            info.addVariable("cls", Builtins.TYPE, true);
            result.put(pair.getKey(), BaseConverter.bytes(0, pair.getValue(), info));
            info.removeStackFrame();
        }
        return result;
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

        public Map<String, TypeObject> getStaticVars() {
            return staticVars;
        }
    }

    private static final class MethodConverter {
        private Map<String, FunctionInfo> methodMap;
        private Map<String, StatementBodyNode> nodeMap;
        private Map<String, FunctionInfo> staticMethods;
        private Map<String, StatementBodyNode> staticNodes;
        private CompilerInfo info;

        MethodConverter(CompilerInfo info) {
            this.info = info;
            methodMap = new HashMap<>();
            nodeMap = new HashMap<>();
            staticMethods = new HashMap<>();
            staticNodes = new HashMap<>();
        }

        void parse(@NotNull MethodDefinitionNode node) {
            var name = methodName(node);
            var args = ArgumentInfo.of(node.getArgs(), info);
            var returns = info.typesOf(node.getRetval());
            if (!node.getDescriptors().contains(DescriptorNode.STATIC)) {
                methodMap.put(name, new FunctionInfo(name, args, returns));
                nodeMap.put(name, node.getBody());
            } else {
                staticMethods.put(name, new FunctionInfo(name, args, returns));
                staticNodes.put(name, node.getBody());
            }
        }

        public Map<String, FunctionInfo> getMethods() {
            return methodMap;
        }

        public Map<String, StatementBodyNode> getNodes() {
            return nodeMap;
        }

        public Map<String, FunctionInfo> getStaticMethods() {
            return staticMethods;
        }

        public Map<String, StatementBodyNode> getStaticNodes() {
            return staticNodes;
        }

        private String methodName(@NotNull MethodDefinitionNode node) {
            return node.getName().getName(); // TODO: Distinguish between args
        }
    }

    private static final class OperatorConverter {
        private CompilerInfo info;
        private Map<OpSpTypeNode, FunctionInfo> operators;
        private Map<OpSpTypeNode, StatementBodyNode> opNodes;

        OperatorConverter(CompilerInfo info) {
            this.operators = new HashMap<>();
            this.opNodes = new HashMap<>();
            this.info = info;
        }

        void parse(@NotNull OperatorDefinitionNode node) {
            var op = node.getOpCode().getOperator();
            var args = ArgumentInfo.of(node.getArgs(), info);
            var returns = info.typesOf(node.getRetType());
            operators.put(op, new FunctionInfo("", args, returns));
            opNodes.put(op, node.getBody());
        }

        public Map<OpSpTypeNode, FunctionInfo> getOperators() {
            return operators;
        }

        public Map<OpSpTypeNode, StatementBodyNode> getOpNodes() {
            return opNodes;
        }
    }
}
