package main.java.converter;

import main.java.parser.ImportExportNode;
import main.java.parser.LineInfo;
import main.java.parser.Lined;
import main.java.parser.TopNode;
import main.java.parser.TypeLikeNode;
import main.java.parser.TypeNode;
import main.java.parser.VariableNode;
import main.java.util.IndexedHashSet;
import main.java.util.IndexedSet;
import main.java.util.IntAllocator;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * The class representing all information needing to be held during compilation.
 * @author Patrick Norton
 */
public final class CompilerInfo {
    private final TopNode node;
    private final ImportHandler importHandler = new ImportHandler(this);
    private final List<Function> functions = new ArrayList<>(Collections.singletonList(null));
    private final IndexedSet<LangConstant> constants = new IndexedHashSet<>();
    private final IndexedSet<ClassInfo> classes = new IndexedHashSet<>();
    private final LoopManager loopManager = new LoopManager();
    private final List<SwitchTable> tables = new ArrayList<>();

    private final List<Map<String, VariableInfo>> variables = new ArrayList<>();
    private final Map<String, TypeObject> typeMap = new HashMap<>();
    private final IntAllocator varNumbers = new IntAllocator();
    private final IntAllocator staticVarNumbers = new IntAllocator();

    private final IntAllocator anonymousNums = new IntAllocator();

    private final FunctionReturnInfo fnReturns = new FunctionReturnInfo();

    private final AccessHandler accessHandler = new AccessHandler();

    private boolean linked = false;
    private boolean compiled = false;
    private boolean dependentsFound = false;

