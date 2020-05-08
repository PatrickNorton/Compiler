package main.java.converter;

import main.java.converter.classbody.AttributeConverter;
import main.java.converter.classbody.MethodConverter;
import main.java.converter.classbody.OperatorDefConverter;
import main.java.converter.classbody.PropertyConverter;
import main.java.parser.ClassDefinitionNode;
import main.java.parser.DescriptorNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
        var declarations = new AttributeConverter(info);
        var methods = new MethodConverter(info);
        var operators = new OperatorDefConverter(info);
        var properties = new PropertyConverter(info);
        var trueSupers = convertSupers(supers);
        var generics = GenericInfo.parse(info, node.getName().getSubtypes());
        var descriptors = node.getDescriptors();
        var isFinal = !descriptors.contains(DescriptorNode.NONFINAL);
        StdTypeObject type;
        if (!info.hasType(node.strName())) {
            type = new StdTypeObject(node.getName().strName(), List.of(trueSupers), generics, isFinal);
            ensureProperInheritance(type, trueSupers);
            info.addType(type);
            parseStatements(declarations, methods, operators, properties);
        } else {
            type = (StdTypeObject) info.getType(node.strName());
        }
        if (type.isFinal() && classIsConstant(declarations, methods, operators, properties)) {
            type.isConstClass();
        }
        type.setOperators(operators.getOperatorInfos());
        List<Short> superConstants = new ArrayList<>();
        for (var sup : trueSupers) {
            superConstants.add(info.constIndex(sup.name()));
        }
        checkAttributes(declarations.getVars(), declarations.getStaticVars(),
                methods.getMethods(), methods.getStaticMethods());
        type.setAttributes(allAttributes(declarations.getVars(), methods.getMethods(), properties.getProperties()));
        type.setStaticAttributes(allAttributes(declarations.getStaticVars(), methods.getStaticMethods(), new HashMap<>()));
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

    private static boolean classIsConstant(@NotNull AttributeConverter decls, @NotNull MethodConverter methods,
                                           @NotNull OperatorDefConverter ops, @NotNull PropertyConverter props) {
        for (var info : decls.getVars().values()) {
            if (info.getDescriptors().contains(DescriptorNode.MUT)) {
                return false;
            }
        }
        for (var info : methods.getMethods().values()) {
            if (info.getDescriptors().contains(DescriptorNode.MUT)) {
                return false;
            }
        }
        for (var info : ops.getOperators().values()) {
            if (info.getDescriptors().contains(DescriptorNode.MUT)) {
                return false;
            }
        }
        for (var info : props.getProperties().values()) {
            if (info.getDescriptors().contains(DescriptorNode.MUT)) {
                return false;
            }
        }
        return true;
    }

    public static void completeType(CompilerInfo info, ClassDefinitionNode node, StdTypeObject obj) {
        new ClassConverter(info, node).completeType(obj);
    }

    private void completeType(@NotNull StdTypeObject obj) {
        var declarations = new AttributeConverter(info);
        var methods = new MethodConverter(info);
        var operators = new OperatorDefConverter(info);
        var properties = new PropertyConverter(info);
        parseStatements(declarations, methods, operators, properties);
        if (obj.isFinal() && classIsConstant(declarations, methods, operators, properties)) {
            obj.isConstClass();
        }
        obj.setOperators(operators.getOperatorInfos());
        checkAttributes(declarations.getVars(), declarations.getStaticVars(),
                methods.getMethods(), methods.getStaticMethods());
        obj.setAttributes(allAttributes(declarations.getVars(), methods.getMethods(), properties.getProperties()));
        obj.seal();
    }
}
