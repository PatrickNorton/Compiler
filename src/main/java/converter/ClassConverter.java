package main.java.converter;

import main.java.converter.classbody.ConverterHolder;
import main.java.parser.ClassDefinitionNode;
import main.java.parser.DescriptorNode;
import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

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
        var descriptors = node.getDescriptors();
        var isFinal = !descriptors.contains(DescriptorNode.NONFINAL);
        var hasType = info.hasType(node.strName());
        StdTypeObject type;
        if (!hasType) {
            var trueSupers = convertSupers(supers);
            var generics = GenericInfo.parse(info, node.getName().getSubtypes());
            type = new StdTypeObject(node.getName().strName(), List.of(trueSupers), generics, isFinal);
            ensureProperInheritance(type, trueSupers);
            info.addType(type);
            var isConst = node.getDescriptors().contains(DescriptorNode.CONST);
            if (!isConst) {
                checkConstSupers(type, Arrays.asList(trueSupers));
            }
            parseIntoObject(converter, type, isConst);
            generics.setParent(type);
        } else {
            type = (StdTypeObject) info.getTypeObj(node.strName());
            try {
                info.accessHandler().addCls(type);
                info.accessHandler().addSuper(superType(type));
                info.addLocalTypes(type.getGenericInfo().getParamMap());
                parseStatements(converter);
            } finally {
                info.accessHandler().removeCls();
                info.accessHandler().removeSuper();
                info.removeLocalTypes();
            }
        }
        var superConstants = getSuperConstants(type);
        checkContract(type, type.getSupers());
        if (hasType) {
            putInInfo(type, "class", superConstants, converter);
        } else {
            addToInfo(type, "class", superConstants, converter);
        }
        return Collections.emptyList();
    }

    private static boolean classIsConstant(@NotNull ConverterHolder holder) {
        for (var info : holder.getVars().values()) {
            if (info.getMutType() != MutableType.STANDARD) {
                return false;
            }
        }
        for (var info : holder.getMethods().values()) {
            if (info.isMut()) {
                return false;
            }
        }
        for (var pair : holder.getOperators().entrySet()) {
            var info = pair.getValue();
            if (info.isMut() && pair.getKey() != OpSpTypeNode.NEW) {
                return false;
            }
        }
        for (var info : holder.getProperties().values()) {
            if (info.getMutType() != MutableType.STANDARD) {
                return false;
            }
        }
        return true;
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

    public static int completeType(CompilerInfo info, ClassDefinitionNode node, StdTypeObject obj) {
        return new ClassConverter(info, node).completeType(obj);
    }

    private int completeType(@NotNull StdTypeObject obj) {
        var converter = new ConverterHolder(info);
        obj.getGenericInfo().reParse(info, node.getName().getSubtypes());
        obj.getGenericInfo().setParent(obj);
        var supers = convertSupers(info.typesOf(node.getSuperclasses()));
        obj.setSupers(Arrays.asList(supers));
        ensureProperInheritance(obj, supers);
        var isConst = node.getDescriptors().contains(DescriptorNode.CONST);
        if (!isConst) {
            checkConstSupers(obj, obj.getSupers());
        }
        parseIntoObject(converter, obj, isConst);
        return info.reserveClass(obj);
    }

    private TypeObject superType(@NotNull StdTypeObject obj) {
        var supers = obj.getSupers();
        if (supers.isEmpty()) {
            return Builtins.OBJECT;
        }
        return supers.get(0);
    }

    private void parseIntoObject(ConverterHolder converter, @NotNull StdTypeObject obj, boolean isConst) {
        try {
            info.accessHandler().addCls(obj);
            info.accessHandler().addSuper(superType(obj));
            info.addLocalTypes(obj.getGenericInfo().getParamMap());
            parseInner(converter, obj, isConst);
        } finally {
            info.accessHandler().removeCls();
            info.accessHandler().removeSuper();
            info.removeLocalTypes();
        }
    }

    private void parseInner(ConverterHolder converter, @NotNull StdTypeObject obj, boolean isConst) {
        parseStatements(converter);
        if (isConst) {
            if (!classIsConstant(converter)) {
                throw CompilerException.format("Cannot make class '%s' const", node, obj.name());
            }
            obj.isConstClass();
        }
        obj.setOperators(converter.getOperatorInfos());
        obj.setStaticOperators(converter.getStaticOperatorInfos());
        converter.checkAttributes();
        obj.setAttributes(converter.allAttrs());
        obj.setStaticAttributes(converter.staticAttrs());
        obj.seal();
    }
}
