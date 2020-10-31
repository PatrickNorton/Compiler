package main.java.converter;

import main.java.parser.DeleteStatementNode;
import main.java.parser.IndexNode;
import main.java.parser.OpSpTypeNode;
import main.java.parser.VariableNode;

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
    public List<Byte> convert(int start) {
        List<Byte> bytes = new ArrayList<>();
        var deleted = node.getDeleted();
        if (deleted instanceof IndexNode) {
            var delIndex = (IndexNode) deleted;
            var varConverter = TestConverter.of(info, delIndex.getVar(), 1);
            List<TestConverter> indexConverters = new ArrayList<>();
            for (var index : delIndex.getIndices()) {
                indexConverters.add(TestConverter.of(info, index, 1));
            }
            checkAccess(varConverter, indexConverters);
            bytes.addAll(varConverter.convert(start + bytes.size()));
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
        } else if (deleted instanceof VariableNode) {  // TODO: Make variable inaccessible
            var delVar = (VariableNode) deleted;
            var index = info.varIndex(delVar);
            bytes.add(Bytecode.LOAD_NULL.value);
            bytes.add(Bytecode.STORE.value);  // Drops value currently stored
            bytes.addAll(Util.shortToBytes(index));
        } else {
            throw CompilerException.of(
                    "'del' statement can only be used on a variable or an indexed statement", node
            );
        }
        return bytes;
    }

    private void checkAccess(TestConverter value, List<TestConverter> indices) {
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
    }
}
