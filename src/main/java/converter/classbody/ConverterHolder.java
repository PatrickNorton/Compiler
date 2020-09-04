package main.java.converter.classbody;

import main.java.converter.AttributeInfo;
import main.java.converter.CompilerException;
import main.java.converter.CompilerInfo;
import main.java.converter.FunctionInfo;
import main.java.converter.MutableType;
import main.java.converter.TypeObject;
import main.java.parser.EnumKeywordNode;
import main.java.parser.Lined;
import main.java.parser.OpSpTypeNode;
import main.java.parser.StatementBodyNode;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ConverterHolder {
    private final AttributeConverter attrs;
    private final MethodConverter methods;
    private final OperatorDefConverter ops;
    private final PropertyConverter props;

    public ConverterHolder(CompilerInfo info) {
        attrs = new AttributeConverter(info);
        methods = new MethodConverter(info);
        ops = new OperatorDefConverter(info);
        props = new PropertyConverter(info);
    }

    // AttributeConverter forwarding methods
    public AttributeConverter attributes() {
        return attrs;
    }

    public Map<String, AttributeInfo> getVars() {
        return attrs.getVars();
    }

    @NotNull
    public Map<String, Short> varsWithInts() {
        return attrs.varsWithInts();
    }

    public Map<String, AttributeInfo> getStaticVars() {
        return attrs.getStaticVars();
    }

    @NotNull
    public Map<String, Short> staticVarsWithInts() {
        return attrs.staticVarsWithInts();
    }

    public Map<String, MethodInfo> getColons() {
        return attrs.getColons();
    }

    public Map<String, MethodInfo> getStaticColons() {
        return attrs.getStaticColons();
    }

    public void addEnumStatics(List<EnumKeywordNode> names, TypeObject type) {
        attrs.addEnumStatics(names, type);
    }

    // MethodConverter forwarding methods
    public MethodConverter methods() {
        return methods;
    }

    public Map<String, MethodInfo> getMethods() {
        return methods.getMethods();
    }

    public Map<String, MethodInfo> getStaticMethods() {
        return methods.getStaticMethods();
    }


    // OperatorDefConverter forwarding methods
    public OperatorDefConverter operators() {
        return ops;
    }

    public Map<OpSpTypeNode, FunctionInfo> getOperatorInfos() {
        return ops.getOperatorInfos();
    }

    public Map<OpSpTypeNode, MethodInfo> getOperators() {
        return ops.getOperators();
    }

    public Map<OpSpTypeNode, FunctionInfo> getStaticOperatorInfos() {
        return ops.getStaticOperatorInfos();
    }

    public Map<OpSpTypeNode, MethodInfo> getStaticOperators() {
        return ops.getStaticOperators();
    }

    // PropertyConverter forwarding methods
    public PropertyConverter properties() {
        return props;
    }

    public Map<String, AttributeInfo> getProperties() {
            return props.getProperties();
        }

    @NotNull
    public Map<String, MethodInfo> allGetters() {
        Map<String, MethodInfo> result = new HashMap<>(props.getGetters());
        result.putAll(attrs.getColons());
        return result;
    }

    @NotNull
    public Map<String, MethodInfo> getSetters() {
        return props.getSetters();
    }

    public Map<String, MethodInfo> staticGetters() {
        return attrs.getStaticColons();
    }

    @NotNull
    public Map<String, MethodInfo> staticSetters() {
        Map<String, MethodInfo> result = new HashMap<>(attrs.getStaticColons().size());
        for (var pair : attrs.getStaticColons().entrySet()) {
            var mInfo = pair.getValue();
            var newMethodInfo = new MethodInfo(
                    mInfo.getAccessLevel(),
                    mInfo.isMut(),
                    mInfo.getInfo(),
                    new StatementBodyNode(),
                    mInfo.getLineInfo()
            );
            result.put(pair.getKey(), newMethodInfo);
        }
        return result;
    }

    public void checkAttributes() {
        checkMaps(attrs.getVars(), methods.getMethods(), methods.getStaticMethods());
        checkMaps(attrs.getStaticVars(), methods.getMethods(), methods.getStaticMethods());
        checkMaps(methods.getMethods(), attrs.getVars(), attrs.getStaticVars());
        checkMaps(methods.getStaticMethods(), attrs.getVars(), attrs.getStaticVars());
    }

    private void checkMaps(@NotNull Map<String, ? extends Lined> vars, Map<String, ? extends Lined> methods,
                           Map<String, ? extends Lined> staticMethods) {
        for (var pair : vars.entrySet()) {
            if (methods.containsKey(pair.getKey())) {
                throw CompilerException.doubleDef(
                        pair.getKey(),
                        pair.getValue(),
                        methods.get(pair.getKey())
                );
            } else if (staticMethods.containsKey(pair.getKey())) {
                throw CompilerException.doubleDef(
                        pair.getKey(),
                        pair.getValue(),
                        staticMethods.get(pair.getKey())
                );
            }
        }
    }

    @NotNull
    public Map<String, AttributeInfo> allAttrs() {
        return mergeAttrs(attrs.getVars(), attrs.getColons(), methods.getMethods(), props.getProperties());
    }

    @NotNull
    public Map<String, AttributeInfo> staticAttrs() {
        return mergeAttrs(attrs.getStaticVars(), attrs.getStaticColons(), methods.getMethods(), new HashMap<>());
    }

    @NotNull
    private Map<String, AttributeInfo> mergeAttrs(
            Map<String, AttributeInfo> attrs,
            @NotNull Map<String, MethodInfo> colons,
            @NotNull Map<String, MethodInfo> methods,
            Map<String, AttributeInfo> properties
    ) {
        var finalAttrs = new HashMap<>(attrs);
        addInfos(finalAttrs, colons.entrySet());
        addInfos(finalAttrs, methods.entrySet());
        finalAttrs.putAll(properties);
        return finalAttrs;
    }

    private static void addInfos(
            Map<String, AttributeInfo> finalAttrs,
            @NotNull Set<Map.Entry<String, MethodInfo>> entrySet
    ) {
        for (var pair : entrySet) {
            var methodInfo = pair.getValue();
            var mutType = methodInfo.isMut() ? MutableType.MUT_METHOD : MutableType.STANDARD;
            var attrInfo = new AttributeInfo(methodInfo.getAccessLevel(), mutType, methodInfo.getInfo().toCallable());
            finalAttrs.put(pair.getKey(), attrInfo);
        }
    }
}
