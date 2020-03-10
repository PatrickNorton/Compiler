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
import main.java.parser.TypeLikeNode;
import main.java.parser.TypeNode;
import main.java.parser.TypeUnionNode;
import main.java.parser.TypewiseAndNode;
import main.java.parser.VariableNode;
import main.java.util.IndexedHashSet;
import main.java.util.IndexedSet;
import main.java.util.IntAllocator;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

// TODO? Split up a little
public final class CompilerInfo {  // FIXME: LineInfo for exceptions
    private TopNode node;
    private Set<String> exports;
    private Map<String, TypeObject> exportTypes;
    private IndexedSet<String> imports;
    private Map<String, TypeObject> importTypes;
    private List<Function> functions;
    private IndexedSet<LangConstant> constants;
    private IndexedSet<ClassInfo> classes;
    private Deque<Boolean> loopLevel;
    private Map<Integer, Set<Integer>> breakPointers;
    private Map<Integer, Set<Integer>> continuePointers;
    private Deque<Integer> continueLocations;

    private List<Map<String, VariableInfo>> variables;
    private Map<String, NameableType> typeMap;
    private IntAllocator varNumbers;

    private boolean allowSettingExports;
    private boolean linked;

    public CompilerInfo(TopNode node) {
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
        this.loopLevel = new ArrayDeque<>();
        this.breakPointers = new HashMap<>();
        this.continuePointers = new HashMap<>();
        this.continueLocations = new ArrayDeque<>();
        this.variables = new ArrayList<>();
        this.typeMap = new HashMap<>();
        this.varNumbers = new IntAllocator();
    }

