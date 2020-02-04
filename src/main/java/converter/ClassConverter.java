package main.java.converter;

import main.java.parser.ClassDefinitionNode;
import main.java.parser.DeclarationNode;
import main.java.parser.DeclaredAssignmentNode;
import main.java.parser.DescriptorNode;
import main.java.parser.MethodDefinitionNode;
import main.java.parser.StatementBodyNode;
import main.java.parser.TypedArgumentNode;
import main.java.parser.VariableNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassConverter implements BaseConverter {
    private ClassDefinitionNode node;
    private CompilerInfo info;

    public ClassConverter(CompilerInfo info, ClassDefinitionNode node) {
        this.node = node;
        this.info = info;
    }

    @Override
    public List<Byte> convert(int start) {
        var supers = info.typesOf(node.getSuperclasses());
        var declarations = new DeclarationConverter(info);
        var methods = new MethodConverter(info);
        assert node.getName().getSubtypes().length == 0;
        for (var stmt : node.getBody()) {
            if (stmt instanceof DeclarationNode) {
                declarations.parse((DeclarationNode) stmt);
            } else if (stmt instanceof DeclaredAssignmentNode) {
                declarations.parse((DeclaredAssignmentNode) stmt);
            } else if (stmt instanceof MethodDefinitionNode) {
                methods.parse((MethodDefinitionNode) stmt);
            } else {
                throw new UnsupportedOperationException("Node not yet supported");
            }
        }
        var trueSupers = Arrays.copyOf(supers, supers.length, StdTypeObject[].class);
        var type = new StdTypeObject(node.getName().strName(), List.of(trueSupers), new ArrayList<>(), null);
        info.addType(type);
        var classInfo = new ClassInfo(type, new HashMap<>(), convert(methods.nodeMap, type));
        info.addClass(classInfo);
        return Collections.emptyList();
    }

    @NotNull
    private <T> Map<T, List<Byte>> convert(@NotNull Map<T, StatementBodyNode> functions, StdTypeObject type) {
        Map<T, List<Byte>> result = new HashMap<>();
        for (var pair : functions.entrySet()) {
            info.addStackFrame();
            info.addVariable("self", type);
            info.addVariable("cls", Builtins.TYPE);
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
        private CompilerInfo info;

        MethodConverter(CompilerInfo info) {
            this.info = info;
            methodMap = new HashMap<>();
            nodeMap = new HashMap<>();
        }

        void parse(@NotNull MethodDefinitionNode node) {
            var name = methodName(node);
            var argNode = node.getArgs();
            var posArgs = getArguments(argNode.getPositionArgs());
            var normalArgs = getArguments(argNode.getArgs());
            var kwArgs = getArguments(argNode.getNameArgs());
            var args = new ArgumentInfo(posArgs, normalArgs, kwArgs);
            var returns = info.typesOf(node.getRetval());
            methodMap.put(name, new FunctionInfo(name, args, returns));
            nodeMap.put(name, node.getBody());
        }

        public Map<String, FunctionInfo> getMethods() {
            return methodMap;
        }

        public Map<String, StatementBodyNode> getNodes() {
            return nodeMap;
        }

        private String methodName(@NotNull MethodDefinitionNode node) {
            return node.getName().getName(); // TODO: Distinguish between args
        }

        @NotNull
        @Contract(pure = true)
        private Argument[] getArguments(@NotNull TypedArgumentNode[] args) {
            var result = new Argument[args.length];
            for (int i = 0; i < args.length; i++) {
                result[i] = new Argument(args[i].getName().getName(), info.getType(args[i].getType()));
            }
            return result;
        }
    }
}