    public CompilerInfo(TopNode node) {
        this.node = node;
        variables.add(new HashMap<>());
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
            if (statement instanceof ImportExportNode) {
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
     * Get the {@link ImportHandler} for this file.
     *
     * @return The handler
     * @see ImportHandler
     */
    public ImportHandler importHandler() {
        return importHandler;
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
    @NotNull
    public Optional<FunctionInfo> fnInfo(String name) {
        for (var fn : functions) {
            if (fn != null && fn.getName().equals(name)) {
                return Optional.of(fn.getInfo());
            }
        }
        return Optional.empty();
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

    public List<Function> getFunctions() {
        return functions;
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
        return constIndex(variableInfo.isPresent()
                ? variableInfo.orElseThrow().constValue()
                : Builtins.constantOf(name));
    }

    /**
     * Reserves a constant value for later use.
     *
     * @param type The type of the constant
     * @return The constant index
     * @see TempConst
     */
    public short reserveConst(TypeObject type) {
        return addConstant(new TempConst(type));
    }

    /**
     * Sets the value of a constant to the one given.
     * <p>
     *     The constant to be redefined must be a {@link TempConst}.
     * </p>
     *
     * @param index The index to redefine
     * @param value The value to set this to
     */
    public void setReserved(short index, @NotNull LangConstant value) {
        var constant = constants.get(index);
        if (!(constant instanceof TempConst)) {
            throw CompilerInternalError.of("Cannot redefine constant not a TempConst", LineInfo.empty());
        }
        assert constant.getType().equals(value.getType());
        constants.set(index, value);
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

    public IndexedSet<LangConstant> getConstants() {
        return constants;
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

    public IndexedSet<ClassInfo> getClasses() {
        return classes;
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
        loadDependents();
        ImportHandler.loadDefaultInterfaces();
        var linker = new Linker(this).link(node);
        importHandler.setFromLinker(linker);
        linked = true;
        return this;
    }

    public CompilerInfo loadDependents() {
        if (dependentsFound) {
            return this;
        }
        dependentsFound = true;
        importHandler.registerDependents(node);
        return this;
    }

    /**
     * Writes itself to the file specified.
     * <p>
     *    For more information on the file layout, see {@link
     *    FileWriter#writeToFile}.
     * </p>
     *
     * @param file The file to write to
     * @see FileWriter#writeToFile
     */
    public void writeToFile(@NotNull File file) {
        new FileWriter(this).writeToFile(file);
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
        assert type instanceof TypeNode;
        var node = (TypeNode) type;
        var name = node.getName().toString();
        switch (name) {
            case "null":
                assert node.getSubtypes().length == 0;
                if (node.isOptional()) {
                    CompilerWarning.warn("Type 'null?' is equivalent to null", type.getLineInfo());
                }
                return Builtins.NULL_TYPE;
            case "cls":
                return wrap(accessHandler.getCls(), node);
            case "super":
                return wrap(accessHandler.getSuper(), node);
            case "":
                return new ListTypeObject(typesOf(node.getSubtypes()));
        }
        var value = typeMap.get(type.strName());
        if (value == null) {
            var builtin = Builtins.BUILTIN_MAP.get(type.strName());
            if (builtin instanceof TypeObject) {
                var typeObj = (TypeObject) builtin;
                var endType = type.getSubtypes().length == 0
                        ? typeObj
                        : typeObj.generify(type, typesOf(type.getSubtypes()));
                return wrap(endType, node);
            } else {
                throw CompilerException.of("Unknown type " + type, type);
            }
        } else {
            var endType = type.getSubtypes().length == 0 ? value : value.generify(type, typesOf(type.getSubtypes()));
            return wrap(endType, node);
        }
    }

    private static TypeObject wrap(TypeObject obj, @NotNull TypeLikeNode node) {
        var mutNode = node.getMutability().map(MutableType::fromDescriptor).orElse(MutableType.STANDARD);
        if (!mutNode.isConstType()) {
            if (node.isOptional()) {
                return TypeObject.optional(obj.makeMut());
            } else {
                return obj.makeMut();
            }
        } else {
            if (node.isOptional()) {
                return TypeObject.optional(obj.makeConst());
            } else {
                return obj.makeConst();
            }
        }
    }

    /**
     * Gets the class, given its name.
     *
     * @param str The name of the class
     * @return The class, or {@code null} if not found
     */
    @NotNull
    public Optional<TypeObject> classOf(String str) {
        var cls = typeMap.get(str);
        if (cls == null) {
            var builtin = Builtins.BUILTIN_MAP.get(str);
            return builtin instanceof TypeObject ? Optional.of((TypeObject) builtin) : Optional.empty();
        }
        return Optional.of(cls);
    }

    public Optional<Integer> classIndex(String str) {
        var index = typeMap.get(str);
        if (index == null) {
            return Optional.empty();
        } else {
            for (int i = 0; i < classes.size(); i++) {
                if (classes.get(i).getType().equals(index)) {
                    return Optional.of(i);
                }
            }
            throw new IllegalStateException("If a type is in typeMap, it should be in classes");
        }
    }

    @NotNull
    public LangConstant typeConstant(@NotNull TypeObject type) {
        var name = type.baseName();
        if (name.isEmpty()) {
            throw CompilerInternalError.of(
                    "Error in literal conversion: Lists of non-nameable types not complete yet", node
            );
        }
        if (Builtins.BUILTIN_MAP.containsKey(name) && Builtins.BUILTIN_MAP.get(name) instanceof TypeObject) {
            return Builtins.constantOf(name);
        } else {
            for (int i = 0; i < constants.size(); i++) {
                var constant = constants.get(i);
                if (constant.getType() instanceof TypeTypeObject && constant.name().equals(name)) {
                    return constant;
                }
            }
            throw new NoSuchElementException("Type not found");
        }
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

    public TypeObject getTypeObj(String typeName) {
        return typeMap.get(typeName);
    }

    /**
     * Get the type of a variable.
     *
     * @param variable The variable name to get the type from
     * @return The type of the variable
     */
    public TypeObject getType(String variable) {
        var info = varInfo(variable);
        return info.isEmpty() ? Builtins.constantOf(variable).getType() : info.orElseThrow().getType();
    }

    public boolean varIsUndefined(String name) {
        return varInfo(name).isEmpty() && !Builtins.BUILTIN_MAP.containsKey(name);
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
     * Reserves a constant variable for future use.
     * <p>
     *     This allows for the variable to be used normally, and does not expose
     *     the fact that this is undefined to the rest of the bytecode
     *     generator, but it does require that the constant be changed from a
     *     temporary (using {@link #setReservedVar} before the compiled unit is
     *     written out to a file. As such, this
     * </p>
     *
     * @param name The name of the variable to add
     * @param type The type of the variable
     * @param info The info for the declaration of the variable
     * @see #setReservedVar
     * @see TempConst
     * @see #addVariable(String, TypeObject, LangConstant, Lined)
     */
    public void reserveConstVar(String name, TypeObject type, @NotNull Lined info) {
        var constant = new TempConst(type);
        addConstant(constant);
        addVariable(name, new VariableInfo(type, constant, info.getLineInfo()));
    }

    /**
     * Sets the previously-reserved variable to the {@link LangConstant} value
     * given.
     * <p>
     *     This requires three things: That the variable whose name is given is
     *     defined, that the variable was defined using {@link
     *     #reserveConstVar}, and that this function has not yet been called on
     *     this variable.
     * </p>
     *
     * @param name The name to set
     * @param value The value
     * @see #reserveConstVar
     */
    public void setReservedVar(String name, LangConstant value) {
        var varInfo = varInfo(name).orElseThrow();
        var constant = varInfo.constValue();
        var constIndex = constIndex(constant);
        setReserved(constIndex, value);
        replaceVarInfo(name, new VariableInfo(value.getType(), value, varInfo.getDeclarationInfo()));
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
        return info.map(VariableInfo::hasConstValue).orElseGet(() -> Builtins.BUILTIN_MAP.containsKey(name));
    }

    /**
     * Checks if a variable is static.
     *
     * @param name The name of the variable to check
     * @return If the variable is static
     */
    public boolean variableIsStatic(String name) {
        var info = varInfo(name);
        return info.map(VariableInfo::isStatic).orElse(false);
    }

    @NotNull
    private Optional<VariableInfo> varInfo(String name) {  // TODO: Universally accessible globals
        for (int i = variables.size() - 1; i >= 0; i--) {
            var map = variables.get(i);
            if (map.containsKey(name)) {
                return Optional.of(map.get(name));
            }
        }
        return Optional.empty();
    }

    private void replaceVarInfo(String name, VariableInfo varInfo) {
        for (int i = variables.size() - 1; i >= 0; i--) {
            var map = variables.get(i);
            if (map.containsKey(name)) {
                map.put(name, varInfo);
            }
        }
    }

    /**
     * The index of the variable in the variable stack.
     *
     * @param node The node representing the variable
     * @return The index in the stack
     */
    public short varIndex(@NotNull VariableNode node) {
        return varInfo(node.getName()).orElseThrow(
                () -> CompilerException.format("Unknown variable '%s'", node, node.getName())
        ).getLocation();
    }

    /**
     * The index of the variable in the static variable set.
     *
     * @param node The node representing the variable
     * @return The index
     */
    public short staticVarIndex(@NotNull VariableNode node) {
        return varInfo(node.getName()).orElseThrow(
                () -> CompilerException.format("Unknown variable '%s'", node, node.getName())
        ).getStaticLocation();
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
        return varInfo(name).orElseThrow().getDeclarationInfo();
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

    public FunctionReturnInfo getFnReturns() {
        return fnReturns;
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

    public String generatorName() {
        return String.format("generator$%d", anonymousNums.getNext());
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
    public AccessLevel accessLevel(TypeObject obj) {
        return accessHandler.accessLevel(obj);
    }

    /**
     * The path to the file being compiled.
     *
     * @return The path
     */
    public Path sourceFile() {
        return node.getPath();
    }

    /**
     * The object in charge of access levels for the file.
     *
     * @return The handler
     */
    public AccessHandler accessHandler() {
        return accessHandler;
    }

    public int addSwitchTable(SwitchTable val) {
        tables.add(val);
        return tables.size() - 1;
    }

    public List<SwitchTable> getTables() {
        return tables;
    }

    /**
     * Add the predeclared {@link TypeObject type objects} to the info.
     *
     * This function may only be called once.
     *
     * @param types The types to add
     */
    void addPredeclaredTypes(Map<String, TypeObject> types) {
        assert !linked;
        typeMap.putAll(types);
        var varFrame = variables.get(variables.size() - 1);
        for (var pair : types.entrySet()) {
            var varInfo = new VariableInfo(  // FIXME: Better VariableInfo
                    Builtins.TYPE.generify(pair.getValue()), true, (short) varFrame.size(), LineInfo.empty()
            );
            varFrame.put(pair.getKey(), varInfo);
        }
    }

    {  // Prevent "non-updating" compiler warning
        anonymousNums.remove(0);
        staticVarNumbers.remove(0);
    }
}
