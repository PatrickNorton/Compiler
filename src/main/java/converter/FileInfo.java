package main.java.converter;

import main.java.parser.ContextDefinitionNode;
import main.java.parser.DefinitionNode;
import main.java.parser.FunctionDefinitionNode;
import main.java.parser.ImportExportNode;
import main.java.parser.LineInfo;
import main.java.parser.MethodDefinitionNode;
import main.java.parser.OperatorDefinitionNode;
import main.java.parser.PropertyDefinitionNode;
import main.java.parser.TopNode;
import main.java.parser.VariableNode;
import main.java.util.IndexedHashSet;
import main.java.util.IndexedSet;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FileInfo {  // FIXME: LineInfo for exceptions
    private TopNode node;
    private Set<String> exports;
    private Map<String, TypeObject> exportTypes;
    private IndexedSet<String> imports;
    private Map<String, TypeObject> importTypes;
    private Map<String, List<Byte>> functions;
    private IndexedSet<LangConstant> constants;

    private boolean allowSettingExports;

    public FileInfo(TopNode node) {
        this.node = node;
        this.exports = new HashSet<>();
        this.exportTypes = new HashMap<>();
        this.imports = new IndexedHashSet<>();
        this.importTypes = new HashMap<>();
        this.functions = new LinkedHashMap<>();
        this.constants = new IndexedHashSet<>();
    }

    public FileInfo compile() {
        link();
        var compilerInfo = new CompilerInfo(this);
        List<Byte> bytes = new ArrayList<>();
        for (var statement : node) {
            if (statement instanceof ImportExportNode
                    && ((ImportExportNode) statement).getType().equals("export")) {
                continue;
            }
            bytes.addAll(BaseConverter.bytes(bytes.size(), statement, compilerInfo));
        }
        functions.put("__default__", bytes);
        return this;
    }

    public void addExport(String name, TypeObject type, LineInfo info) {
        if (!allowSettingExports) {
            throw CompilerException.of("Illegal position for export statement", info);
        }
        this.exports.add(name);
        exportTypes.put(name, type);
    }

    public TypeObject exportType(String name) {
        return exportTypes.get(name);
    }

    public int addImport(String name) {
        if (!imports.contains(name)) {
            FileInfo f = Converter.findModule(name);
            imports.add(name);
            importTypes.put(name, f.exportTypes.get(name));
        }
        return imports.indexOf(name);
    }

    public TypeObject importType(String name) {
        return importTypes.get(name);
    }

    public void addFunction(String name, List<Byte> bytecode) {
        this.functions.put(name, bytecode);
    }

    public int addConstant(LangConstant value) {
        constants.add(value);
        return constIndex(value);
    }

    public int constIndex(LangConstant value) {
        return constants.indexOf(value);
    }

    public FileInfo link() {
        Map<String, String> exports = new HashMap<>();
        Map<String, TypeObject> globals = new HashMap<>();
        for (var stmt : node) {
            if (stmt instanceof DefinitionNode) {
                var name = ((DefinitionNode) stmt).getName();
                TypeObject type;
                if (stmt instanceof FunctionDefinitionNode) {
                    type = null;
                } else if (stmt instanceof PropertyDefinitionNode) {
                    var typeNode = ((PropertyDefinitionNode) stmt).getType();
                    type = null;  // FIXME: Convert type properly
                } else if (stmt instanceof ContextDefinitionNode) {
                    type = null;
                } else if (stmt instanceof OperatorDefinitionNode) {
                    throw CompilerInternalError.of("Illegal operator definition", stmt);
                } else if (stmt instanceof MethodDefinitionNode) {
                    throw CompilerInternalError.of("Illegal method definition", stmt);
                } else {
                    type = Builtins.TYPE;
                }
                globals.put(name.toString(), type);
            } else if (stmt instanceof ImportExportNode) {
                var ieNode = (ImportExportNode) stmt;
                switch (ieNode.getType()) {
                    case "import":
                    case "typeget":
                        addImports(ieNode, globals);
                        break;
                    case "export":
                        addExports(ieNode, exports);
                        break;
                    default:
                        throw CompilerInternalError.of(
                                "Unknown type of import/export", ieNode.getLineInfo()
                        );
                }
            }
        }
        try {
            allowSettingExports = true;
            for (var entry : exports.entrySet()) {
                var exportName = entry.getValue();
                var exportType = globals.get(entry.getKey());
                if (exportType == null) {
                    throw CompilerException.of("Undefined name for export: " + exportName, LineInfo.empty());
                }
                this.exportTypes.put(exportName, exportType);
            }
        } finally {
            allowSettingExports = false;
        }
        return this;
    }

    private void addImports(@NotNull ImportExportNode node, Map<String, TypeObject> globals) {
        assert node.getType().equals("import");
        for (int i = 0; i < node.getValues().length; i++) {
            var value = node.getValues()[i];
            var as = node.getAs()[i];
            String importName;
            if (as.isEmpty()) {
                if (!(value.getPreDot() instanceof VariableNode) || value.getPostDots().length > 0) {
                    throw CompilerException.of("Illegal import " + value, value);
                }
                importName = (((VariableNode) value.getPreDot()).getName());
            } else {
                if (!(as.getPreDot() instanceof VariableNode) || as.getPostDots().length > 0) {
                    throw CompilerException.of("Illegal import " + value, value);
                }
                importName = (value.toString());
            }
            FileInfo f = Converter.findModule(importName);
            if (globals.containsKey(importName)) {
                throw CompilerException.format("Name %s already defined", node, importName);
            } else {
                globals.put(importName, f.exportType(importName));
            }
        }
    }

    private void addExports(@NotNull ImportExportNode node, Map<String, String> exports) {
        assert node.getType().equals("export");
        for (int i = 0; i < node.getValues().length; i++) {
            var as = node.getAs()[i];
            var value = node.getValues()[i];
            if (!(value.getPreDot() instanceof VariableNode) || value.getPostDots().length > 0) {
                throw CompilerException.of("Illegal import " + value, value);
            }
            var name = ((VariableNode) value.getPreDot()).getName();
            var asName = as.isEmpty() ? name : ((VariableNode) as.getPreDot()).getName();
            if (exports.containsKey(asName)) {
                throw CompilerException.format("Name %s already exported", node, asName);
            } else {
                exports.put(name, asName);
            }
        }
    }

    public void writeToFile(File file) {
        try (var writer = Files.newOutputStream(file.toPath())) {
            writer.write(Util.MAGIC_NUMBER);
            writer.write(Util.toByteArray(imports.size()));
            for (var name : imports) {
                var keyArray = StandardCharsets.UTF_8.encode(name).array();
                var valArray = StandardCharsets.UTF_8.encode(name).array();
                writer.write(Util.toByteArray(keyArray.length));
                writer.write(keyArray);
                writer.write(Util.toByteArray(valArray.length));
                writer.write(valArray);
            }
            writer.flush();
            writer.write(Util.toByteArray(exports.size()));
            for (var export : exports) {
                var byteArray = StandardCharsets.UTF_8.encode(export).array();
                writer.write(Util.toByteArray(byteArray.length));
                writer.write(byteArray);
            }
            writer.flush();
            writer.write(Util.toByteArray(functions.size()));
            for (var entry : functions.entrySet()) {
                var keyArray = StandardCharsets.UTF_8.encode(entry.getKey()).array();
                var valArray = Util.toByteArray(entry.getValue());
                writer.write(Util.toByteArray(keyArray.length));
                writer.write(keyArray);
                writer.write(Util.toByteArray(valArray.length));
                writer.write(valArray);
            }
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("Error in writing bytecode to file:\n" + e.getMessage());
        }
    }
}
