package main.java.converter;

import main.java.parser.DecrementNode;
import main.java.parser.DottedVariableNode;
import main.java.parser.IncDecNode;
import main.java.parser.IncrementNode;
import main.java.parser.IndexNode;
import main.java.parser.OpSpTypeNode;
import main.java.parser.VariableNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class IncrementDecrementConverter implements BaseConverter {
    private final IncDecNode node;
    private final CompilerInfo info;

    public IncrementDecrementConverter(CompilerInfo info, IncDecNode node) {
        this.node = node;
        this.info = info;
    }

    @NotNull
    @Override
    public final List<Byte> convert(int start) {
        boolean isDecrement = node instanceof DecrementNode;
        assert isDecrement ^ node instanceof IncrementNode;
        if (node.getVariable() instanceof VariableNode) {
            return convertVariable(start, isDecrement);
        } else if (node.getVariable() instanceof DottedVariableNode) {
            return convertDot(start, isDecrement);
        } else if (node.getVariable() instanceof IndexNode) {
            return convertIndex(start, isDecrement);
        } else {
            throw CompilerInternalError.format("Non-variable %s not yet implemented", node, incName(isDecrement));
        }
    }

    private List<Byte> convertVariable(int start, boolean isDecrement) {
        assert node.getVariable() instanceof VariableNode;
        var converter = TestConverter.of(info, node.getVariable(), 1);
        if (!Builtins.INT.isSuperclass(converter.returnType()[0])) {
            throw typeError(converter.returnType()[0], isDecrement);
        }
        List<Byte> bytes = new ArrayList<>(converter.convert(start));
        bytes.add(Bytecode.LOAD_CONST.value);
        int constIndex = info.addConstant(LangConstant.of(1));
        bytes.addAll(Util.shortToBytes((short) constIndex));
        bytes.add((isDecrement ? Bytecode.MINUS : Bytecode.PLUS).value);
        var variable = (VariableNode) node.getVariable();
        if (info.variableIsConstant(variable.getName())) {
            throw CompilerException.format("Cannot %s non-mut variable", node, incName(isDecrement));
        }
        short varIndex = info.varIndex(variable);
        bytes.add(Bytecode.STORE.value);
        bytes.addAll(Util.shortToBytes(varIndex));
        return bytes;
    }

    private List<Byte> convertDot(int start, boolean isDecrement) {
        assert node.getVariable() instanceof DottedVariableNode;
        var dot = (DottedVariableNode) node.getVariable();
        if (dot.getPostDots()[dot.getPostDots().length - 1].getPostDot() instanceof VariableNode) {
            var pair = DotConverter.exceptLast(info, dot, 1);
            var converter = pair.getKey();
            var name = pair.getValue();
            var retType = converter.returnType()[0];
            var attrInfo = retType.tryAttrType(node, name, info);
            if (!Builtins.INT.isSuperclass(attrInfo)) {
                throw typeError(attrInfo, isDecrement);
            }
            if (!retType.canSetAttr(name, info)) {
                throw CompilerException.format("Cannot %s non-mut attribute", node, incName(isDecrement));
            }
            List<Byte> bytes = new ArrayList<>(converter.convert(start));
            bytes.add(Bytecode.DUP_TOP.value);
            bytes.add(Bytecode.LOAD_DOT.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(name))));
            bytes.add(Bytecode.LOAD_CONST.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(1))));
            bytes.add((isDecrement ? Bytecode.MINUS : Bytecode.PLUS).value);
            bytes.add(Bytecode.STORE_ATTR.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(name))));
            return bytes;
        } else {
            throw CompilerTodoError.format("Cannot convert index %s yet", node, incName(isDecrement));
        }
    }

    private List<Byte> convertIndex(int start, boolean isDecrement) {
        assert node.getVariable() instanceof IndexNode;
        var index = (IndexNode) node.getVariable();
        var converter = TestConverter.of(info, index.getVar(), 1);
        var indices = index.getIndices();
        checkIndex(converter.returnType()[0], isDecrement);
        var bytes = IndexConverter.convertDuplicate(start, converter, indices, info);
        bytes.addAll(Util.intToBytes((short) indices.length));
        bytes.add(Bytecode.LOAD_CONST.value);
        bytes.addAll(Util.shortToBytes(info.constIndex(LangConstant.of(1))));
        bytes.add(Bytecode.PLUS.value);
        bytes.add(Bytecode.STORE_SUBSCRIPT.value);
        bytes.addAll(Util.shortToBytes((short) (indices.length + 1)));
        return bytes;
    }

    private CompilerException typeError(TypeObject retType, boolean isDecrement) {
        return CompilerException.format(
                    "TypeError: Object of type %s cannot be %sed",
                    node.getLineInfo(), retType.name(), incName(isDecrement)
        );
    }

    private void checkIndex(TypeObject indexType, boolean isDecrement) {
        if (indexType.operatorInfo(OpSpTypeNode.GET_ATTR, info).isEmpty()
                || indexType.operatorInfo(OpSpTypeNode.SET_ATTR, info).isEmpty()) {
            throw typeError(indexType, isDecrement);
        }
    }

    private static String incName(boolean isDecrement) {
        return isDecrement ? "decrement" : "increment";
    }
}
