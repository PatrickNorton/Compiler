package main.java.converter;

import main.java.converter.classbody.ConverterHolder;
import main.java.converter.classbody.Method;
import main.java.parser.DeclarationNode;
import main.java.parser.DeclaredAssignmentNode;
import main.java.parser.DescribableNode;
import main.java.parser.DescriptorNode;
import main.java.parser.IndependentNode;
import main.java.parser.LineInfo;
import main.java.parser.ReturnStatementNode;
import main.java.parser.StatementBodyNode;
import main.java.parser.TestListNode;
import main.java.parser.TestNode;
import main.java.parser.UnionDefinitionNode;
import main.java.parser.VariableNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class UnionConverter extends ClassConverterBase<UnionDefinitionNode> implements BaseConverter {
    private final Map<String, Pair<Integer, AttributeInfo>> variants = new HashMap<>();

    public UnionConverter(CompilerInfo info, UnionDefinitionNode node) {
        super(info, node);
    }

    @Override
    @NotNull
    @Unmodifiable
    public List<Byte> convert(int start) {
        var converter = new ConverterHolder(info);
        var hasType = info.hasType(node.strName());
        UnionTypeObject type;
        if (!hasType) {
            var supers = info.typesOf(node.getSuperclasses());
            var trueSupers = convertSupers(supers);
            var generics = GenericInfo.parse(info, node.getName().getSubtypes());
            type = new UnionTypeObject(node.getName().strName(), List.of(trueSupers), generics);
            ensureProperInheritance(type, trueSupers);
            info.addType(type);
            var isConst = node.getDescriptors().contains(DescriptorNode.CONST);
            if (!isConst) {
                checkConstSupers(type, Arrays.asList(trueSupers));
            }
            parseIntoObject(converter, type, isConst);
        } else {
            type = (UnionTypeObject) info.getTypeObj(node.strName());
            info.accessHandler().addCls(type);
            info.addLocalTypes(type, type.getGenericInfo().getParamMap());
            parseStatements(converter);
            info.removeLocalTypes();
            info.accessHandler().removeCls();
        }
        if (node.getDescriptors().contains(DescriptorNode.NONFINAL)) {
            throw CompilerException.of("Union may not be nonfinal", node);
        }
        var superConstants = getSuperConstants(type);
        checkContract(type, type.getSupers());
        if (hasType) {
            putInInfo(type, "union", convertVariants(), superConstants, converter);
        } else {
            addToInfo(type, "union", convertVariants(), superConstants, converter);
        }
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
            throw CompilerException.doubleDef(name, variants.get(name).getValue(), decl);
        } else {
            variants.put(name, Pair.of(variants.size(), new AttributeInfo(type, decl.getLineInfo())));
        }
    }

    private boolean isStatic(@NotNull DescribableNode node) {
        return node.getDescriptors().contains(DescriptorNode.STATIC);
    }

    public static int completeType(CompilerInfo info, UnionDefinitionNode node, UnionTypeObject obj) {
        return new UnionConverter(info, node).completeType(obj);
    }

    private int completeType(@NotNull UnionTypeObject obj) {
        var converter = new ConverterHolder(info);
        var supers = convertSupers(info.typesOf(node.getSuperclasses()));
        var isConst = node.getDescriptors().contains(DescriptorNode.CONST);
        if (isConst) {
            checkConstSupers(obj, Arrays.asList(supers));
        }
        obj.setSupers(Arrays.asList(supers));
        ensureProperInheritance(obj, supers);
        try {
            info.accessHandler().addCls(obj);
            info.addLocalTypes(obj, obj.getGenericInfo().getParamMap());
            parseIntoObject(converter, obj, isConst);
        } finally {
            info.accessHandler().removeCls();
            info.removeLocalTypes();
        }
        return info.reserveClass(obj);
    }

    private void parseIntoObject(ConverterHolder converter, @NotNull UnionTypeObject obj, boolean isConst) {
        info.addLocalTypes(obj, obj.getGenericInfo().getParamMap());
        if (isConst) {
            if (!classIsConstant(converter)) {
                throw CompilerException.format("Cannot make union '%s' const", node, obj.name());
            }
            obj.isConstClass();
        }
        parseStatements(converter);
        converter.attributes().addUnionMethods(variantMethods(obj));
        obj.setOperators(converter.getOperatorInfos());
        obj.setStaticOperators(converter.getStaticOperatorInfos());
        converter.checkAttributes();
        obj.setAttributes(withVariantInfos(converter.allAttrs()));
        obj.setStaticAttributes(withStaticVariants(converter.staticAttrs(), obj));
        obj.setVariants(getVariants());
        obj.seal();
    }

    @NotNull
    private Map<String, AttributeInfo> withVariantInfos(@NotNull Map<String, AttributeInfo> vars) {
        Map<String, AttributeInfo> result = new HashMap<>(vars.size() + variants.size());
        result.putAll(vars);
        for (var pair : variants.entrySet()) {
            var fnInfo = TypeObject.optional(pair.getValue().getValue().getType());
            result.put(pair.getKey(), new AttributeInfo(AccessLevel.PUBLIC, fnInfo));
        }
        return result;
    }

    @NotNull
    private Map<String, AttributeInfo> withStaticVariants(
            @NotNull Map<String, AttributeInfo> vars, UnionTypeObject selfType
    ) {
        Map<String, AttributeInfo> result = new HashMap<>(vars.size() + variants.size());
        result.putAll(vars);
        for (var pair : variants.entrySet()) {
            var fnInfo = variantInfo(pair.getValue().getValue().getType(), selfType).toCallable();
            result.put(pair.getKey(), new AttributeInfo(AccessLevel.PUBLIC, fnInfo));
        }
        return result;
    }

    @NotNull
    private List<String> convertVariants() {
        List<String> result = new ArrayList<>(Collections.nCopies(variants.size(), null));
        for (var pair : variants.entrySet()) {
            assert result.get(pair.getValue().getKey()) == null;
            result.set(pair.getValue().getKey(), pair.getKey());
        }
        assert !result.contains(null);
        return result;
    }

    private List<Pair<String, TypeObject>> getVariants() {
        List<Pair<String, TypeObject>> result = new ArrayList<>(Collections.nCopies(variants.size(), null));
        for (var pair : variants.entrySet()) {
            int index = pair.getValue().getKey();
            var name = pair.getKey();
            var type = pair.getValue().getValue().getType();
            assert result.get(index) == null;
            result.set(index, Pair.of(name, type));
        }
        return result;
    }

    private static final String VARIANT_NAME = "val";

    @NotNull
    private Map<String, Method> variantMethods(UnionTypeObject selfType) {
        Map<String, Method> result = new HashMap<>(variants.size());
        for (var pair : variants.entrySet()) {
            var fnInfo = variantInfo(pair.getValue().getValue().getType(), selfType);
            var selfVar = new VariableNode(LineInfo.empty(), selfType.name());
            var variantNo = pair.getValue().getKey();
            var variantVal = new VariableNode(LineInfo.empty(), VARIANT_NAME);
            var stmt = new VariantCreationNode(node.getLineInfo(), selfVar, pair.getKey(), variantNo, variantVal);
            var list = new TestListNode(stmt);
            var retStmt = new ReturnStatementNode(node.getLineInfo(), list, TestNode.empty());
            var body = new StatementBodyNode(LineInfo.empty(), retStmt);
            result.put(pair.getKey(), new Method(AccessLevel.PUBLIC, fnInfo, body, node.getLineInfo()));
        }
        return result;
    }

    @NotNull
    private FunctionInfo variantInfo(TypeObject val, UnionTypeObject type) {
        if (val.sameBaseType(Builtins.NULL_TYPE)) {
            return new FunctionInfo(type.makeMut());
        } else {
            var arg = new Argument(VARIANT_NAME, val);
            return new FunctionInfo(new ArgumentInfo(arg), type.makeMut());
        }
    }

    private void checkConstSupers(UnionTypeObject type, @NotNull Iterable<TypeObject> supers) {
        for (var cls : supers) {
            if (cls instanceof UserType<?> && ((UserType<?>) cls).constSemantics()) {
                throw CompilerException.format(
                        "Union '%s' inherits from the const class '%s', but is not itself const",
                        node, type.name(), cls.name()
                );
            }
        }
    }

    private static boolean classIsConstant(@NotNull ConverterHolder holder) {
        for (var info : holder.getVars().values()) {
            if (info.getMutType() != MutableType.STANDARD) {
                return false;
            }
        }
        return true;
    }
}
