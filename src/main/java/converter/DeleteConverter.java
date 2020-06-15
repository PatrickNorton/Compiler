package main.java.converter;

import main.java.parser.DeleteStatementNode;
import main.java.parser.IndexNode;
import main.java.parser.OpSpTypeNode;
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
        List<Byte> bytes = new ArrayList<>();
        var deleted = node.getDeleted();
        if (deleted instanceof IndexNode) {
            var delIndex = (IndexNode) deleted;
            var varConverter = TestConverter.of(info, delIndex.getVar(), 1);
            var indexConverter = TestConverter.of(info, delIndex.getIndices()[0], 1);
            checkAccess(varConverter, indexConverter);
            bytes.addAll(varConverter.convert(start + bytes.size()));
            bytes.addAll(indexConverter.convert(start + bytes.size()));
            // TODO: Multiple params
            bytes.add(Bytecode.DEL_SUBSCRIPT.value);
        } else if (deleted instanceof VariableNode) {  // TODO: Make variable inaccessible
            var delVar = (VariableNode) deleted;
            var index = info.varIndex(delVar.getName());
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

    private void checkAccess(@NotNull TestConverter converter, @NotNull TestConverter index) {
        var retType = converter.returnType()[0];
        var indexType = index.returnType()[0];
        var opInfo = retType.operatorInfo(OpSpTypeNode.DEL_ATTR, info.accessLevel(retType));
        if (opInfo == null) {
            throw CompilerException.format(
                    "Delete cannot be called on an index with a variable of type '%s'" +
                            "('%s' has no usable operator del[])",
                    node, retType.name(), retType.name()
            );
        } else if (opInfo.matches(new Argument("", indexType))) {
            throw CompilerException.format(
                    "operator del[] on type '%s' does not accept '%s' as an index type",
                    node, retType.name(), indexType.name()
            );
        }
    }
}
