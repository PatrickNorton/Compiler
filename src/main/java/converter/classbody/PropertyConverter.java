package main.java.converter.classbody;

import main.java.converter.AccessLevel;
import main.java.converter.Argument;
import main.java.converter.ArgumentInfo;
import main.java.converter.AttributeInfo;
import main.java.converter.CompilerException;
import main.java.converter.CompilerInfo;
import main.java.converter.FunctionInfo;
import main.java.converter.MutableType;
import main.java.parser.LineInfo;
import main.java.parser.Lined;
import main.java.parser.PropertyDefinitionNode;
import main.java.parser.StatementBodyNode;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class PropertyConverter {
    private final Map<String, AttributeInfo> properties;
    private final Map<String, PropertyInfo> propertyInfos;
    private final CompilerInfo info;

    public PropertyConverter(CompilerInfo info) {
        this.properties = new HashMap<>();
        this.propertyInfos = new HashMap<>();
        this.info = info;
    }

    public void parse(@NotNull PropertyDefinitionNode node) {
        var name = node.getName().getName();
        var type = info.getType(node.getType());
        if (properties.containsKey(name)) {
            throw CompilerException.doubleDef(name, propertyInfos.get(name), node);
        }
        var accessLevel = AccessLevel.fromDescriptors(node.getDescriptors());
        assert MutableType.fromDescriptors(node.getDescriptors()) == MutableType.STANDARD
                : "Properties should never be mut";
        var argInfo = ArgumentInfo.of(node.getSetArgs(), info);
        if (argInfo.size() > 0 && !argInfo.matches(new Argument("", type))) {
            throw CompilerException.format("Invalid argument info for setter", node.getSetArgs());
        }
        properties.put(name, new AttributeInfo(accessLevel, type));
        // TODO: If setter is empty
        propertyInfos.put(name, new PropertyInfo(node.getGet(), node.getSet(), argInfo, node.getLineInfo()));
    }

    public Map<String, AttributeInfo> getProperties() {
        return properties;
    }

    @NotNull
    public Map<String, Method> getGetters() {
        Map<String, Method> result = new HashMap<>();
        for (var pair : propertyInfos.entrySet()) {
            var property = properties.get(pair.getKey());
            var fnInfo = new FunctionInfo(property.getType());
            var mInfo = new Method(property.getAccessLevel(), fnInfo,
                    pair.getValue().getGetter(), pair.getValue().getLineInfo());
            result.put(pair.getKey(), mInfo);
        }
        return result;
    }

    @NotNull
    public Map<String, Method> getSetters() {
        Map<String, Method> result = new HashMap<>();
        for (var pair : propertyInfos.entrySet()) {
            var property = properties.get(pair.getKey());
            var fnInfo = new FunctionInfo(pair.getValue().getSetterArgs());
            var mInfo = new Method(property.getAccessLevel(), true, fnInfo,
                    pair.getValue().getSetter(), pair.getValue().getLineInfo());
            result.put(pair.getKey(), mInfo);
        }
        return result;
    }

    private static final class PropertyInfo implements Lined {
        private final StatementBodyNode getter;
        private final StatementBodyNode setter;
        private final ArgumentInfo setterArgs;
        private final LineInfo lineInfo;

        public PropertyInfo(
                StatementBodyNode getter, StatementBodyNode setter, ArgumentInfo setterArgs, LineInfo lineInfo
        ) {
            this.getter = getter;
            this.setter = setter;
            this.setterArgs = setterArgs;
            this.lineInfo = lineInfo;
        }

        @Override
        public LineInfo getLineInfo() {
            return lineInfo;
        }

        public StatementBodyNode getGetter() {
            return getter;
        }

        public StatementBodyNode getSetter() {
            return setter;
        }

        public ArgumentInfo getSetterArgs() {
            return setterArgs;
        }
    }
}
