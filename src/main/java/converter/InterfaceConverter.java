package main.java.converter;

import main.java.converter.classbody.ConverterHolder;
import main.java.parser.DescriptorNode;
import main.java.parser.GenericFunctionNode;
import main.java.parser.GenericOperatorNode;
import main.java.parser.IndependentNode;
import main.java.parser.InterfaceDefinitionNode;
import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
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
    @Unmodifiable
    public List<Byte> convert(int start) {
        this.genericOps.clear();
        this.genericAttrs.clear();
        var converter = new ConverterHolder(info);
        var trueSupers = convertSupers(info.typesOf(node.getSuperclasses()));
        var generics = GenericInfo.parse(info, node.getName().getSubtypes());
        InterfaceType type;
        if (!info.hasType(node.getName().strName())) {
            type = new InterfaceType(node.getName().strName(), generics, List.of(trueSupers));
            ensureProperInheritance(type, trueSupers);
            info.addType(type);
            parseIntoObject(converter, type);
            if (node.getDescriptors().contains(DescriptorNode.AUTO)) {
                throw CompilerException.of("Auto interfaces may only be defined at top level", node);
            }
        } else {
            type = (InterfaceType) info.getTypeObj(node.getName().strName());
            parseStatements(converter);
        }
        List<Short> superConstants = new ArrayList<>();
        for (var sup : trueSupers) {
            superConstants.add(info.constIndex(sup.name()));
        }
        converter.checkAttributes();
        addToInfo(type, "interface", superConstants, converter);
        return Collections.emptyList();
    }

    public static void completeType(CompilerInfo info, InterfaceDefinitionNode node, InterfaceType obj) {
        new InterfaceConverter(info, node).completeType(obj);
    }

    private void completeType(@NotNull InterfaceType obj) {
        var converter = new ConverterHolder(info);
        obj.getGenericInfo().reParse(info, node.getName().getSubtypes());
        try {
            info.accessHandler().addCls(obj);
            parseIntoObject(converter, obj);
        } finally {
            info.accessHandler().removeCls();
        }
        // Note: 'auto' interfaces should already be registered with the compiler, so no action is needed
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
    protected final void parseStatement(IndependentNode stmt, ConverterHolder converter) {
        if (stmt instanceof GenericOperatorNode) {
            var opNode = (GenericOperatorNode) stmt;
            this.genericOps.add(opNode.getOpCode().getOperator());
            converter.operators().parse(opNode);

        } else if (stmt instanceof GenericFunctionNode) {
            var fnNode = (GenericFunctionNode) stmt;
            this.genericAttrs.add(fnNode.getName().getName());
            converter.methods().parse(fnNode);
        } else {
            super.parseStatement(stmt, converter);
        }
    }
}