    public CompilerInfo compile() {
        link();
        this.addStackFrame();
        List<Byte> bytes = new ArrayList<>();
        for (var statement : node) {
            if (statement instanceof ImportExportNode
                    && ((ImportExportNode) statement).getType() == ImportExportNode.EXPORT) {
                continue;
            }
            bytes.addAll(BaseConverter.bytes(bytes.size(), statement, this));
        }
        this.removeStackFrame();
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
            CompilerInfo f = Converter.findModule(names[0]);
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

    /**
     * Add a constant to the constant pool.
     *
     * @param value The value to add
     */
    public short addConstant(LangConstant value) {
        constants.add(value);
        if (constants.indexOf(value) > Short.MAX_VALUE) {
            throw new RuntimeException("Too many constants");
        }
        return (short) constants.indexOf(value);
    }

    /**
     * The index of a constant in the constant stack.
     *
     * @param value The name of the variable
     * @return The index in the stack
     */
    public short constIndex(LangConstant value) {
        return constants.contains(value) ? (short) constants.indexOf(value) : addConstant(value);
    }

    /**
     * The index of a constant variable in the constant stack.
     *
     * @param name The name of the variable
     * @return The index in the stack
     */
    public short constIndex(String name) {
        var variableInfo = varInfo(name);
        return constIndex(variableInfo != null
                ? variableInfo.constValue()
                : Builtins.constantOf(name));
    }

    public LangConstant getConstant(short index) {
        return constants.get(index);
    }

    public int addClass(ClassInfo info) {
        classes.add(info);
        return classes.indexOf(info);
    }

    public CompilerInfo link() {
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
                this.exports.add(exportName);
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
            CompilerInfo f = node.getPreDots() > 0
                    ? Converter.findLocalModule(this.node.getPath().getParent(), moduleName)
                    : Converter.findModule(moduleName);
            f.compile().writeToFile(Converter.getDestFile().toPath().resolve(moduleName + ".nbyte").toFile());
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

    public void writeToFile(@NotNull File file) {
        printDisassembly();
        if (!file.getParentFile().exists()) {
            if (!file.getParentFile().mkdir()) {
                throw new RuntimeException("Could not create file " + file);
            }
        }
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
                for (int i = 0; i < constants.size(); i++) {
                    if (constants.get(i).name().equals(export)) {
                        writer.write(Util.toByteArray(i));
                    }
                }
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
            for (var fnPair : cls.getStaticMethods().entrySet()) {
                System.out.printf("%s.%s:%n", cls.getType().name(), fnPair.getKey());
                System.out.println(Bytecode.disassemble(this, fnPair.getValue()));
            }
            for (var opPair : cls.getOperatorDefs().entrySet()) {
                System.out.printf("%s.%s:%n", cls.getType().name(), opPair.getKey().toString());
                System.out.println(Bytecode.disassemble(this, opPair.getValue()));
            }
        }
    }

    /**
     * Enter another loop, implying another level of break/continue statements.
     */
    public void enterLoop(boolean hasContinue) {
        loopLevel.push(hasContinue);
        continueLocations.push(-1);
        assert continueLocations.size() == loopLevel.size();
        addStackFrame();
    }

    /**
     * Exit a loop, and set all dangling pointers to the end of the loop.
     *
     * @param listStart The index of the beginning of the list relative to the
     *                  start of the function
     * @param bytes The list of bytes
     */
    public void exitLoop(int listStart, @NotNull List<Byte> bytes) {
        int level = loopLevel.size();
        boolean hasContinue = loopLevel.pop();
        int endLoop = listStart + bytes.size();
        for (int i : breakPointers.getOrDefault(level, Collections.emptySet())) {
            setPointer(i - listStart, bytes, endLoop);
        }
        breakPointers.remove(level);
        int continueLocation = continueLocations.pop();
        if (hasContinue) {
            assert continueLocation != -1 : "Continue location not defined";
            for (int i : continuePointers.getOrDefault(level, Collections.emptySet())) {
                setPointer(i - listStart, bytes, continueLocation);
            }
            continuePointers.remove(level);
        } else {
            assert continueLocation == -1 : "Continue location erroneously set";
        }
        removeStackFrame();
    }

    /**
     * Add a break statement to the pool of un-linked statements.
     *
     * @param levels The number of levels to break
     * @param location The location (absolute, by start of function)
     */
    public void addBreak(int levels, int location) {
        var pointerSet = breakPointers.computeIfAbsent(loopLevel.size() - levels, k -> new HashSet<>());
        pointerSet.add(location);
    }

    /**
     * Add a continue statement's pointer to the list.
     *
     * @param location The location (absolute, by start of function)
     */
    public void addContinue(int location) {
        var pointerSet = continuePointers.computeIfAbsent(loopLevel.size(), k -> new HashSet<>());
        pointerSet.add(location);
    }

    /**
     * Set the point where a {@code continue} statement should jump to.
     *
     * @param location The location (absolute, by start of function)
     */
    public void setContinuePoint(int location) {
        assert continueLocations.size() == loopLevel.size() : "Mismatch in continue levels";
        int oldValue = continueLocations.pop();
        assert oldValue == -1 : "Defined multiple continue points for loop";
        continueLocations.push(location);
    }

    /**
     * Get the compiler's type from a {@link TypeLikeNode}.
     *
     * @param type The node to translate
     * @return The compiler's type
     */
    @NotNull
    @Contract(pure = true)
    public TypeObject getType(@NotNull TypeLikeNode type) {
        if (type instanceof TypeUnionNode) {
            var union = (TypeUnionNode) type;
            return TypeObject.union(typesOf(union.getSubtypes()));
        } else if (type instanceof TypewiseAndNode) {
            var union = (TypewiseAndNode) type;
            return TypeObject.intersection(typesOf(union.getSubtypes()));
        } else {
            assert type instanceof TypeNode;
            if (((TypeNode) type).getName().toString().equals("null")) {
                var nullType = (TypeNode) type;
                assert nullType.getSubtypes().length == 0;
                if (nullType.isOptional()) {
                    CompilerWarning.warn("Type 'null?' is equivalent to null", type.getLineInfo());
                }
                return Builtins.NULL_TYPE;
            }
            var value = typeMap.get(type.strName());
            if (value == null) {
                var builtin = Builtins.BUILTIN_MAP.get(type.strName());
                if (builtin instanceof TypeObject) {
                    var typeObj = (TypeObject) builtin;
                    var endType = type.getSubtypes().length == 0 ? typeObj : typeObj.generify(typesOf(type.getSubtypes()));
                    return type.isOptional() ? TypeObject.optional(endType) : endType;
                } else {
                    throw new RuntimeException("Unknown type " + type);
                }
            } else {
                var endType = type.getSubtypes().length == 0 ? value : value.generify(typesOf(type.getSubtypes()));
                return type.isOptional() ? TypeObject.optional(endType) : endType;
            }
        }
    }

    @Nullable
    public NameableType classOf(String str) {
        var cls = typeMap.get(str);
        if (cls == null) {
            var builtin = Builtins.BUILTIN_MAP.get(str);
            return builtin instanceof NameableType ? (NameableType) builtin : null;
        }
        return cls;
    }

    public void addType(NameableType type) {
        typeMap.put(type.name(), type);
    }

    /**
     * Get the type of a variable.
     *
     * @param variable The variable name to get the type from
     * @return The type of the variable
     */
    public TypeObject getType(String variable) {
        var info = varInfo(variable);
        return info == null ? Builtins.constantOf(variable).getType() : info.getType();
    }

    public boolean varIsUndefined(String name) {
        return varInfo(name) == null && !Builtins.BUILTIN_MAP.containsKey(name);
    }

    /**
     * Add a new set of variable names to the stack.
     */
    public void addStackFrame() {
        variables.add(new HashMap<>());
    }

    /**
     * Remove the current level of variable declarations from the stack.
     */
    public void removeStackFrame() {
        var vars = variables.remove(variables.size() - 1);
        for (var pair : vars.values()) {
            varNumbers.remove(pair.getLocation());
        }
    }

    /**
     * Add a variable with a constant value to the stack.
     *
     * @param name The name of the variable
     * @param type The type of the variable
     * @param constValue The constant value the variable has
     */
    public void addVariable(String name, TypeObject type, LangConstant constValue) {
        addConstant(constValue);
        addVariable(name, new VariableInfo(type, constValue));
    }

    public void addVariable(String name, TypeObject type, boolean isConst) {
        addVariable(name, new VariableInfo(type, isConst, (short) varNumbers.getNext()));
    }

    /**
     * Add a variable to the stack.
     *
     * @param name The name of the variable
     * @param type The type of the variable
     */
    public void addVariable(String name, TypeObject type) {
        addVariable(name, new VariableInfo(type, (short) varNumbers.getNext()));
    }

    private void addVariable(String name, VariableInfo info) {
         variables.get(variables.size() - 1).put(name, info);
    }

    /**
     * Check if a variable is constant.
     *
     * @param name The name of the variable to check
     * @return If the variable is constant
     */
    public boolean variableIsConstant(String name) {
        var info = varInfo(name);
        return info == null ? Builtins.BUILTIN_MAP.containsKey(name) : info.hasConstValue();
    }

    @Nullable
    private VariableInfo varInfo(String name) {  // TODO: Universally accessible globals
        for (int i = variables.size() - 1; i >= 0; i--) {
            var map = variables.get(i);
            if (map.containsKey(name)) {
                return map.get(name);
            }
        }
        return null;
    }

    /**
     * The index of the variable in the variable stack.
     *
     * @param name The name of the variable
     * @return The index in the stack
     */
    public short varIndex(String name) {
        return Objects.requireNonNull(varInfo(name), "Unknown variable").getLocation();
    }

    @NotNull
    @Contract(pure = true)
    public TypeObject[] typesOf(@NotNull TypeLikeNode... types) {
        var typeObjects = new TypeObject[types.length];
        for (int i = 0; i < types.length; i++) {
            typeObjects[i] = getType(types[i]);
        }
        return typeObjects;
    }

    /**
     * Set a pointer starting at a given index in the byte list.
     *
     * @param listIndex The index in the list to set
     * @param bytes The list to be set
     * @param value The pointer value to set it to
     */
    private void setPointer(int listIndex, List<Byte> bytes, int value) {
        var ptrBytes = Util.intToBytes(value);
        for (int i = 0; i < ptrBytes.size(); i++) {
            bytes.set(listIndex + i, ptrBytes.get(i));
        }
    }
}
