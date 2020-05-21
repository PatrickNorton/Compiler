package main.java.converter;

import main.java.parser.DescriptorNode;
import main.java.parser.ImportExportNode;
import main.java.parser.LineInfo;
import main.java.parser.Lined;
import main.java.parser.TopNode;
import main.java.parser.TypeLikeNode;
import main.java.parser.TypeNode;
import main.java.parser.TypeUnionNode;
import main.java.parser.TypewiseAndNode;
import main.java.util.Counter;
import main.java.util.HashCounter;
import main.java.util.IndexedHashSet;
import main.java.util.IndexedSet;
import main.java.util.IntAllocator;
import main.java.util.Pair;
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

/**
 * The class representing all information needing to be held during compilation.
 * @author Patrick Norton
 */
public final class CompilerInfo {
    private final TopNode node;
    private final Set<String> exports = new HashSet<>();
    private final Map<String, TypeObject> exportTypes = new HashMap<>();
    private final IndexedSet<String> imports = new IndexedHashSet<>();
    private final Map<String, TypeObject> importTypes = new HashMap<>();
    private final List<Function> functions = new ArrayList<>(Collections.singletonList(null));
    private final IndexedSet<LangConstant> constants = new IndexedHashSet<>();
    private final IndexedSet<ClassInfo> classes = new IndexedHashSet<>();
    private final LoopManager loopManager = new LoopManager();

    private final List<Map<String, VariableInfo>> variables = new ArrayList<>();
    private final Map<String, TypeObject> typeMap = new HashMap<>();
    private final IntAllocator varNumbers = new IntAllocator();
    private final IntAllocator staticVarNumbers = new IntAllocator();

    private final IntAllocator anonymousNums = new IntAllocator();

    private final Deque<Pair<Boolean, TypeObject[]>> fnReturns = new ArrayDeque<>();

    private final Counter<TypeObject> classesWithAccess = new HashCounter<>();
    private final Counter<TypeObject> classesWithProtected = new HashCounter<>();

    private boolean allowSettingExports = false;
    private boolean linked = false;
    private boolean compiled = false;

    public CompilerInfo(TopNode node) {
        this.node = node;
    }

    /**
     * Compiles this, taking the name of the file to write to.
     * <p>
     *     If this is already compiled, nothing will happen. Otherwise, it
     *     compiles the {@link TopNode} represented by this, and writes it to
     *     å file. Furthermore, it will compile all files upon which it depends.
     * </p>
     *
     * @throws CompilerException If a compilation error occurs
     * @throws CompilerInternalError An unexpected internal error
     * @param file The name of the file to be compiled
     * @return Itself
     */
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

    /**
     * Adds an export.
     * <p>
     *     If setting exports is not allowed, (e.g. this is not in the process
     *     of {@link #link linking}), a {@link CompilerException} will be
     *     thrown. This is not a {@link CompilerInternalError} because it is
     *     most likely to be thrown due to an illegally-placed {@link
     *     ImportExportNode 'export'} statement, as opposed to a compiler bug.
     * </p>
     *
     * @see #link
     * @param name The name of the export
     * @param type The type of the export
     * @param info The {@link LineInfo} for the export statement
     */
    public void addExport(String name, TypeObject type, LineInfo info) {
        if (!allowSettingExports) {
            throw CompilerException.of("Illegal position for export statement", info);
        }
        this.exports.add(name);
        exportTypes.put(name, type);
    }

    /**
     * Gets the type of an export.
     *
     * @param name The name of the export
     * @return The export type
     */
    public TypeObject exportType(String name) {
        return exportTypes.get(name);
    }

    /**
     * Gets the set of names exported by this file.
     *
     * @return The set of exports
     */
    public Set<String> getExports() {
        return exports;
    }

    /**
     * Adds an import.
     *
     * @param name The name if the import
     * @return The index of the import in the imports set
     */
    public int addImport(@NotNull String name) {
        var names = name.split("\\.");
        if (!imports.contains(name)) {
            var file = Converter.resolveFile(names[0]);
            CompilerInfo f = Converter.findModule(names[0]).compile(file);
            imports.add(name);
            importTypes.put(name, f.exportTypes.get(names[1]));
        }
        return imports.indexOf(name);
    }

