package main.java.converter;

import main.java.converter.classbody.ConverterHolder;
import main.java.parser.ClassDefinitionNode;
import main.java.parser.DescriptorNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ClassConverter extends ClassConverterBase<ClassDefinitionNode> implements BaseConverter {
    public ClassConverter(CompilerInfo info, ClassDefinitionNode node) {
        super(info, node);
    }

    @NotNull
    @Override
    @Unmodifiable
    public List<Byte> convert(int start) {
        var supers = info.typesOf(node.getSuperclasses());
        var converter = new ConverterHolder(info);
        var trueSupers = convertSupers(supers);
        var generics = GenericInfo.parse(info, node.getName().getSubtypes());
        var descriptors = node.getDescriptors();
        var isFinal = !descriptors.contains(DescriptorNode.NONFINAL);
        StdTypeObject type;
        if (!info.hasType(node.strName())) {
            type = new StdTypeObject(node.getName().strName(), List.of(trueSupers), generics, isFinal);
            ensureProperInheritance(type, trueSupers);
            info.addType(type);
            var isConst = node.getDescriptors().contains(DescriptorNode.CONST);
            if (!isConst) {
                checkConstSupers(type, Arrays.asList(trueSupers));
            }
            parseIntoObject(converter, type, isConst);
        } else {
            type = (StdTypeObject) info.getTypeObj(node.strName());
            parseStatements(converter);
        }
        List<Short> superConstants = new ArrayList<>();
        for (var sup : trueSupers) {
            superConstants.add(info.constIndex(sup.name()));
        }
        checkContract(type, trueSupers);
        addToInfo(type, "class", superConstants, converter);
        return Collections.emptyList();
    }

    private static boolean classIsConstant(@NotNull ConverterHolder holder) {
        for (var info : holder.getVars().values()) {
            if (info.getDescriptors().contains(DescriptorNode.MUT)) {
                return false;
            }
        }
        for (var info : holder.getMethods().values()) {
            if (info.getDescriptors().contains(DescriptorNode.MUT)) {
                return false;
            }
        }
        for (var info : holder.getOperators().values()) {
            if (info.getDescriptors().contains(DescriptorNode.MUT)) {
                return false;
            }
        }
        for (var info : holder.getProperties().values()) {
            if (info.getDescriptors().contains(DescriptorNode.MUT)) {
                return false;
            }
        }
        return true;
    }

    private void checkContract(StdTypeObject type, @NotNull UserType<?>... supers) {
        for (var sup : supers) {
            var contract = sup.contract();
            for (var attr : contract.getKey()) {
                if (type.attrType(attr, DescriptorNode.PUBLIC) == null) {
                    throw CompilerException.format(
                            "Missing impl for method '%s' (defined by interface %s)",
                            node, attr, sup.name()
                    );
                }
            }
            for (var op : contract.getValue()) {
                if (type.operatorInfo(op, DescriptorNode.PUBLIC) == null) {
                    throw CompilerException.format(
                            "Missing impl for %s (defined by interface %s)",
                            node, op, sup.name()
                    );
                }
            }
        }
    }

    private void checkConstSupers(StdTypeObject type, @NotNull Iterable<TypeObject> supers) {
        for (var cls : supers) {
            if (cls instanceof StdTypeObject && ((StdTypeObject) cls).constSemantics()) {
                throw CompilerException.format(
                        "Class '%s' inherits from the const class '%s', but is not itself const",
                        node, type.name(), cls.name()
                );
            }
        }
    }

    public static void completeType(CompilerInfo info, ClassDefinitionNode node, StdTypeObject obj) {
        new ClassConverter(info, node).completeType(obj);
    }

    private void completeType(@NotNull StdTypeObject obj) {
        var converter = new ConverterHolder(info);
        obj.getGenericInfo().reParse(info, node.getName().getSubtypes());
        var isConst = node.getDescriptors().contains(DescriptorNode.CONST);
        if (!isConst) {
            checkConstSupers(obj, obj.getSupers());
        }
        try {
            info.accessHandler().addCls(obj);
            parseIntoObject(converter, obj, isConst);
        } finally {
            info.accessHandler().removeCls();
        }
    }

    private void parseIntoObject(ConverterHolder converter, @NotNull StdTypeObject obj, boolean isConst) {
        parseStatements(converter);
        if (isConst) {
            if (!classIsConstant(converter)) {
                throw CompilerException.format("Cannot make class '%s' const", node, obj.name());
            }
            obj.isConstClass();
        }
        obj.setOperators(converter.getOperatorInfos());
        converter.checkAttributes();
        obj.setAttributes(converter.allAttrs());
        obj.setStaticAttributes(converter.staticAttrs());
        obj.seal();
    }
}
