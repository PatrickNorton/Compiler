package main.java.converter.classbody;

import main.java.converter.ArgumentInfo;
import main.java.converter.AttributeInfo;
import main.java.converter.CompilerException;
import main.java.converter.CompilerInfo;
import main.java.converter.FunctionInfo;
import main.java.parser.LineInfo;
import main.java.parser.PropertyDefinitionNode;
import main.java.parser.StatementBodyNode;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class PropertyConverter {
    private final Map<String, AttributeInfo> properties;
        private final Map<String, StatementBodyNode> getters;
        private final Map<String, StatementBodyNode> setters;
        private final Map<String, LineInfo> lineInfos;
        private final CompilerInfo info;

        public PropertyConverter(CompilerInfo info) {
            this.properties = new HashMap<>();
            this.getters = new HashMap<>();
            this.setters = new HashMap<>();
            this.lineInfos = new HashMap<>();
            this.info = info;
        }

        public void parse(@NotNull PropertyDefinitionNode node) {
            var name = node.getName().getName();
            var type = info.getType(node.getType());
            if (properties.containsKey(name)) {
                throw CompilerException.format(
                        "Illegal name: property with name '%s' already defined in this class (see line %d)",
                        node, name, lineInfos.get(name).getLineNumber()
                );
            }
            properties.put(name, new AttributeInfo(node.getDescriptors(), type));
            getters.put(name, node.getGet());
            setters.put(name, node.getSet());  // TODO: If setter is empty
            lineInfos.put(name, node.getLineInfo());
        }

        public Map<String, AttributeInfo> getProperties() {
            return properties;
        }

        @NotNull
        public Map<String, MethodInfo> getGetters() {
            Map<String, MethodInfo> result = new HashMap<>();
            for (var pair : getters.entrySet()) {
                var property = properties.get(pair.getKey());
                var fnInfo = new FunctionInfo(property.getType());
                var mInfo = new MethodInfo(property.getDescriptors(), fnInfo,
                        pair.getValue(), pair.getValue().getLineInfo());
                result.put(pair.getKey(), mInfo);
            }
            return result;
        }

        @NotNull
        public Map<String, MethodInfo> getSetters() {
            Map<String, MethodInfo> result = new HashMap<>();
            for (var pair : setters.entrySet()) {
                var property = properties.get(pair.getKey());
                var fnInfo = new FunctionInfo(ArgumentInfo.of(properties.get(pair.getKey()).getType()));
                var mInfo = new MethodInfo(property.getDescriptors(), fnInfo,
                        pair.getValue(), pair.getValue().getLineInfo());
                result.put(pair.getKey(), mInfo);
            }
            return result;
        }
}
