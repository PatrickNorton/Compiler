package main.java.converter;

import main.java.converter.classbody.AttributeConverter;
import main.java.converter.classbody.MethodConverter;
import main.java.converter.classbody.OperatorDefConverter;
import main.java.converter.classbody.PropertyConverter;
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
import java.util.HashMap;
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
        var declarations = new AttributeConverter(info);
        var methods = new MethodConverter(info);
        var operators = new OperatorDefConverter(info);
        var properties = new PropertyConverter(info);
        var trueSupers = convertSupers(info.typesOf(node.getSuperclasses()));
        InterfaceType type;
        if (!info.hasType(node.getName().strName())) {
            type = new InterfaceType(node.getName().strName(), List.of(trueSupers));
            ensureProperInheritance(type, trueSupers);
            info.addType(type);
            parseStatements(declarations, methods, operators, properties);
        } else {
            type = (InterfaceType) info.getType(node.getName().strName());
        }
        List<Short> superConstants = new ArrayList<>();
        for (var sup : trueSupers) {
            superConstants.add(info.constIndex(sup.name()));
        }
        checkAttributes(declarations.getVars(), declarations.getStaticVars(),
                methods.getMethods(), methods.getStaticMethods());
        type.setOperators(operators.getOperatorInfos(), genericOps);
        type.setAttributes(
                allAttributes(declarations.getVars(), methods.getMethods(), properties.getProperties()),
                genericAttrs
        );
        type.setStaticAttributes(
                allAttributes(declarations.getStaticVars(), methods.getStaticMethods(), new HashMap<>()),
                new HashSet<>()
        );
        type.seal();
        var cls = new ClassInfo.Factory()
                .setType(type)
                .setSuperConstants(superConstants)
                .setVariables(declarations.varsWithInts())
                .setStaticVariables(declarations.staticVarsWithInts())
                .setOperatorDefs(convert(type, operators.getOperators()))
                .setStaticOperators(new HashMap<>())
                .setMethodDefs(convert(type, methods.getMethods()))
                .setStaticMethods(convert(type, methods.getStaticMethods()))
                .setProperties(merge(convert(type, properties.getGetters()), convert(type, properties.getSetters())))
                .create();
        int classIndex = info.addClass(cls);
        var name = node.getName().strName();
        if (Builtins.FORBIDDEN_NAMES.contains(name)) {
            throw CompilerException.format("Illegal name for class '%s'", node.getName(), name);
        }
        info.checkDefinition(name, node);
        info.addVariable(name, Builtins.TYPE.generify(type), new ClassConstant(name, classIndex), node);
        return Collections.emptyList();
    }

    public static void completeType(CompilerInfo info, InterfaceDefinitionNode node, InterfaceType obj) {
        new InterfaceConverter(info, node).completeType(obj);
    }

    private void completeType(@NotNull InterfaceType obj) {
        var declarations = new AttributeConverter(info);
        var methods = new MethodConverter(info);
        var operators = new OperatorDefConverter(info);
        var properties = new PropertyConverter(info);
        parseStatements(declarations, methods, operators, properties);
        obj.setOperators(operators.getOperatorInfos(), genericOps);
        checkAttributes(declarations.getVars(), declarations.getStaticVars(),
                methods.getMethods(), methods.getStaticMethods());
        obj.setAttributes(
                allAttributes(declarations.getVars(), methods.getMethods(), properties.getProperties()),
                genericAttrs
        );
        obj.seal();
    }

    @Override
    protected final void parseStatement(
            IndependentNode stmt,
            AttributeConverter declarations,
            MethodConverter methods,
            OperatorDefConverter operators,
            PropertyConverter properties
    ) {
        if (stmt instanceof GenericOperatorNode) {
            var opNode = (GenericOperatorNode) stmt;
            this.genericOps.add(opNode.getOpCode().getOperator());
            operators.parse(opNode);

        } else if (stmt instanceof GenericFunctionNode) {
            var fnNode = (GenericFunctionNode) stmt;
            this.genericAttrs.add(fnNode.getName().getName());
            methods.parse(fnNode);
        } else {
            super.parseStatement(stmt, declarations, methods, operators, properties);
        }
    }
}
