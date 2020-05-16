package main.java.converter.classbody;

import main.java.converter.ArgumentInfo;
import main.java.converter.Builtins;
import main.java.converter.CompilerException;
import main.java.converter.CompilerInfo;
import main.java.converter.FunctionInfo;
import main.java.converter.TypeObject;
import main.java.parser.GenericOperatorNode;
import main.java.parser.LineInfo;
import main.java.parser.OpSpTypeNode;
import main.java.parser.OperatorDefinitionNode;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class OperatorDefConverter {
    private final CompilerInfo info;
        private final Map<OpSpTypeNode, FunctionInfo> operatorInfos;
        private final Map<OpSpTypeNode, MethodInfo> operators;

        public OperatorDefConverter(CompilerInfo info) {
            this.operatorInfos = new HashMap<>();
            this.operators = new HashMap<>();
            this.info = info;
        }

        public void parse(@NotNull OperatorDefinitionNode node) {
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
            if (operatorInfos.containsKey(op)) {
                throw CompilerException.doubleDef(op, node, operators.get(op));
            }
            operatorInfos.put(op, fnInfo);
            operators.put(op, new MethodInfo(node.getDescriptors(), fnInfo, node.getBody(), node.getLineInfo()));
        }

        public void parse(@NotNull GenericOperatorNode node) {
            var op = node.getOpCode().getOperator();
            var args = ArgumentInfo.of(node.getArgs(), info);
            var returns = info.typesOf(node.getRetvals());
            FunctionInfo fnInfo;
            if (DEFAULT_RETURNS.containsKey(op)) {
                var lineInfo = node.getRetvals().length > 0 ? node.getRetvals()[0].getLineInfo() : LineInfo.empty();
                fnInfo = new FunctionInfo("", args, validateReturns(lineInfo, op, returns));
            } else {
                fnInfo = new FunctionInfo("", args, returns);
            }
            if (operatorInfos.containsKey(op)) {
                throw CompilerException.doubleDef(op, node, operators.get(op));
            }
            operatorInfos.put(op, fnInfo);
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
