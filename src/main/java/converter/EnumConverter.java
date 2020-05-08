package main.java.converter;

import main.java.converter.classbody.AttributeConverter;
import main.java.converter.classbody.MethodConverter;
import main.java.converter.classbody.MethodInfo;
import main.java.converter.classbody.OperatorDefConverter;
import main.java.converter.classbody.PropertyConverter;
import main.java.parser.DescriptorNode;
import main.java.parser.EnumDefinitionNode;
import main.java.parser.EnumKeywordNode;
import main.java.parser.FunctionCallNode;
import main.java.parser.LineInfo;
import main.java.parser.OpSpTypeNode;
import main.java.parser.VariableNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EnumConverter extends ClassConverterBase<EnumDefinitionNode> implements BaseConverter {
    public EnumConverter(CompilerInfo info, EnumDefinitionNode node) {
        super(info, node);
    }

    @Override
    @NotNull
    @Unmodifiable
    public List<Byte> convert(int start) {
        var names = getNames();
        var declarations = new AttributeConverter(info);
        var methods = new MethodConverter(info);
        var operators = new OperatorDefConverter(info);
        var properties = new PropertyConverter(info);
        var trueSupers = convertSupers(info.typesOf(node.getSuperclasses()));
        StdTypeObject type;
        if (!info.hasType(node.getName().strName())) {
            type = new StdTypeObject(node.getName().strName(), List.of(trueSupers), GenericInfo.empty(), true);
            ensureProperInheritance(type, trueSupers);
            info.addType(type);
            parseStatements(declarations, methods, operators, properties);
        } else {
            type = (StdTypeObject) info.getType(node.getName().strName());
        }
        type.isConstClass();
        var operatorDefs = new HashMap<>(operators.getOperatorInfos());
        operatorDefs.remove(OpSpTypeNode.NEW);  // Should not be accessible publicly
        var staticVars = staticInfos(Arrays.asList(node.getNames()), declarations.getStaticVars(), type);
        checkAttributes(declarations.getVars(), staticVars, methods.getMethods(), methods.getStaticMethods());
        type.setAttributes(allAttributes(declarations.getVars(), methods.getMethods(), properties.getProperties()));
        type.setStaticAttributes(allAttributes(staticVars, methods.getStaticMethods(), new HashMap<>()));
        type.setOperators(operatorDefs);
        List<Short> superConstants = new ArrayList<>();
        for (var sup : trueSupers) {
            superConstants.add(info.constIndex(sup.name()));
        }
        var staticNames = getStatics(names, declarations.staticVarsWithInts());
        var cls = new ClassInfo.Factory()
                .setType(type)
                .setSuperConstants(superConstants)
                .setVariables(declarations.varsWithInts())
                .setStaticVariables(staticNames)
                .setOperatorDefs(convert(type, operators.getOperators()))
                .setStaticOperators(new HashMap<>())
                .setMethodDefs(convert(type, methods.getMethods()))
                .setStaticMethods(convert(type, methods.getStaticMethods()))
                .setProperties(merge(convert(type, properties.getGetters()), convert(type, properties.getSetters())))
                .create();
        int classIndex = info.addClass(cls);
        var name = node.getName().strName();
        if (Builtins.FORBIDDEN_NAMES.contains(name)) {
            throw CompilerException.format("Illegal name for enum '%s'", node.getName(), name);
        }
        info.checkDefinition(name, node);
        info.addVariable(name, Builtins.TYPE.generify(type), new ClassConstant(name, classIndex), node);

        return getInitBytes(start, operators.getOperators().get(OpSpTypeNode.NEW));
    }

    @NotNull
    private List<Byte> getInitBytes(int start, MethodInfo newOperatorInfo) {
        List<Byte> bytes = new ArrayList<>();
        bytes.add(Bytecode.DO_STATIC.value);
        int doStaticPos = bytes.size();
        bytes.addAll(Util.zeroToBytes());
        for (var name : node.getNames()) {
            if (name instanceof VariableNode) {
                if (!newOperatorInfo.getInfo().matches()) {
                    throw CompilerException.of(
                            "Incorrect number of arguments for enum " +
                                    "(parentheses may only be omitted when enum constructor may take 0 arguments)",
                            name
                    );
                }
            } else if (name instanceof FunctionCallNode) {
                if (!newOperatorInfo.getInfo().matches()) {
                    throw CompilerException.of(
                            "Invalid arguments for enum constructor",
                            name
                    );
                }
            } else {
                throw CompilerInternalError.format(
                        "Node of type %s not a known EnumKeywordNode", name, name.getClass()
                );
            }
        }
        Util.emplace(bytes, Util.intToBytes(start + bytes.size()), doStaticPos);
        throw new UnsupportedOperationException();
    }

    @NotNull
    private List<String> getNames() {
        var len = node.getNames().length;
        List<String> names = new ArrayList<>(len);
        Map<String, LineInfo> doubleDefCheck = new HashMap<>(len);
        for (var name : node.getNames()) {
            var strName = name.getVariable().getName();
            if (doubleDefCheck.containsKey(strName)) {
                throw CompilerException.doubleDef(strName, doubleDefCheck.get(strName), name.getLineInfo());
            }
            doubleDefCheck.put(strName, name.getLineInfo());
            names.add(strName);
        }
        return names;
    }

    @NotNull
    private Map<String, Short> getStatics(@NotNull List<String> names, Map<String, Short> staticVars) {
        Map<String, Short> result = new HashMap<>(staticVars);
        for (var name : names) {
            result.put(name, (short) 0);
        }
        return result;
    }

    @NotNull
    private Map<String, AttributeInfo> staticInfos(@NotNull List<EnumKeywordNode> names,
                                                   Map<String, AttributeInfo> staticVars, TypeObject type) {
        Map<String, AttributeInfo> result = new HashMap<>(staticVars);
        for (var name : names) {
            var strName = name.getVariable().getName();
            if (result.containsKey(strName)) {
                throw CompilerException.doubleDef(strName, result.get(strName), staticVars.get(strName));
            }
            result.put(strName, new AttributeInfo(Set.of(DescriptorNode.PUBLIC), type, name.getLineInfo()));
        }
        return result;
    }

    public static void completeType(CompilerInfo info, EnumDefinitionNode node, StdTypeObject obj) {
        new EnumConverter(info, node).completeType(obj);
    }

    private void completeType(@NotNull StdTypeObject obj) {
        throw new UnsupportedOperationException();
    }
}
