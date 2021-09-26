package main.java.converter;

import main.java.converter.bytecode.ArgcBytecode;
import main.java.converter.bytecode.ConstantBytecode;
import main.java.converter.bytecode.VariableBytecode;
import main.java.parser.DecrementNode;
import main.java.parser.DottedVariableNode;
import main.java.parser.FunctionCallNode;
import main.java.parser.IncDecNode;
import main.java.parser.IncrementNode;
import main.java.parser.IndexNode;
import main.java.parser.OpSpTypeNode;
import main.java.parser.SpecialOpNameNode;
import main.java.parser.TestNode;
import main.java.parser.VariableNode;
import org.jetbrains.annotations.NotNull;

public final class IncrementDecrementConverter implements BaseConverter {
    private final IncDecNode node;
    private final CompilerInfo info;

    public IncrementDecrementConverter(CompilerInfo info, IncDecNode node) {
        this.node = node;
        this.info = info;
    }

    @NotNull
    @Override
    public BytecodeList convert() {
        boolean isDecrement = node instanceof DecrementNode;
        assert isDecrement ^ node instanceof IncrementNode;
        if (node.getVariable() instanceof VariableNode) {
            return convertVariable(isDecrement);
        } else if (node.getVariable() instanceof DottedVariableNode) {
            return convertDot(isDecrement);
        } else if (node.getVariable() instanceof IndexNode) {
            return convertIndex(isDecrement);
        } else {
            throw CompilerInternalError.format("Non-variable %s not yet implemented", node, incName(isDecrement));
        }
    }

    @NotNull
    private BytecodeList convertVariable(boolean isDecrement) {
        assert node.getVariable() instanceof VariableNode;
        var converter = TestConverter.of(info, node.getVariable(), 1);
        if (!Builtins.intType().isSuperclass(converter.returnType()[0])) {
            throw typeError(converter.returnType()[0], isDecrement);
        }
        var bytes = new BytecodeList(converter.convert());
        bytes.loadConstant(LangConstant.of(1), info);
        bytes.add(isDecrement ? Bytecode.MINUS : Bytecode.PLUS);
        var variable = (VariableNode) node.getVariable();
        if (info.variableIsImmutable(variable.getName())) {
            throw CompilerException.format("Cannot %s non-mut variable", node, incName(isDecrement));
        }
        bytes.add(Bytecode.STORE, new VariableBytecode(info.varIndex(variable)));
        return bytes;
    }

    private BytecodeList convertDot(boolean isDecrement) {
        assert node.getVariable() instanceof DottedVariableNode;
        var dot = (DottedVariableNode) node.getVariable();
        var last = dot.getLast().getPostDot();
        if (last instanceof VariableNode) {
            return convertDotVar(dot, isDecrement);
        } else if (last instanceof IndexNode) {
            return convertDotIndex(dot, isDecrement);
        } else if (last instanceof FunctionCallNode) {
            throw CompilerException.format("Cannot %s result of function call", node, incName(isDecrement));
        } else if (last instanceof SpecialOpNameNode) {
            throw CompilerException.format("Cannot %s raw operator", node, incName(isDecrement));
        } else {
            throw CompilerInternalError.format("Unknown type for dotted %s", node, incName(isDecrement));
        }
    }

    @NotNull
    private BytecodeList convertDotVar(DottedVariableNode dot, boolean isDecrement) {
        assert node.getVariable() == dot;
        var pair = DotConverter.exceptLast(info, dot, 1);
        var converter = pair.getKey();
        var name = pair.getValue();
        var retType = converter.returnType()[0];
        var attrInfo = retType.tryAttrType(node, name, info);
        if (!Builtins.intType().isSuperclass(attrInfo)) {
            throw typeError(attrInfo, isDecrement);
        }
        if (!retType.canSetAttr(name, info)) {
            throw CompilerException.format("Cannot %s non-mut attribute", node, incName(isDecrement));
        }
        var bytes = new BytecodeList(converter.convert());
        bytes.add(Bytecode.DUP_TOP);
        bytes.add(Bytecode.LOAD_DOT, new ConstantBytecode(LangConstant.of(name), info));
        bytes.loadConstant(LangConstant.of(1), info);
        bytes.add(isDecrement ? Bytecode.MINUS : Bytecode.PLUS);
        bytes.add(Bytecode.STORE_ATTR, new ConstantBytecode(LangConstant.of(name), info));
        return bytes;
    }

    @NotNull
    private BytecodeList convertDotIndex(DottedVariableNode dot, boolean isDecrement) {
        assert node.getVariable() == dot;
        var pair = DotConverter.exceptLastIndex(info, dot, 1);
        var converter = pair.getKey();
        var indices = pair.getValue();
        return finishIndex(isDecrement, converter, indices);
    }

    @NotNull
    private BytecodeList convertIndex(boolean isDecrement) {
        assert node.getVariable() instanceof IndexNode;
        var index = (IndexNode) node.getVariable();
        var converter = TestConverter.of(info, index.getVar(), 1);
        var indices = index.getIndices();
        return finishIndex(isDecrement, converter, indices);
    }

    @NotNull
    private BytecodeList finishIndex(boolean isDecrement, @NotNull TestConverter converter, TestNode[] indices) {
        checkIndex(converter.returnType()[0], isDecrement);
        var bytes = IndexConverter.convertDuplicate(converter, indices, info, indices.length);
        bytes.loadConstant(LangConstant.of(1), info);
        bytes.add(Bytecode.PLUS);
        bytes.add(Bytecode.STORE_SUBSCRIPT, new ArgcBytecode((short) (indices.length + 1)));
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
