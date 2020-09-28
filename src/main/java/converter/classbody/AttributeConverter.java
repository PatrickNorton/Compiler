package main.java.converter.classbody;

import main.java.converter.AccessLevel;
import main.java.converter.AttributeInfo;
import main.java.converter.CompilerException;
import main.java.converter.CompilerInfo;
import main.java.converter.FunctionInfo;
import main.java.converter.MutableType;
import main.java.converter.TestConverter;
import main.java.converter.TypeObject;
import main.java.parser.DeclarationNode;
import main.java.parser.DeclaredAssignmentNode;
import main.java.parser.DescriptorNode;
import main.java.parser.EnumKeywordNode;
import main.java.parser.Lined;
import main.java.parser.ReturnStatementNode;
import main.java.parser.StatementBodyNode;
import main.java.parser.TestNode;
import main.java.parser.VariableNode;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AttributeConverter {
    private final Map<String, AttributeInfo> vars;
    private final Map<String, AttributeInfo> staticVars;
    private final Map<String, Method> colons;
    private final Map<String, Method> staticColons;
    private final CompilerInfo info;

    public AttributeConverter(CompilerInfo info) {
        this.info = info;
        vars = new HashMap<>();
        staticVars = new HashMap<>();
        colons = new HashMap<>();
        staticColons = new HashMap<>();
    }

    public void parse(@NotNull DeclarationNode node) {
        for (var name : node.getNames()) {
            var strName = ((VariableNode) name).getName();
            var descriptors = node.getDescriptors();
            var accessLevel = AccessLevel.fromDescriptors(descriptors);
            var mutType = MutableType.fromDescriptors(descriptors);
            checkVars(strName, name, vars);
            checkVars(strName, name, staticVars);
            var attrInfo = new AttributeInfo(accessLevel, mutType, info.getType(node.getType()), node.getLineInfo());
            if (descriptors.contains(DescriptorNode.STATIC)) {
                staticVars.put(strName, attrInfo);
            } else {
                vars.put(strName, attrInfo);
            }
        }
    }

    public void parse(@NotNull DeclaredAssignmentNode node) {
        if (node.isColon()) {
            parseColon(node);
        } else {
            parseNonColon(node);
        }
    }

    public Map<String, AttributeInfo> getVars() {
        return vars;
    }

    @NotNull
    public Map<String, Short> varsWithInts() {
        Map<String, Short> result = new HashMap<>();
        for (var pair : vars.entrySet()) {
            result.put(pair.getKey(), (short) 0);  // TODO: Effectively serialize types (esp. union)
        }
        return result;
    }

    public Map<String, AttributeInfo> getStaticVars() {
        return staticVars;
    }

    @NotNull
    public Map<String, Short> staticVarsWithInts() {
        Map<String, Short> result = new HashMap<>();
        for (var pair : vars.entrySet()) {
            result.put(pair.getKey(), (short) 0);
        }
        return result;
    }

    public Map<String, Method> getColons() {
        return colons;
    }

    public Map<String, Method> getStaticColons() {
        return staticColons;
    }

    public void addEnumStatics(@NotNull List<EnumKeywordNode> names, TypeObject type) {
        for (var name : names) {
            var strName = name.getVariable().getName();
            if (staticVars.containsKey(strName)) {
                throw CompilerException.doubleDef(strName, staticVars.get(strName), name);
            }
            staticVars.put(strName, new AttributeInfo(AccessLevel.PUBLIC, type, name.getLineInfo()));
        }
    }

    public void addUnionMethods(@NotNull Map<String, Method> variantMethods) {
        for (var pair : variantMethods.entrySet()) {
            checkVars(pair.getKey(), pair.getValue(), staticVars);
            checkVars(pair.getKey(), pair.getValue(), staticColons);
        }
        staticColons.putAll(variantMethods);
    }

    private void parseNonColon(@NotNull DeclaredAssignmentNode node) {
        var attrType = info.getType(node.getTypes()[0].getType());
        var accessLevel = AccessLevel.fromDescriptors(node.getDescriptors());
        var mutType = MutableType.fromDescriptors(node.getDescriptors());
        var attrInfo = new AttributeInfo(accessLevel, mutType, attrType, node.getLineInfo());
        var name = ((VariableNode) node.getNames()[0]).getName();
        if (node.getDescriptors().contains(DescriptorNode.STATIC)) {
            checkVars(name, node, staticVars);
            checkVars(name, node, staticColons);
            staticVars.put(name, attrInfo);
        } else {
            checkVars(name, node, vars);
            checkVars(name, node, colons);
            vars.put(name, attrInfo);
        }
    }

    private void parseColon(@NotNull DeclaredAssignmentNode node) {
        assert node.isColon();
        var lineInfo = node.getLineInfo();
        var retStmt = new ReturnStatementNode(lineInfo, node.getValues(), TestNode.empty());
        var body = new StatementBodyNode(lineInfo, retStmt);
        var strName = node.getNames()[0].toString();
        var descriptors = node.getDescriptors();
        var accessLevel = AccessLevel.fromDescriptors(descriptors);
        var isMut = descriptors.contains(DescriptorNode.MUT);
        boolean isStatic = descriptors.contains(DescriptorNode.STATIC);
        checkVars(strName, node, isStatic ? staticColons : colons);
        checkVars(strName, node, isStatic ? staticVars : vars);
        var retType = TestConverter.returnType(node.getValues().get(0), info, 1)[0];
        var fnInfo = new FunctionInfo(retType);
        (isStatic ? staticColons : colons).put(strName, new Method(accessLevel, isMut, fnInfo, body, lineInfo));
    }

    private static void checkVars(String strName, Lined name, @NotNull Map<String, ? extends Lined> vars) {
        if (vars.containsKey(strName)) {
            throw CompilerException.doubleDef(strName, name, vars.get(strName));
        }
    }
}
