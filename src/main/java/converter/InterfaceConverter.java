package main.java.converter;

import main.java.converter.classbody.ConverterHolder;
import main.java.parser.DescriptorNode;
import main.java.parser.GenericFunctionNode;
import main.java.parser.GenericOperatorNode;
import main.java.parser.IndependentNode;
import main.java.parser.InterfaceDefinitionNode;
import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class InterfaceConverter extends ClassConverterBase<InterfaceDefinitionNode> implements BaseConverter {
    private final Set<OpSpTypeNode> genericOps;
    private final Set<String> genericAttrs;

    public InterfaceConverter(CompilerInfo info, InterfaceDefinitionNode node) {
        super(info, node);
        this.genericOps = EnumSet.noneOf(OpSpTypeNode.class);
        this.genericAttrs = new HashSet<>();
    }

    @Override
    @NotNull
    public BytecodeList convert() {
        this.genericOps.clear();
        this.genericAttrs.clear();
        var converter = new ConverterHolder(info);
        var trueSupers = convertSupers(info.typesOf(node.getSuperclasses()));
        InterfaceType type;
        var hasType = info.hasType(node.getName().strName());
        if (!hasType) {
            var generics = GenericInfo.parse(info, node.getName().getSubtypes());
            type = new InterfaceType(node.getName().strName(), generics, List.of(trueSupers));
            generics.setParent(type);
            ensureProperInheritance(type, trueSupers);
            info.addType(type);
            parseIntoObject(converter, type);
            if (node.getDescriptors().contains(DescriptorNode.AUTO)) {
                throw CompilerException.of("Auto interfaces may only be defined at top level", node);
            }
        } else {
            type = (InterfaceType) info.getTypeObj(node.getName().strName());
            try {
                info.accessHandler().addCls(type);
                info.addLocalTypes(type, type.getGenericInfo().getParamMap());
                parseStatements(converter);
            } finally {
                info.accessHandler().removeCls();
                info.removeLocalTypes();
            }
        }
        List<Short> superConstants = new ArrayList<>();
        for (var sup : trueSupers) {
            superConstants.add(info.constIndex(sup.baseName()));
        }
        converter.checkAttributes();
        if (hasType && !node.getDescriptors().contains(DescriptorNode.AUTO)) {
            putInInfo(type, "interface", superConstants, converter);
        } else {
            addToInfo(type, "interface", superConstants, converter);
        }
        return new BytecodeList();
    }

    public static void completeWithoutReserving(CompilerInfo info, InterfaceDefinitionNode node, InterfaceType obj) {
        new InterfaceConverter(info, node).completeWithoutReserving(obj);
    }

    public static int completeType(CompilerInfo info, InterfaceDefinitionNode node, InterfaceType obj) {
        return new InterfaceConverter(info, node).completeType(obj);
    }

    private void completeWithoutReserving(@NotNull InterfaceType obj) {
        var converter = new ConverterHolder(info);
        obj.getGenericInfo().reParse(info, node.getName().getSubtypes());
        obj.getGenericInfo().setParent(obj);
        try {
            info.accessHandler().addCls(obj);
            info.addLocalTypes(obj, obj.getGenericInfo().getParamMap());
            parseIntoObject(converter, obj);
        } finally {
            info.accessHandler().removeCls();
            info.removeLocalTypes();
        }
        // Note: 'auto' interfaces should already be registered with the compiler, so no action is needed
    }

    private int completeType(@NotNull InterfaceType obj) {
        completeWithoutReserving(obj);
        return info.reserveClass(obj);
    }

    private void parseIntoObject(ConverterHolder converter, @NotNull InterfaceType obj) {
        parseStatements(converter);
        converter.checkAttributes();
        obj.setOperators(converter.getOperatorInfos(), genericOps);
        obj.setStaticOperators(converter.getStaticOperatorInfos());
        obj.setAttributes(converter.allAttrs(), genericAttrs);
        obj.setStaticAttributes(converter.staticAttrs(), new HashSet<>());
        obj.seal();
    }

    @Override
    protected void parseStatement(IndependentNode stmt, ConverterHolder converter) {
        if (stmt instanceof GenericOperatorNode opNode) {
            this.genericOps.add(opNode.getOpCode().getOperator());
            converter.operators().parse(opNode);

        } else if (stmt instanceof GenericFunctionNode fnNode) {
            this.genericAttrs.add(fnNode.getName().getName());
            converter.methods().parse(fnNode);
        } else {
            super.parseStatement(stmt, converter);
        }
    }
}
