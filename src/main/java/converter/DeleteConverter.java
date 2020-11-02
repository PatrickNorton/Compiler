package main.java.converter;

import main.java.parser.DeleteStatementNode;
import main.java.parser.IndexNode;
import main.java.parser.OpSpTypeNode;
import main.java.parser.SliceNode;
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
    public List<Byte> convert(int start) {
        var deleted = node.getDeleted();
        if (deleted instanceof IndexNode) {
            var del = (IndexNode) deleted;
            if (del.getIndices().length == 1 && del.getIndices()[0] instanceof SliceNode) {
                return convertSlice(start, del);
            } else {
                return convertIndex(start, del);
            }
        } else if (deleted instanceof VariableNode) {  // TODO: Make variable inaccessible
            return convertVariable((VariableNode) deleted);
        } else {
            throw CompilerException.of(
                    "'del' statement can only be used on a variable or an indexed statement", node
            );
        }
    }

    private List<Byte> convertIndex(int start, IndexNode delIndex) {
        var varConverter = TestConverter.of(info, delIndex.getVar(), 1);
        List<TestConverter> indexConverters = new ArrayList<>();
        for (var index : delIndex.getIndices()) {
            indexConverters.add(TestConverter.of(info, index, 1));
        }
        checkAccess(varConverter, indexConverters);
        List<Byte> bytes = new ArrayList<>(varConverter.convert(start));
        for (var converter : indexConverters) {
            bytes.addAll(converter.convert(start + bytes.size()));
        }
        if (indexConverters.size() == 1) {
            bytes.add(Bytecode.DEL_SUBSCRIPT.value);
        } else {
            bytes.add(Bytecode.CALL_OP.value);
            bytes.addAll(Util.shortToBytes((short) OpSpTypeNode.DEL_ATTR.ordinal()));
            bytes.addAll(Util.shortToBytes((short) indexConverters.size()));
        }
        return bytes;
    }

    private List<Byte> convertSlice(int start, IndexNode delIndex) {
        assert delIndex.getIndices().length == 1 && delIndex.getIndices()[0] instanceof SliceNode;
        var varConverter = TestConverter.of(info, delIndex.getVar(), 1);
        var sliceConverter = new SliceConverter(info, (SliceNode) delIndex.getIndices()[0]);
        var varType = varConverter.returnType()[0];
        var sliceInfo = varType.operatorInfo(OpSpTypeNode.DEL_SLICE, info).orElseThrow(
                () -> CompilerException.format(
                        "Object of type '%s' has no operator del[:]", delIndex.getVar(), varType.name()
                )
        );
        if (!sliceInfo.matches(new Argument("", Builtins.SLICE))) {
            throw CompilerException.format(
                    "Cannot call '%s'.operator del[:] (operator del[:] should always be callable with a slice)",
                    delIndex.getVar(), varType.name()
            );
        }
        List<Byte> bytes = new ArrayList<>(varConverter.convert(start));
        bytes.addAll(sliceConverter.convert(start + bytes.size()));
        bytes.add(Bytecode.CALL_OP.value);
        bytes.addAll(Util.shortToBytes((short) OpSpTypeNode.DEL_SLICE.ordinal()));
        bytes.addAll(Util.shortToBytes((short) 1));
        return bytes;
    }

    private List<Byte> convertVariable(VariableNode delVar) {
        List<Byte> bytes = new ArrayList<>();
        var index = info.varIndex(delVar);
        bytes.add(Bytecode.LOAD_NULL.value);
        bytes.add(Bytecode.STORE.value);  // Drops value currently stored
        bytes.addAll(Util.shortToBytes(index));
        return bytes;
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