    /**
     * Gets the type of an import.
     *
     * @param name The name of the import
     * @return THe type of the import
     */
    public TypeObject importType(String name) {
        return importTypes.get(name);
    }

    /**
     * Adds a function to the global pool of functions.
     * <p>
     *     This will not add the name of the function to any set of in-scope
     *     variables, so the function's name will need to be added separately
     *     using {@link #addVariable(String, TypeObject, Lined)}. This is for
     *     things such as {@link main.java.parser.LambdaNode lambdae}, which
     *     should not be put into scope automatically, being anonymous. This
     *     will, however, ensure the bytecode of all functions gets written to
     *     the output file.
     * </p>
     *
     * @param info The (already-compiled) function info
     * @return The index of the function when written
     */
    public int addFunction(@NotNull Function info) {
        functions.add(info);
        return functions.size() - 1;
    }

    /**
     * Get the {@link FunctionInfo} representing a function.
     *
     * @param name The name of the function
     * @return The function info, or {@code null} if not found
     */
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
     * Get the index of a function in the function list.
     *
     * @param name The name of the function
     * @return The index, or {@code -1} if not found
     */
    public short fnIndex(String name) {
        for (int i = 0; i < functions.size(); i++) {
            var fn = functions.get(i);
            if (fn != null && fn.getName().equals(name)) {
                return (short) i;
            }
        }
        return -1;
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

    /**
     * Gets the {@link LangConstant} given the index.
     * Currently used only for {@link Bytecode} formatting.
     *
     * @param index The index of the constant
     * @return The constant itself
     */
    public LangConstant getConstant(short index) {
        return constants.get(index);
    }

    /**
     * Adds a class to the class pool.
     * <p>
     *     Like its friend {@link #addFunction}, this does not link a name
     *     to the class, that must be done separately. This is both for local
     *     classes, which may have different scoping rules, and internal
     *     consistency. This will ensure the class gets written to the bytecode
     *     file at the end of compilation.
     * </p>
     *
     * @param info The {@link ClassInfo} representing the class
     * @return The index of the class in the class pool
     */
    public int addClass(ClassInfo info) {
        classes.add(info);
        return classes.indexOf(info);
    }

    /**
     * Links this.
     * <p>
     *     Most of linking is done using the {@link Linker}, and is described
     *     {@link Linker#link there}. If this has already been linked, nothing
     *     happens. Otherwise, it runs the Linker, and then takes its exports
     *     and globals and makes them accessible for later compilation. Linking
     *     is run as part of {@link #compile compilation}, as well as before
     *     getting imports.
     * </p><p>
     *     Because of the link-check that occurs at the beginning of the
     *     function, calling this multiple times is (almost) zero-cost.
     *     Accordingly, one should call this method before doing
     *     anything that might require information being processed at link-time
     *     if the link status is in any doubt.
     * </p>
     *
     * @return Itself
     */
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

    /**
     * Writes itself to the file specified.
     * <p>
     *     The file layout is as follows:
     * <code><pre>
     * Magic number: 0xABADE66
     * Imports:
     *     Name of import
     *     Name of import
     * Exports:
     *     Name of export
     *     Index of constant
     * Constants:
     *     Byte representation of each constant ({@link LangConstant#toBytes})
     * Functions:
     *     Function name
     *     Whether it is a generator or not
     *     Number of local variables (currently unused)
     *     Length of the bytecode
     *     Bytecode
     * Classes:
     *     Byte representation of the class ({@link ClassInfo#toBytes})
     * </pre></code>
     * </p>
     *
     * @param file The file to write to
     */
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
                writer.write(function.isGenerator() ? 1 : 0);
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

    /**
     * Gets the class, given its name.
     *
     * @param str The name of the class
     * @return The class, or {@code null} if not found
     */
    @Nullable
    public TypeObject classOf(String str) {
        var cls = typeMap.get(str);
        if (cls == null) {
            var builtin = Builtins.BUILTIN_MAP.get(str);
            return builtin instanceof TypeObject ? (TypeObject) builtin : null;
        }
        return cls;
    }

    /**
     * Adds a type to the map.
     *
     * @param type The type to add
     */
    public void addType(NameableType type) {
        typeMap.put(type.name(), type);
    }

    /**
     * If the type has been previously been defined.
     *
     * @param typeName The name of the type
     * @return If the type has been defined
     */
    public boolean hasType(String typeName) {
        return typeMap.containsKey(typeName);
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

    public boolean varDefinedInCurrentFrame(String name) {
        return variables.get(variables.size() - 1).get(name) != null;
    }

    public void checkDefinition(String name, Lined info) {
        if (varDefinedInCurrentFrame(name)) {
            throw CompilerException.doubleDef(name, declarationInfo(name), info.getLineInfo());
        }
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
    public void addVariable(String name, TypeObject type, LangConstant constValue, @NotNull Lined info) {
        addConstant(constValue);
        addVariable(name, new VariableInfo(type, constValue, info.getLineInfo()));
    }

    /**
     * Adds a variable to the stack.
     *
     * @param name The name of the variable
     * @param type The type of the variable
     * @param isConst If the variable is const
     * @param info The {@link LineInfo} for the variable's definition
     * @return The index of the variable for {@link Bytecode#LOAD_VALUE}
     */
    public short addVariable(String name, TypeObject type, boolean isConst, @NotNull Lined info) {
        var index = (short) varNumbers.getNext();
        addVariable(name, new VariableInfo(type, isConst, index, info.getLineInfo()));
        return index;
    }

    /**
     * Add a variable to the stack.
     *
     * @param name The name of the variable
     * @param type The type of the variable
     */
    public void addVariable(String name, TypeObject type, @NotNull Lined info) {
        addVariable(name, new VariableInfo(type, (short) varNumbers.getNext(), info.getLineInfo()));
    }

    private void addVariable(String name, VariableInfo info) {
         variables.get(variables.size() - 1).put(name, info);
    }

    /**
     * Adds a static variable to the stack.
     * <p>
     *     Static variables get their static index through {@link
     *     VariableInfo#getStaticLocation()} as opposed to non-static variables,
     *     which get their index through {@link VariableInfo#getLocation()}.
     *     This prevents accidental mishaps with loading a static variable
     *     non-statically and vice versa.
     * </p>
     *
     * @param name The name of the variable
     * @param type The type of the variable
     * @param isConst If the variable is const
     * @param info The {@link LineInfo} representing the variable
     * @return The index of the variable for {@link Bytecode#LOAD_STATIC}
     */
    public short addStaticVar(String name, TypeObject type, boolean isConst, @NotNull Lined info) {
        var index = (short) staticVarNumbers.getNext();
        addVariable(name, new VariableInfo(type, isConst, true, index, info.getLineInfo()));
        return index;
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

    /**
     * Checks if a variable is static.
     *
     * @param name The name of the variable to check
     * @return If the variable is static
     */
    public boolean variableIsStatic(String name) {
        var info = varInfo(name);
        return info != null && info.isStatic();
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

    /**
     * The index of the variable in the static variable set.
     *
     * @param name The name of the variable
     * @return The index
     */
    public short staticVarIndex(String name) {
        return Objects.requireNonNull(varInfo(name), "Unknown variable").getStaticLocation();
    }

    /**
     * Get the {@link LineInfo} for the declaration of a variable.
     * <p>
     *     This requires that the variable is defined, or it will throw a
     *     {@link NullPointerException}.
     * </p>
     * @param name The name to check for
     * @return The {@link LineInfo} for the declaration
     */
    @NotNull
    public LineInfo declarationInfo(String name) {
        return Objects.requireNonNull(varInfo(name)).getDeclarationInfo();
    }

    /**
     * Converts multiple {@link TypeLikeNode}s to the corresponding
     * {@link TypeObject}s.
     *
     * @apiNote The single-value version of this is {@link
     *      #getType(TypeLikeNode)}, not {@code typeOf} as might be expected.
     * @param types The types
     * @return The array of translated types
     */
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
     * Gets the path to the compiled file.
     *
     * @return The path
     */
    public Path path() {
        return node.getPath();
    }

    /**
     * Adds the given function returns to the file.
     *
     * @param values The values to be added
     * @see #currentFnReturns()
     */
    public void addFunctionReturns(TypeObject[] values) {
        fnReturns.push(Pair.of(false, values));
    }

    public void addFunctionReturns(boolean isGen, TypeObject[] values) {
        fnReturns.push(Pair.of(isGen, values));
    }

    /**
     * Gets the return values of the currently-being-compiled function, as
     * given in {@link #addFunctionReturns}.
     *
     * @return The returns of the current function
     * @see #addFunctionReturns(TypeObject[])
     */
    public TypeObject[] currentFnReturns() {
        assert fnReturns.peekFirst() != null;
        return fnReturns.peekFirst().getValue();
    }

    public boolean isGenerator() {
        assert fnReturns.peekFirst() != null;
        return fnReturns.peekFirst().getKey();
    }

    /**
     * Pops the function returns, for when the function is done being compiled.
     *
     * @see #addFunctionReturns(TypeObject[])
     * @see #currentFnReturns()
     */
    public void popFnReturns() {
        fnReturns.pop();
    }

    /**
     * If the compiler is currently not compiling a function.
     *
     * @return If no function returns exist
     */
    public boolean notInFunction() {
        return fnReturns.isEmpty();
    }

    /**
     * Get a globally-unique lambda name.
     * <p>
     *     This name is guaranteed to not match any valid variable name, as well
     *     as not match any other lambda name generated using this function.
     * </p>
     *
     * @implNote This name is of the form {@code lambda$[x]}, where {@code [x]}
     *      is a unique integer.
     * @return The unique lambda name
     */
    public String lambdaName() {
        return String.format("lambda$%d", anonymousNums.getNext());
    }

    /**
     * The current access level of the given {@link TypeObject}.
     * <p>
     *     The access level is the highest-security level which is allowed
     *     to be accessed by values in the current scope. All security levels
     *     less strict than the one given may be used. Others will throw an
     *     exception at some point in compilation
     * </p>
     * @param obj The object to get the access level for
     * @return The security access level of the type
     */
    public DescriptorNode accessLevel(TypeObject obj) {
        return classesWithAccess.contains(obj) ? DescriptorNode.PRIVATE
                : classesWithProtected.contains(obj) ? DescriptorNode.PROTECTED : DescriptorNode.PUBLIC;
    }

    /**
     * Allows private access to the {@link TypeObject} given.
     *
     * @param obj The object to allow private access for
     * @see #removePrivateAccess
     */
    public void allowPrivateAccess(TypeObject obj) {
        classesWithAccess.increment(obj);
    }

    /**
     * Removes private access for the {@link TypeObject} given.
     * <p>
     *     This does not guarantee that the value returned by {@link
     *     #accessLevel} will be more strict than {@code private}, as it is
     *     possible that private access was given in another location. Private
     *     access will last until this has been called as many times as {@link
     *     #allowPrivateAccess}, in order to allow correct access.
     * </p>
     *
     * @param obj The object to remove private access for
     * @see #allowPrivateAccess
     */
    public void removePrivateAccess(TypeObject obj) {
        assert classesWithAccess.contains(obj);
        classesWithAccess.decrement(obj);
    }

    /**
     * Allows protected access to the {@link TypeObject} given.
     *
     * @param obj The object to allow protected access for
     * @see #removeProtectedAccess
     */
    public void allowProtectedAccess(TypeObject obj) {
        classesWithProtected.increment(obj);
    }

    /**
     * Removes protected access for the {@link TypeObject} given.
     * <p>
     *     This does not guarantee that the value returned by {@link
     *     #accessLevel} will be more strict than {@code protected}, as it is
     *     possible that protected access was given in another location.
     *     Protected access will last until this has been called as many times
     *     as {@link #allowProtectedAccess}, in order to allow correct access.
     * </p>
     *
     * @param obj The object to remove private access for
     * @see #allowProtectedAccess
     */
    public void removeProtectedAccess(TypeObject obj) {
        assert classesWithProtected.contains(obj);
        classesWithProtected.decrement(obj);
    }

    /**
     * Add the predeclared {@link TypeObject type objects} to the info.
     *
     * This function may only be called once.
     *
     * @param types The types to add
     */
    void addPredeclaredTypes(Map<String, TypeObject> types) {
        assert !linked && typeMap.isEmpty();
        typeMap.putAll(types);
    }

    {  // Prevent "non-updating" compiler warning
        anonymousNums.remove(0);
        staticVarNumbers.remove(0);
    }
}
