package main.java.converter.classbody;

import main.java.converter.AttributeInfo;
import main.java.converter.CompilerException;
import main.java.converter.CompilerInfo;
import main.java.converter.FunctionInfo;
import main.java.converter.TestConverter;
import main.java.parser.DeclarationNode;
import main.java.parser.DeclaredAssignmentNode;
import main.java.parser.DescriptorNode;
import main.java.parser.Lined;
import main.java.parser.ReturnStatementNode;
import main.java.parser.StatementBodyNode;
import main.java.parser.TestNode;
import main.java.parser.VariableNode;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class AttributeConverter {
    private final Map<String, AttributeInfo> vars;
    private final Map<String, AttributeInfo> staticVars;
    private final Map<String, MethodInfo> colons;
    private final Map<String, MethodInfo> staticColons;
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
            checkVars(strName, name, vars);
            checkVars(strName, name, staticVars);
            var attrInfo = new AttributeInfo(descriptors, info.getType(node.getType()), node.getLineInfo());
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

    public Map<String, MethodInfo> getColons() {
        return colons;
    }

    public Map<String, MethodInfo> getStaticColons() {
        return staticColons;
    }

    private void parseNonColon(@NotNull DeclaredAssignmentNode node) {
        var attrType = info.getType(node.getTypes()[0].getType());
        var attrInfo = new AttributeInfo(node.getDescriptors(), attrType, node.getLineInfo());
        if (node.getDescriptors().contains(DescriptorNode.STATIC)) {
            staticVars.put(((VariableNode) node.getNames()[0]).getName(), attrInfo);
        } else {
            vars.put(((VariableNode) node.getNames()[0]).getName(), attrInfo);
        }
    }

    private void parseColon(@NotNull DeclaredAssignmentNode node) {
        assert node.isColon();
        var lineInfo = node.getLineInfo();
        var retStmt = new ReturnStatementNode(lineInfo, node.getValues(), TestNode.empty());
        var body = new StatementBodyNode(lineInfo, retStmt);
        var strName = node.getNames()[0].toString();
        checkVars(strName, node, colons);
        checkVars(strName, node, staticColons);
        checkVars(strName, node, vars);
        checkVars(strName, node, staticVars);
        var retType = TestConverter.returnType(node.getValues().get(0), info, 1)[0];
        var fnInfo = new FunctionInfo(retType);
        var descriptors = node.getDescriptors();
        (descriptors.contains(DescriptorNode.STATIC) ? staticColons : colons)
                .put(strName, new MethodInfo(descriptors, fnInfo, body, lineInfo));
    }

    private static void checkVars(String strName, Lined name, @NotNull Map<String, ? extends Lined> vars) {
        if (vars.containsKey(strName)) {
            throw CompilerException.doubleDef(strName, name, vars.get(strName));
        }
    }
}
