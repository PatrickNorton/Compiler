package main.java.converter;

import main.java.parser.ClassDefinitionNode;
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
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FileInfo {  // FIXME: LineInfo for exceptions
    private TopNode node;
    private Set<String> exports;
    private Map<String, TypeObject> exportTypes;
    private IndexedSet<String> imports;
    private Map<String, TypeObject> importTypes;
    private List<Function> functions;
    private IndexedSet<LangConstant> constants;
    private IndexedSet<ClassInfo> classes;

    private boolean allowSettingExports;
    private boolean linked;

    public FileInfo(TopNode node) {
        this.node = node;
        this.exports = new HashSet<>();
        this.exportTypes = new HashMap<>();
        this.imports = new IndexedHashSet<>();
        this.importTypes = new HashMap<>();
        this.functions = new ArrayList<>(Collections.singletonList(null));
        this.constants = new IndexedHashSet<>();
        this.classes = new IndexedHashSet<>();
        this.allowSettingExports = false;
        this.linked = false;
    }

    public FileInfo compile() {
        link();
        var compilerInfo = new CompilerInfo(this);
        compilerInfo.addStackFrame();
        List<Byte> bytes = new ArrayList<>();
        for (var statement : node) {
            if (statement instanceof ImportExportNode
                    && ((ImportExportNode) statement).getType() == ImportExportNode.EXPORT) {
                continue;
            }
            bytes.addAll(BaseConverter.bytes(bytes.size(), statement, compilerInfo));
        }
        compilerInfo.removeStackFrame();
        // Put the default function at the beginning
        functions.set(0, new Function(new FunctionInfo("__default__", new ArgumentInfo()), bytes));
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

    public int addImport(@NotNull String name) {
        var names = name.split("\\.");
        if (!imports.contains(name)) {
            FileInfo f = Converter.findModule(names[0]);
            imports.add(name);
            importTypes.put(name, f.exportTypes.get(names[1]));
        }
        return imports.indexOf(name);
    }

    public TypeObject importType(String name) {
        return importTypes.get(name);
    }

    public int addFunction(@NotNull Function info) {
        functions.add(info);
        return functions.size() - 1;
    }

    @Nullable
    public FunctionInfo fnInfo(String name) {
        var function = findFunction(name);
        return function == null ? null : function.getInfo();
    }

    @Nullable
    private Function findFunction(String name) {
        for (var fn : functions) {
            if (fn != null && fn.getName().equals(name)) {
                return fn;
            }
        }
        return null;
    }

    public short addConstant(LangConstant value) {
        constants.add(value);
        if (constants.indexOf(value) > Short.MAX_VALUE) {
            throw new RuntimeException("Too many constants");
        }
        return (short) constants.indexOf(value);
    }

    public short constIndex(LangConstant value) {
        return constants.contains(value) ? (short) constants.indexOf(value) : addConstant(value);
    }

    public LangConstant getConstant(short index) {
        return constants.get(index);
    }

    public int addClass(ClassInfo info) {
        classes.add(info);
        return classes.indexOf(info);
    }

    public FileInfo link() {
        if (linked) {
            return this;
        }
        Map<String, String> exports = new HashMap<>();
        Map<String, TypeObject> globals = new HashMap<>();
        for (var stmt : node) {
            if (stmt instanceof DefinitionNode) {
                var name = ((DefinitionNode) stmt).getName();
                TypeObject type;
                if (stmt instanceof FunctionDefinitionNode) {  // TODO: Register functions properly
                    type = Builtins.CALLABLE;
                } else if (stmt instanceof PropertyDefinitionNode) {
                    var typeNode = ((PropertyDefinitionNode) stmt).getType();
                    type = null;  // FIXME: Convert type properly
                } else if (stmt instanceof ContextDefinitionNode) {
                    type = null;
                } else if (stmt instanceof OperatorDefinitionNode) {
                    throw CompilerInternalError.of("Illegal operator definition", stmt);
                } else if (stmt instanceof MethodDefinitionNode) {
                    throw CompilerInternalError.of("Illegal method definition", stmt);
                } else if (stmt instanceof ClassDefinitionNode) {
                    type = Builtins.TYPE;  // FIXME: Generify types correctly
                } else {
                    throw new UnsupportedOperationException(String.format("Unknown definition %s", name.getClass()));
                }
                globals.put(name.toString(), type);
            } else if (stmt instanceof ImportExportNode) {
                var ieNode = (ImportExportNode) stmt;
                switch (ieNode.getType()) {
                    case IMPORT:
                    case TYPEGET:
                        addImports(ieNode, globals);
                        break;
                    case EXPORT:
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
        linked = true;
        return this;
    }

    private void addImports(@NotNull ImportExportNode node, Map<String, TypeObject> globals) {
        assert node.getType() == ImportExportNode.IMPORT;
        boolean notRenamed = node.getAs().length == 0;
        for (int i = 0; i < node.getValues().length; i++) {
            var value = node.getValues()[i];
            var as = notRenamed ? value : node.getAs()[i];
            String moduleName = moduleName(node, i);
            if (!(as.getPreDot() instanceof VariableNode) || as.getPostDots().length > 0) {
                throw CompilerException.of("Illegal import " + as, as);
            }
            String importName = value.toString();
            FileInfo f = node.getPreDots() > 0
                    ? Converter.findLocalModule(this.node.getPath().getParent(), moduleName)
                    : Converter.findModule(moduleName);
            f.compile();  // TODO: Write to file
            if (globals.containsKey(importName)) {
                throw CompilerException.format("Name %s already defined", node, importName);
            } else {
                globals.put(importName, f.exportType(importName));
                addImport(node.getFrom().toString() + "." + value.toString());
            }
        }
    }

    private void addExports(@NotNull ImportExportNode node, Map<String, String> exports) {
        assert node.getType() == ImportExportNode.EXPORT;
        boolean notRenamed = node.getAs().length == 0;
        for (int i = 0; i < node.getValues().length; i++) {
            var value = node.getValues()[i];
            var as = notRenamed ? value : node.getAs()[i];
            if (!(value.getPreDot() instanceof VariableNode) || value.getPostDots().length > 0) {
                throw CompilerException.of("Illegal export " + value, value);
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

    private String moduleName(@NotNull ImportExportNode node, int i) {
        if (!node.getFrom().isEmpty()) {
            return ((VariableNode) node.getFrom().getPreDot()).getName();
        } else {
            return ((VariableNode) node.getValues()[i].getPreDot()).getName();
        }
    }

    public void writeToFile(File file) {
        printDisassembly();
        try (var writer = Files.newOutputStream(file.toPath())) {
            writer.write(Util.MAGIC_NUMBER);
            writer.write(Util.toByteArray(imports.size()));
            for (var name : imports) {
                var keyArray = name.getBytes(StandardCharsets.UTF_8);
                var valArray = name.getBytes(StandardCharsets.UTF_8);
                writer.write(Util.toByteArray(keyArray.length));
                writer.write(keyArray);
                writer.write(Util.toByteArray(valArray.length));
                writer.write(valArray);
            }
            writer.flush();
            writer.write(Util.toByteArray(exports.size()));
            for (var export : exports) {
                var byteArray = export.getBytes(StandardCharsets.UTF_8);
                writer.write(Util.toByteArray(byteArray.length));
                writer.write(byteArray);
            }
            writer.flush();
            writer.write(Util.toByteArray(constants.size()));
            for (var constant : constants) {
                var byteArray = Util.toByteArray(constant.toBytes());
                writer.write(byteArray);
            }
            writer.flush();
            writer.write(Util.toByteArray(functions.size()));
            for (var function : functions) {
                var byteArray = Util.toByteArray(function.getBytes());
                writer.write(Util.toByteArray(StringConstant.strBytes(function.getName())));
                writer.write(Util.toByteArray((short) 0));  // TODO: Put variable count
                writer.write(Util.toByteArray(byteArray.length));
                writer.write(byteArray);
            }
            writer.flush();
            writer.write(Util.toByteArray(classes.size()));
            for (var cls : classes) {
                writer.write(Util.toByteArray(cls.toBytes()));
            }
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("Error in writing bytecode to file:\n" + e.getMessage());
        }
    }

    private void printDisassembly() {
        System.out.println("Constants:");
        for (var constant : constants) {
            System.out.printf("%d: %s%n", constants.indexOf(constant), constant.name());
        }
        for (var function : functions) {
            System.out.printf("%s:%n", function.getName());
            System.out.println(Bytecode.disassemble(this, function.getBytes()));
        }
        for (var cls : classes) {
            for (var fnPair : cls.getMethodDefs().entrySet()) {
                System.out.printf("%s.%s:%n", cls.getType().name(), fnPair.getKey());
                System.out.println(Bytecode.disassemble(this, fnPair.getValue()));
            }
            for (var opPair : cls.getOperatorDefs().entrySet()) {
                System.out.printf("%s.%s:%n", cls.getType().name(), opPair.getKey().toString());
                System.out.println(Bytecode.disassemble(this, opPair.getValue()));
            }
        }
    }
}
