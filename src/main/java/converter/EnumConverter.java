package main.java.converter;

import main.java.converter.classbody.ConverterHolder;
import main.java.converter.classbody.RawMethod;
import main.java.parser.DescriptorNode;
import main.java.parser.EnumDefinitionNode;
import main.java.parser.FunctionCallNode;
import main.java.parser.OpSpTypeNode;
import main.java.parser.StatementBodyNode;
import main.java.parser.VariableNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public final class EnumConverter extends ClassConverterBase<EnumDefinitionNode> implements BaseConverter {
    public EnumConverter(CompilerInfo info, EnumDefinitionNode node) {
        super(info, node);
    }

    @Override
    @NotNull
    public List<Byte> convert(int start) {
        var converter = new ConverterHolder(info);
        var trueSupers = convertSupers(info.typesOf(node.getSuperclasses()));
        var hasType = info.hasType(node.getName().strName());
        StdTypeObject type;
        if (!hasType) {
            type = new StdTypeObject(node.getName().strName(), List.of(trueSupers), GenericInfo.empty(), true);
            ensureProperInheritance(type, trueSupers);
            info.addType(type);
            parseIntoObject(converter, type);
        } else {
            type = (StdTypeObject) info.getTypeObj(node.getName().strName());
            parseStatements(converter);
        }
        if (node.getDescriptors().contains(DescriptorNode.NONFINAL)) {
            throw CompilerException.of("Enum class may not be nonfinal", node);
        }
        List<Short> superConstants = new ArrayList<>();
        for (var sup : trueSupers) {
            superConstants.add(info.constIndex(sup.name()));
        }
        if (!converter.getOperators().containsKey(OpSpTypeNode.NEW)) {
            converter.getOperators().put(OpSpTypeNode.NEW, defaultNew());
        }
        if (hasType) {
            putInInfo(type, "enum", superConstants, converter);
        } else {
            addToInfo(type, "enum", superConstants, converter);
        }
        return getInitBytes(start, converter.getOperators().get(OpSpTypeNode.NEW));
    }

    @NotNull
    private List<Byte> getInitBytes(int start, RawMethod newOperatorInfo) {
        List<Byte> bytes = new ArrayList<>();
        bytes.add(Bytecode.DO_STATIC.value);
        int doStaticPos = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        bytes.add(Bytecode.LOAD_CONST.value);
        bytes.addAll(Util.shortToBytes(info.constIndex(node.getName().strName())));
        for (var name : node.getNames()) {
            bytes.add(Bytecode.DUP_TOP.value);
            if (name instanceof VariableNode) {
                if (!newOperatorInfo.getInfo().matches()) {
                    throw CompilerException.of(
                            "Incorrect number of arguments for enum " +
                                    "(parentheses may only be omitted when enum constructor may take 0 arguments)",
                            name
                    );
                }
                bytes.add(Bytecode.DUP_TOP.value);
                bytes.add(Bytecode.CALL_TOS.value);
                bytes.addAll(Util.shortZeroBytes());
            } else if (name instanceof FunctionCallNode) {
                if (!newOperatorInfo.getInfo().matches()) {
                    throw CompilerException.of(
                            "Invalid arguments for enum constructor",
                            name
                    );
                }
                var fnNode = (FunctionCallNode) name;
                bytes.add(Bytecode.DUP_TOP.value);
                for (var arg : fnNode.getParameters()) {
                    bytes.addAll(TestConverter.bytes(start + bytes.size(), arg.getArgument(), info, 1));
                }
                bytes.add(Bytecode.CALL_TOS.value);
                bytes.addAll(Util.shortToBytes((short) fnNode.getParameters().length));
            } else {
                throw CompilerInternalError.format(
                        "Node of type %s not a known EnumKeywordNode", name, name.getClass()
                );
            }
            bytes.add(Bytecode.STORE_ATTR.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(new StringConstant(name.getVariable().getName()))));
        }
        bytes.add(Bytecode.POP_TOP.value);
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), doStaticPos);
        return bytes;
    }

    public static int completeType(CompilerInfo info, EnumDefinitionNode node, StdTypeObject obj) {
        return new EnumConverter(info, node).completeType(obj);
    }

    private int completeType(@NotNull StdTypeObject obj) {
        var converter = new ConverterHolder(info);
        obj.getGenericInfo().reParse(info, node.getName().getSubtypes());
        try {
            info.accessHandler().addCls(obj);
            parseIntoObject(converter, obj);
        } finally {
            info.accessHandler().removeCls();
        }
        return info.reserveClass(obj);
    }

    private void parseIntoObject(ConverterHolder converter, @NotNull StdTypeObject obj) {
        parseStatements(converter);
        obj.isConstClass();
        converter.addEnumStatics(Arrays.asList(node.getNames()), obj);
        var operatorInfos = new HashMap<>(converter.getOperatorInfos());
        operatorInfos.remove(OpSpTypeNode.NEW);
        converter.checkAttributes();
        obj.setOperators(operatorInfos);
        obj.setStaticOperators(converter.getStaticOperatorInfos());
        obj.setAttributes(converter.allAttrs());
        obj.setStaticAttributes(converter.staticAttrs());
        obj.seal();
    }

    private RawMethod defaultNew() {
        var fnInfo = new FunctionInfo("", ArgumentInfo.of());
        return new RawMethod(AccessLevel.PRIVATE, fnInfo, new StatementBodyNode(), node.getLineInfo());
    }
}
