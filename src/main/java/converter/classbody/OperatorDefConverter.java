package main.java.converter.classbody;

import main.java.converter.AccessLevel;
import main.java.converter.ArgumentInfo;
import main.java.converter.Builtins;
import main.java.converter.CompilerException;
import main.java.converter.CompilerInfo;
import main.java.converter.FunctionInfo;
import main.java.converter.MethodInfo;
import main.java.converter.TypeObject;
import main.java.parser.DescriptorNode;
import main.java.parser.GenericOperatorNode;
import main.java.parser.LineInfo;
import main.java.parser.OpSpTypeNode;
import main.java.parser.OperatorDefinitionNode;
import main.java.parser.StatementBodyNode;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class OperatorDefConverter {
    private final CompilerInfo info;
    private final Map<OpSpTypeNode, MethodInfo> operatorInfos;
    private final Map<OpSpTypeNode, Method> operators;
    private final Map<OpSpTypeNode, MethodInfo> staticOperatorInfos;
    private final Map<OpSpTypeNode, Method> staticOperators;

    public OperatorDefConverter(CompilerInfo info) {
        this.operatorInfos = new HashMap<>();
        this.operators = new HashMap<>();
        this.staticOperatorInfos = new HashMap<>();
        this.staticOperators = new HashMap<>();
        this.info = info;
    }

    public void parse(@NotNull OperatorDefinitionNode node) {
        var op = node.getOpCode().getOperator();
        var args = ArgumentInfo.of(node.getArgs(), info);
        var returns = info.typesOf(node.getRetType());
        var lineInfo = node.getRetType().length > 0 ? node.getRetType()[0].getLineInfo() : node.getLineInfo();
        boolean isGenerator = ALWAYS_GENERATOR.contains(op);
        var retValues = validateReturns(lineInfo, isGenerator, op, returns);
        FunctionInfo fnInfo = new FunctionInfo("", isGenerator, args, retValues);
        boolean isStatic = node.getDescriptors().contains(DescriptorNode.STATIC);
        var opInfos = isStatic ? staticOperatorInfos : operatorInfos;
        var ops = isStatic ? staticOperators : operators;
        if (opInfos.containsKey(op)) {
            throw CompilerException.doubleDef(op, node, ops.get(op));
        }
        var accessLevel = AccessLevel.fromDescriptors(node.getDescriptors());
        var isMut = op == OpSpTypeNode.NEW || node.getDescriptors().contains(DescriptorNode.MUT);
        var mInfo = new MethodInfo(accessLevel, isMut, fnInfo);
        opInfos.put(op, mInfo);
        ops.put(op, new Method(accessLevel, isMut, fnInfo, node.getBody(), node.getLineInfo()));
    }

    public void parse(@NotNull GenericOperatorNode node) {
        var op = node.getOpCode().getOperator();
        var args = ArgumentInfo.of(node.getArgs(), info);
        var returns = info.typesOf(node.getRetvals());
        var lineInfo = node.getRetvals().length > 0 ? node.getRetvals()[0].getLineInfo() : node.getLineInfo();
        var isGenerator = ALWAYS_GENERATOR.contains(op);
        var retValues = validateReturns(lineInfo, isGenerator, op, returns);
        FunctionInfo fnInfo = new FunctionInfo("", isGenerator, args, retValues);
        if (operatorInfos.containsKey(op)) {
            throw CompilerException.doubleDef(op, node, operators.get(op));
        }
        var accessLevel = AccessLevel.fromDescriptors(node.getDescriptors());
        var isMut = op == OpSpTypeNode.NEW || node.getDescriptors().contains(DescriptorNode.MUT);
        var mInfo = new MethodInfo(accessLevel, isMut, fnInfo);
        operatorInfos.put(op, mInfo);
        operators.put(op, new Method(accessLevel, isMut, fnInfo,
                StatementBodyNode.empty(), node.getLineInfo()));
    }

    public Map<OpSpTypeNode, MethodInfo> getOperatorInfos() {
        return operatorInfos;
    }

    public Map<OpSpTypeNode, Method> getOperators() {
        return operators;
    }

    public Map<OpSpTypeNode, MethodInfo> getStaticOperatorInfos() {
        return staticOperatorInfos;
    }

    public Map<OpSpTypeNode, Method> getStaticOperators() {
        return staticOperators;
    }

    @NotNull
    private TypeObject[] validateReturns(LineInfo info, boolean isGenerator, OpSpTypeNode op, @NotNull TypeObject... returns) {
        if (MANDATORY_RETURNS.containsKey(op) && returns.length < MANDATORY_RETURNS.get(op)) {
            var retCount = MANDATORY_RETURNS.get(op);
            if (retCount == 1) {
                throw CompilerException.format("%s must specify a return value", info, op);
            } else {
                throw CompilerException.format(
                        "%s must return at least %d values, only %d were declared",
                        info, op, retCount, returns.length
                );
            }
        } else if (returns.length > 0) {
            if (DEFAULT_RETURNS.containsKey(op) && !DEFAULT_RETURNS.get(op).isSuperclass(returns[0])) {
                throw CompilerException.format(
                        "%s must return '%s', which clashes with the given type '%s'",
                        info, op, returns[0].name(), DEFAULT_RETURNS.get(op).name()
                );
            }
            return isGenerator ? new TypeObject[]{Builtins.ITERABLE.generify(info, returns)} : returns;
        } else {
            return DEFAULT_RETURNS.containsKey(op) ? new TypeObject[] {DEFAULT_RETURNS.get(op)} : new TypeObject[0];
        }
    }

    private static final Map<OpSpTypeNode, TypeObject> DEFAULT_RETURNS;
    private static final Map<OpSpTypeNode, Integer> MANDATORY_RETURNS;
    private static final Set<OpSpTypeNode> ALWAYS_GENERATOR;

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

        temp.put(OpSpTypeNode.COMPARE, Builtins.INT);

        DEFAULT_RETURNS = Collections.unmodifiableMap(temp);
    }

    static {
        var temp = new EnumMap<OpSpTypeNode, Integer>(OpSpTypeNode.class);
        temp.put(OpSpTypeNode.ITER, 1);
        temp.put(OpSpTypeNode.ITER_SLICE, 1);
        temp.put(OpSpTypeNode.GET_ATTR, 1);
        temp.put(OpSpTypeNode.GET_SLICE, 1);

        MANDATORY_RETURNS = Collections.unmodifiableMap(temp);
    }

    static {
        ALWAYS_GENERATOR = EnumSet.of(OpSpTypeNode.ITER, OpSpTypeNode.ITER_SLICE);
    }
}
