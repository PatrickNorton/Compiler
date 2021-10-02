package main.java.converter;

import main.java.converter.bytecode.VariableBytecode;
import main.java.parser.ElifStatementNode;
import main.java.parser.IfStatementNode;
import main.java.parser.Lined;
import main.java.parser.OperatorNode;
import main.java.parser.OperatorTypeNode;
import main.java.parser.StatementBodyNode;
import main.java.parser.TestNode;
import main.java.parser.VariableNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

public final class IfConverter implements BaseConverter {
    private final IfStatementNode node;
    private final CompilerInfo info;

    public IfConverter(CompilerInfo info, IfStatementNode node) {
        this.node = node;
        this.info = info;
    }

    @NotNull
    @Override
    public BytecodeList convert() {
        return convertAndReturn().getKey();
    }

    @Override
    @NotNull
    public Pair<BytecodeList, DivergingInfo> convertAndReturn() {
        BytecodeList bytes;
        DivergingInfo willReturn;
        boolean hasAs = !node.getAs().isEmpty();
        boolean hasElse = !node.getElseStmt().isEmpty();
        checkConditions();
        var endLabel = info.newJumpLabel();
        if (hasAs) {
            bytes = addAs(node.getConditional(), node.getAs());
            willReturn = addBodyWithAs(bytes, node.getBody(), node.getAs(), endLabel);
            info.removeStackFrame();
        } else {
            var pair = convertOptimizedNot(node.getConditional());
            bytes = new BytecodeList(pair.getKey());
            willReturn = addBody(bytes, node.getBody(), pair.getValue(), endLabel);
        }
        for (var elif : node.getElifs()) {
            willReturn.andWith(addElif(bytes, elif, endLabel));
        }
        if (hasElse) {
            willReturn.andWith(addElse(bytes, node.getElseStmt()));
        } else {
            willReturn.makeUncertain();
        }
        bytes.addLabel(endLabel);
        return Pair.of(bytes, willReturn);
    }

    private DivergingInfo addBody(
            @NotNull BytecodeList bytes, StatementBodyNode body, boolean jumpType, Label endLabel
    ) {
        var pair = BaseConverter.bytesWithReturn(body, info);
        var bodyBytes = pair.getKey();
        var jumpTarget = info.newJumpLabel();
        bytes.add(jumpType ? Bytecode.JUMP_TRUE : Bytecode.JUMP_FALSE, jumpTarget);
        bytes.addAll(bodyBytes);
        bytes.add(Bytecode.JUMP, endLabel);
        bytes.addLabel(jumpTarget);
        return pair.getValue();
    }

    private DivergingInfo addBodyWithAs(
            @NotNull BytecodeList bytes, StatementBodyNode body, VariableNode asName, Label endLabel
    ) {
        var jumpLabel = info.newJumpLabel();
        bytes.add(Bytecode.JUMP_FALSE, jumpLabel);
        bytes.add(Bytecode.STORE, new VariableBytecode(info.varIndex(asName)));
        var pair = BaseConverter.bytesWithReturn(body, info);
        bytes.addAll(pair.getKey());
        bytes.add(Bytecode.JUMP, endLabel);
        bytes.addLabel(jumpLabel);
        bytes.add(Bytecode.POP_TOP);
        return pair.getValue();
    }

    private DivergingInfo addElif(
            @NotNull BytecodeList bytes, @NotNull ElifStatementNode elif, Label endLabel
    ) {
        var cond = elif.getTest();
        var body = elif.getBody();
        DivergingInfo willReturn;
        if (elif.getAs().isEmpty()) {
            var pair = convertOptimizedNot(cond);
            bytes.addAll(pair.getKey());
            willReturn = addBody(bytes, body, pair.getValue(), endLabel);
        } else {
            bytes.addAll(addAs(cond, elif.getAs()));
            willReturn = addBodyWithAs(bytes, body, elif.getAs(), endLabel);
            info.removeStackFrame();
        }
        return willReturn;
    }

    private DivergingInfo addElse(@NotNull BytecodeList bytes, StatementBodyNode body) {
        var pair = BaseConverter.bytesWithReturn(body, info);
        bytes.addAll(pair.getKey());
        return pair.getValue();
    }

    private BytecodeList addAs(TestNode condition, VariableNode as) {
        if (!(condition instanceof OperatorNode)) {
            throw CompilerException.of(
                    "Cannot use 'as' here: condition must be 'instanceof' or 'is not null'", condition
            );
        }
        var result = OperatorConverter.convertWithAs((OperatorNode) condition, info, 1);
        var bytes = result.getKey();
        var asType = result.getValue();
        info.addStackFrame();
        info.checkDefinition(as.getName(), as);
        info.addVariable(as.getName(), asType, as);
        return bytes;
    }

    private void checkConditions() {
        // TODO: Use this information to actually change emitted bytecode
        var firstConst = TestConverter.constantReturn(node.getConditional(), info, 1);
        if (firstConst.isPresent()) {
            checkCond(firstConst.orElseThrow(), node.getConditional());
        }
        for (var elif : node.getElifs()) {
            var constant = TestConverter.constantReturn(elif.getTest(), info, 1);
            if (constant.isPresent()) {
                checkCond(constant.orElseThrow(), node.getConditional());
            }
        }
    }

    private void checkCond(LangConstant value, Lined node) {
        var boolVal = value.boolValue();
        if (boolVal.isPresent()) {
            CompilerWarning.warnf(
                    "Statement in conditional will always evaluate to %b",
                    WarningType.UNREACHABLE, info, node, boolVal.orElseThrow()
            );
        }
    }

    @NotNull
    private Pair<BytecodeList, Boolean> convertOptimizedNot(TestNode cond) {
        if (cond instanceof OperatorNode op) {
            if (op.getOperator() == OperatorTypeNode.BOOL_NOT) {
                if (op.getOperands().length != 1) {
                    throw CompilerException.format(
                            "'not' statement expected one operand, got %d", cond, op.getOperands().length
                    );
                } else {
                    var bytes = TestConverter.bytes(op.getOperands()[0].getArgument(), info, 1);
                    return Pair.of(bytes, true);
                }
            }
        }
        var bytes = TestConverter.bytes(cond, info, 1);
        return Pair.of(bytes, false);
    }

    public static Label addJump(BytecodeList bytes, @NotNull TestNode cond, CompilerInfo info) {
        if (!cond.isEmpty()) {
            bytes.addAll(TestConverter.bytes(cond, info, 1));
            var label = info.newJumpLabel();
            bytes.add(Bytecode.JUMP_FALSE, label);
            return label;
        } else {
            return null;
        }
    }
}
