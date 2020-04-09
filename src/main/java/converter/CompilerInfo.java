package main.java.converter;

import main.java.parser.DescriptorNode;
import main.java.parser.ImportExportNode;
import main.java.parser.LineInfo;
import main.java.parser.TopNode;
import main.java.parser.TypeLikeNode;
import main.java.parser.TypeNode;
import main.java.parser.TypeUnionNode;
import main.java.parser.TypewiseAndNode;
import main.java.util.IndexedHashSet;
import main.java.util.IndexedSet;
import main.java.util.IntAllocator;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

public final class CompilerInfo {
    private TopNode node;
    private Set<String> exports = new HashSet<>();
    private Map<String, TypeObject> exportTypes = new HashMap<>();
    private IndexedSet<String> imports = new IndexedHashSet<>();
    private Map<String, TypeObject> importTypes = new HashMap<>();
    private List<Function> functions = new ArrayList<>(Collections.singletonList(null));
    private IndexedSet<LangConstant> constants = new IndexedHashSet<>();
    private IndexedSet<ClassInfo> classes = new IndexedHashSet<>();
    private LoopManager loopManager = new LoopManager();

    private List<Map<String, VariableInfo>> variables = new ArrayList<>();
    private Map<String, NameableType> typeMap = new HashMap<>();
    private IntAllocator varNumbers = new IntAllocator();

    private IntAllocator anonymousNums = new IntAllocator();

    private Deque<TypeObject[]> fnReturns = new ArrayDeque<>();

    private Set<TypeObject> classesWithAccess = new HashSet<>();

    private boolean allowSettingExports = false;
    private boolean linked = false;
    private boolean compiled = false;

    public CompilerInfo(TopNode node) {
        this.node = node;
    }

    public CompilerInfo compile(File file) {
        if (compiled) {
            return this;
        }
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
        writeToFile(file);
        compiled = true;
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
        for (var fn : functions) {
            if (fn != null && fn.getName().equals(name)) {
                return fn.getInfo();
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
        var linker = new Linker(this).link(node);
        var exports = linker.getExports();
        var globals = linker.getGlobals();
        try {
            allowSettingExports = true;
            for (var entry : exports.entrySet()) {
                var exportName = entry.getValue().getKey();
                var exportType = globals.get(entry.getKey());
                if (exportType == null) {
                    var lineInfo = entry.getValue().getValue();
                    throw CompilerException.of("Undefined name for export: " + exportName, lineInfo);
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
                writer.write(StringConstant.strByteArray(name));
                writer.write(StringConstant.strByteArray(name));  // TODO: Make these meaningfully different
            }
            writer.flush();
            writer.write(Util.toByteArray(exports.size()));
            for (var export : exports) {
                writer.write(StringConstant.strByteArray(export));
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
            for (var propPair : cls.getProperties().entrySet()) {
                System.out.printf("%s.%s.get:%n", cls.getType().name(), propPair.getKey());
                System.out.println(Bytecode.disassemble(this, propPair.getValue().getKey()));
                System.out.printf("%s.%s.set:%n", cls.getType().name(), propPair.getKey());
                System.out.println(Bytecode.disassemble(this, propPair.getValue().getValue()));
            }
        }
    }

    /**
     * Add a break statement to the pool of un-linked statements.
     *
     * @param levels The number of levels to break
     * @param location The location (absolute, by start of function)
     */
    public void addBreak(int levels, int location) {
        loopManager.addBreak(levels, location);
    }

    /**
     * Add a continue statement's pointer to the list.
     *
     * @param location The location (absolute, by start of function)
     */
    public void addContinue(int location) {
        loopManager.addContinue(location);
    }

    /**
     * Set the point where a {@code continue} statement should jump to.
     *
     * @param location The location (absolute, by start of function)
     */
    public void setContinuePoint(int location) {
        loopManager.setContinuePoint(location);
    }

    public LoopManager loopManager() {
        return loopManager;
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

    public Path path() {
        return node.getPath();
    }

    public void addFunctionReturns(TypeObject[] values) {
        fnReturns.push(values);
    }

    public TypeObject[] currentFnReturns() {
        return fnReturns.peekFirst();
    }

    public void popFnReturns() {
        fnReturns.pop();
    }

    public boolean notInFunction() {
        return fnReturns.isEmpty();
    }

    public String lambdaName() {
        return String.format("lambda$%d", anonymousNums.getNext());
    }

    public DescriptorNode accessLevel(TypeObject obj) {
        return classesWithAccess.contains(obj) ? DescriptorNode.PRIVATE : DescriptorNode.PUBLIC;
    }

    public void allowPrivateAccess(TypeObject obj) {
        assert !classesWithAccess.contains(obj);
        classesWithAccess.add(obj);
    }

    public void removePrivateAccess(TypeObject obj) {
        assert classesWithAccess.contains(obj);
        classesWithAccess.remove(obj);
    }

    {  // Prevent "non-updating" compiler warning
        anonymousNums.remove(0);
    }
}
