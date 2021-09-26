package main.java.converter;

import main.java.converter.bytecode.VariableBytecode;
import main.java.parser.DeleteStatementNode;
import main.java.parser.DottedVariableNode;
import main.java.parser.IndexNode;
import main.java.parser.OpSpTypeNode;
import main.java.parser.SliceNode;
import main.java.parser.TestNode;
import main.java.parser.VariableNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class DeleteConverter implements BaseConverter {
    private final DeleteStatementNode node;
    private final CompilerInfo info;

    public DeleteConverter(CompilerInfo info, DeleteStatementNode node) {
        this.info = info;
        this.node = node;
    }

    @Override
    @NotNull
    public BytecodeList convert() {
        var deleted = node.getDeleted();
        if (deleted instanceof DottedVariableNode del) {
            return convertDot(del);
        } else if (deleted instanceof IndexNode del) {
            if (IndexConverter.isSlice(del.getIndices())) {
                return convertSlice(del);
            } else {
                return convertIndex(del);
            }
        } else if (deleted instanceof VariableNode) {
            return convertVariable((VariableNode) deleted);
        } else {
            throw CompilerException.of(
                    "'del' statement can only be used on a variable or an indexed statement", node
            );
        }
    }

    @NotNull
    private BytecodeList convertIndex(@NotNull IndexNode delIndex) {
        var varConverter = TestConverter.of(info, delIndex.getVar(), 1);
        return convertIndex(varConverter, delIndex.getIndices());
    }

    @NotNull
    private BytecodeList convertIndex(TestConverter varConverter, TestNode[] indices) {
        List<TestConverter> indexConverters = new ArrayList<>();
        for (var index : indices) {
            indexConverters.add(TestConverter.of(info, index, 1));
        }
        checkAccess(varConverter, indexConverters);
        var bytes = new BytecodeList(varConverter.convert());
        for (var converter : indexConverters) {
            bytes.addAll(converter.convert());
        }
        if (indexConverters.size() == 1) {
            bytes.add(Bytecode.DEL_SUBSCRIPT);
        } else {
            bytes.addCallOp(OpSpTypeNode.DEL_ATTR, (short) indexConverters.size());
        }
        return bytes;
    }

    @NotNull
    private BytecodeList convertSlice(@NotNull IndexNode delIndex) {
        assert IndexConverter.isSlice(delIndex.getIndices());
        var varConverter = TestConverter.of(info, delIndex.getVar(), 1);
        return convertSlice(varConverter, (SliceNode) delIndex.getIndices()[0]);
    }

    @NotNull
    private BytecodeList convertSlice(@NotNull TestConverter varConverter, SliceNode slice) {
        var sliceConverter = new SliceConverter(info, slice);
        var varType = varConverter.returnType()[0];
        var sliceInfo = varType.tryOperatorInfo(node, OpSpTypeNode.DEL_SLICE, info);
        if (!sliceInfo.matches(new Argument("", Builtins.slice()))) {
            throw CompilerException.format(
                    "Cannot call '%s'.operator del[:] (operator del[:] should always be callable with a slice)",
                    node, varType.name()
            );
        }
        var bytes = new BytecodeList(varConverter.convert());
        bytes.addAll(sliceConverter.convert());
        bytes.addCallOp(OpSpTypeNode.DEL_SLICE, (short) 1);
        return bytes;
    }

    @NotNull
    private BytecodeList convertVariable(@NotNull VariableNode delVar) {
        var bytes = new BytecodeList();
        var name = delVar.getName();
        var index = info.varIndex(delVar);
        bytes.add(Bytecode.LOAD_NULL);
        bytes.add(Bytecode.STORE, new VariableBytecode(index));  // Drops value currently stored
        var varHolder = info.varHolder();
        if (varHolder.varDefinedInCurrentFrame(name)) {  // TODO: Drop non-top-frame variable properly
            varHolder.removeVariable(name);
        } else {
            CompilerWarning.warn(
                    "del on variable not defined in current frame will not remove the variable properly",
                    WarningType.TODO, info, node
            );
        }
        return bytes;
    }

    private BytecodeList convertDot(@NotNull DottedVariableNode dottedVar) {
        if (!(dottedVar.getLast().getPostDot() instanceof IndexNode)) {
            throw CompilerException.of("Cannot delete non-index from dotted variable", node);
        } else {
            return convertDotIndex(dottedVar);
        }
    }

    private BytecodeList convertDotIndex(DottedVariableNode dottedVar) {
        var pair = DotConverter.exceptLastIndex(info, dottedVar, 1);
        if (IndexConverter.isSlice(pair.getValue())) {
            return convertSlice(pair.getKey(), (SliceNode) pair.getValue()[0]);
        } else {
            return convertIndex(pair.getKey(), pair.getValue());
        }
    }

    private void checkAccess(@NotNull TestConverter value, List<TestConverter> indices) {
        var retType = value.returnType()[0];
        var maybeOpInfo = retType.operatorInfo(OpSpTypeNode.DEL_ATTR, info.accessLevel(retType));
        if (maybeOpInfo.isEmpty()) {
            throw CompilerException.format(
                    "Delete cannot be called on an index with a variable of type '%s'" +
                            "('%s' has no usable operator del[])",
                    node, retType.name(), retType.name()
            );
        } else {
            var opInfo = maybeOpInfo.orElseThrow();
            List<Argument> tempArgs = new ArrayList<>();
            for (var index : indices) {
                tempArgs.add(new Argument("", index.returnType()[0]));
            }
            var args = tempArgs.toArray(new Argument[0]);
            if (!opInfo.matches(args)) {
                throw typeMismatchError(args, retType, opInfo);
            }
        }
    }

    private CompilerException typeMismatchError(Argument[] args, TypeObject retType, FunctionInfo opInfo) {
        if (args.length == 1) {
            throw CompilerException.format(
                    "operator del[] on type '%s' does not accept '%s' as an index type",
                    node, retType.name(), args[0].getType().name()
            );
        } else {
            var argsString = String.join(", ", TypeObject.name(Argument.typesOf(args)));
            var nameArr = TypeObject.name(Argument.typesOf(opInfo.getArgs().getNormalArgs()));
            var expectedStr = String.join(", ", nameArr);
            throw CompilerException.format(
                    "operator del[] on type '%s' expected types [%s], got [%s]",
                    node, argsString, expectedStr
            );
        }
    }
}
