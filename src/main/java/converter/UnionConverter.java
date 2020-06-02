package main.java.converter;

import main.java.converter.classbody.ConverterHolder;
import main.java.converter.classbody.MethodInfo;
import main.java.parser.DeclarationNode;
import main.java.parser.DeclaredAssignmentNode;
import main.java.parser.DescribableNode;
import main.java.parser.DescriptorNode;
import main.java.parser.IndependentNode;
import main.java.parser.LineInfo;
import main.java.parser.StatementBodyNode;
import main.java.parser.TestNode;
import main.java.parser.UnionDefinitionNode;
import main.java.parser.VariableNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class UnionConverter extends ClassConverterBase<UnionDefinitionNode> implements BaseConverter {
    private final Map<String, Pair<LineInfo, TypeObject>> variants = new HashMap<>();

    public UnionConverter(CompilerInfo info, UnionDefinitionNode node) {
        super(info, node);
    }

    @Override
    @NotNull
    @Unmodifiable
    public List<Byte> convert(int start) {
        var supers = info.typesOf(node.getSuperclasses());
        var converter = new ConverterHolder(info);
        var trueSupers = convertSupers(supers);
        var generics = GenericInfo.parse(info, node.getName().getSubtypes());
        StdTypeObject type;
        if (!info.hasType(node.strName())) {
            type = new StdTypeObject(node.getName().strName(), List.of(trueSupers), generics, true);
            ensureProperInheritance(type, trueSupers);
            info.addType(type);
            parseIntoObject(converter, type);
        } else {
            type = (StdTypeObject) info.getType(node.strName());
            parseStatements(converter);
        }
        List<Short> superConstants = new ArrayList<>();
        for (var sup : trueSupers) {
            superConstants.add(info.constIndex(sup.name()));
        }
        checkContract(type, trueSupers);
        addToInfo(type, "union", superConstants, converter);
        return Collections.emptyList();
    }

    protected void parseStatement(IndependentNode stmt, ConverterHolder converter) {
        if (stmt instanceof DeclarationNode) {
            var decl = (DeclarationNode) stmt;
            if (isStatic(decl)) {
                converter.attributes().parse(decl);
            } else {
                addVariant(decl);
            }
        } else if (stmt instanceof DeclaredAssignmentNode) {
            var decl = (DeclaredAssignmentNode) stmt;
            if (isStatic(decl)) {
                converter.attributes().parse(decl);
            } else {
                throw CompilerException.of("Non-static variables not allowed in unions", decl);
            }
        } else {
            super.parseStatement(stmt, converter);
        }
    }

    private void addVariant(@NotNull DeclarationNode decl) {
        var name = decl.getName().getName();
        var type = info.getType(decl.getType());
        if (variants.containsKey(name)) {
            throw CompilerException.doubleDef(name, variants.get(name).getKey(), decl.getLineInfo());
        } else {
            variants.put(name, Pair.of(decl.getLineInfo(), type));
        }
    }

    private boolean isStatic(@NotNull DescribableNode node) {
        return node.getDescriptors().contains(DescriptorNode.STATIC);
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

    public static void completeType(CompilerInfo info, UnionDefinitionNode node, StdTypeObject obj) {
        new UnionConverter(info, node).completeType(obj);
    }

    private void completeType(@NotNull StdTypeObject obj) {
        var converter = new ConverterHolder(info);
        parseIntoObject(converter, obj);
    }

    private void parseIntoObject(ConverterHolder converter, @NotNull StdTypeObject obj) {
        parseStatements(converter);
        converter.attributes().addUnionMethods(variantMethods());
        obj.setOperators(converter.getOperatorInfos());
        converter.checkAttributes();
        obj.setAttributes(withVariantInfos(converter.allAttrs()));
        obj.setStaticAttributes(withStaticVariants(converter.staticAttrs()));
        obj.seal();
    }

    @NotNull
    private Map<String, AttributeInfo> withVariantInfos(@NotNull Map<String, AttributeInfo> vars) {
        Map<String, AttributeInfo> result = new HashMap<>(vars.size() + variants.size());
        result.putAll(vars);
        for (var pair : variants.entrySet()) {
            var fnInfo = new OptionTypeObject(pair.getValue().getValue());
            result.put(pair.getKey(), new AttributeInfo(EnumSet.of(DescriptorNode.PUBLIC), fnInfo));
        }
        return result;
    }

    @NotNull
    private Map<String, AttributeInfo> withStaticVariants(@NotNull Map<String, AttributeInfo> vars) {
        Map<String, AttributeInfo> result = new HashMap<>(vars.size() + variants.size());
        result.putAll(vars);
        for (var pair : variants.entrySet()) {
            var fnInfo = Builtins.CALLABLE.generify(pair.getValue().getValue());
            result.put(pair.getKey(), new AttributeInfo(EnumSet.of(DescriptorNode.PUBLIC), fnInfo));
        }
        return result;
    }

    @NotNull
    private Map<String, MethodInfo> variantMethods() {
        Map<String, MethodInfo> result = new HashMap<>(variants.size());
        for (var pair : variants.entrySet()) {
            var fnInfo = new FunctionInfo(pair.getValue().getValue());
            var selfVar = new VariableNode(LineInfo.empty(), "self");
            var variantNo = 0;  // TODO: Get variant number
            var stmt = new VariantCreationNode(node.getLineInfo(), selfVar, pair.getKey(), variantNo, TestNode.empty());
            var body = new StatementBodyNode(LineInfo.empty(), stmt);
            var descriptors = EnumSet.of(DescriptorNode.PUBLIC);
            result.put(pair.getKey(), new MethodInfo(descriptors, fnInfo, body, node.getLineInfo()));
        }
        return result;
    }
}
