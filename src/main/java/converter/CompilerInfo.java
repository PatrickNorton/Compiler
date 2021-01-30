package main.java.converter;

import main.java.parser.ImportExportNode;
import main.java.parser.LineInfo;
import main.java.parser.Lined;
import main.java.parser.TopNode;
import main.java.parser.TypeLikeNode;
import main.java.parser.VariableNode;
import main.java.util.IndexedSet;
import main.java.util.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The class representing all information needing to be held during compilation.
 * @author Patrick Norton
 */
public final class CompilerInfo {
    private final GlobalCompilerInfo globalInfo;

    private final TopNode node;
    private final ImportHandler importHandler = new ImportHandler(this);
    private final int staticIndex;
    private final Map<String, Integer> fnIndices = new HashMap<>();
    private final LoopManager loopManager = new LoopManager();
    private final WarningHolder warnings = new WarningHolder();

    private final VariableHolder varHolder;

    private final FunctionReturnInfo fnReturns = new FunctionReturnInfo();

    private boolean linked = false;
    private boolean compiled = false;
    private boolean dependentsFound = false;

    public CompilerInfo(TopNode node, GlobalCompilerInfo globalInfo) {
        this.node = node;
        this.globalInfo = globalInfo;
        this.varHolder = new VariableHolder(globalInfo);
        this.staticIndex = globalInfo.reserveStatic();
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
     * @return Itself
     */
    public CompilerInfo compile() {
        if (compiled) {
            return this;
        }
        link();
        addLocals();
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
        globalInfo.setStatic(staticIndex, bytes);
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

    public GlobalCompilerInfo globalInfo() {
        return globalInfo;
    }

    public WarningHolder warningHolder() {
        return warnings;
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
        int index = globalInfo.addFunction(info);
        fnIndices.put(info.getName(), index);
        return index;
    }

    /**
     * Get the {@link FunctionInfo} representing a function.
     *
     * @param name The name of the function
     * @return The function info, or {@code null} if not found
     */
    @NotNull
    public Optional<FunctionInfo> fnInfo(String name) {
        var index = fnIndices.get(name);
        return index == null ? Builtins.functionOf(name) : Optional.of(globalInfo.getFunction(index).getInfo());
    }

    public Optional<Function> getFn(String name) {
        var index = fnIndices.get(name);
        return index == null ? Optional.empty() : Optional.of(globalInfo.getFunction(index));
    }

    /**
     * Get the index of a function in the function list.
     *
     * @param name The name of the function
     * @return The index, or {@code -1} if not found
     */
    public short fnIndex(String name) {
        var index = fnIndices.get(name);
        return index == null ? -1 : index.shortValue();
    }

    public List<Function> getFunctions() {
        return globalInfo.getFunctions();
    }

    /**
     * Add a constant to the constant pool.
     *
     * @param value The value to add
     */
    public short addConstant(LangConstant value) {
        globalInfo.addConstant(value);
        if (globalInfo.indexOf(value) > Short.MAX_VALUE) {
            throw new RuntimeException("Too many constants");
        }
        return (short) globalInfo.indexOf(value);
    }

    /**
     * The index of a constant in the constant stack.
     *
     * @param value The name of the variable
     * @return The index in the stack
     */
    public short constIndex(LangConstant value) {
        return globalInfo.containsConst(value) ? (short) globalInfo.indexOf(value) : addConstant(value);
    }

    /**
     * The index of a constant variable in the constant stack.
     *
     * @param name The name of the variable
     * @return The index in the stack
     */
    public short constIndex(String name) {
        var variableInfo = varHolder.varInfo(name);
        return constIndex(variableInfo.isPresent()
                ? variableInfo.orElseThrow().constValue()
                : Builtins.constantOf(name).orElseThrow());
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
        var constant = globalInfo.getConstant(index);
        if (!(constant instanceof TempConst)) {
            throw CompilerInternalError.of("Cannot redefine constant not a TempConst", LineInfo.empty());
        }
        assert constant.getType().equals(value.getType());
        globalInfo.setConstant(index, value);
    }

    /**
     * Gets the {@link LangConstant} given the index.
     * Currently used only for {@link Bytecode} formatting.
     *
     * @param index The index of the constant
     * @return The constant itself
     */
    public LangConstant getConstant(short index) {
        return globalInfo.getConstant(index);
    }

    public IndexedSet<LangConstant> getConstants() {
        return IndexedSet.unmodifiable(globalInfo.getConstants());
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
        return globalInfo.addClass(info);
    }

    public List<ClassInfo> getClasses() {
        return globalInfo.getClasses();
    }

    public int reserveClass(UserType<?> type) {
        return globalInfo.reserveClass(type);
    }

    public int setClass(ClassInfo info) {
        return globalInfo.setClass(info);
    }

    public int classIndex(UserType<?> type) {
        return globalInfo.classIndex(type);
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

    public void addGlobals(Map<String, TypeObject> globals, Map<String, Integer> constants) {
        varHolder.addGlobals(globals, constants);
    }

    private void addLocals() {
        varHolder.addLocals(importHandler);
    }

    public void loadDependents() {
        if (!dependentsFound) {
            dependentsFound = true;
            importHandler.registerDependents(node);
        }
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

    public VariableHolder varHolder() {
        return varHolder;
    }

    /**
     * Get the compiler's type from a {@link TypeLikeNode}.
     *
     * @param type The node to translate
     * @return The compiler's type
     * @see VariableHolder#getType
     */
    @NotNull
    @Contract(pure = true)
    public TypeObject getType(@NotNull TypeLikeNode type) {
        return varHolder.getType(type);
    }

    /**
     * Gets the class, given its name.
     *
     * @param str The name of the class
     * @return The class, or {@code null} if not found
     * @see VariableHolder#classOf
     */
    @NotNull
    public Optional<TypeObject> classOf(String str) {
        return varHolder.classOf(str);
    }

    public Optional<TypeObject> localParent(TypeObject typ) {
        return varHolder.localParent(typ);
    }

    /**
     * Adds a type to the map.
     *
     * @param type The type to add
     * @see VariableHolder#addType
     */
    public void addType(TypeObject type) {
        varHolder.addType(type);
    }

    /**
     * If the type has been previously been defined locally.
     * <p>
     *     Unlike {@link #classOf(String)}, this does not check {@link
     *     Builtins}, as its purpose is for checking if the type has been
     *     linked, and is not for general use.
     * </p>
     *
     * @param typeName The name of the type
     * @return If the type has been defined
     * @see VariableHolder#hasType
     */
    public boolean hasType(String typeName) {
        return varHolder.hasType(typeName);
    }

    /**
     * Gets the (locally-defined) type with the given name.
     * <p>
     *     This method is meant to be used in conjunction with {@link
     *     #hasType(String)}, and will return {@code null} iff that returns
     *     {@code false}. Like {@link #hasType(String)}, this does not check
     *     {@link Builtins}; {@link #classOf(String)} should be used for that
     *     purpose.
     * </p>
     *
     * @param typeName The name of the type
     * @return The object representing the type
     * @see VariableHolder#getTypeObj
     */
    public TypeObject getTypeObj(String typeName) {
        return varHolder.getTypeObj(typeName);
    }

    /**
     * Adds a frame of local types to the stack.
     * <p>
     *     This is similar to {@link #addStackFrame()}, but is used for
     *     generics (and eventually local types, when they are implemented),
     *     where the type definitions are not accessible for the full file,
     *     simply the region specified.
     * </p>
     * <p>
     *     This method is meant to be used in conjunction with {@link
     *     #removeLocalTypes()}. Calls to this method should have a
     *     corresponding call to the other.
     * </p>
     *
     * @param values The list of values to add to the map
     * @see #addStackFrame()
     * @see #removeLocalTypes()
     * @see VariableHolder#addLocalTypes
     */
    public void addLocalTypes(TypeObject parent, Map<String, TypeObject> values) {
        varHolder.addLocalTypes(parent, values);
    }

    /**
     * Removes a frame of local types from the stack.
     * <p>
     *     This method is meant to be used in conjunction with {@link
     *     #addLocalTypes(TypeObject, Map)}. Calls to this method should have a
     *     corresponding call to the other.
     * </p>
     *
     * @see #removeStackFrame()
     * @see #addLocalTypes(TypeObject, Map)
     * @see VariableHolder#removeLocalTypes
     */
    public void removeLocalTypes() {
        varHolder.removeLocalTypes();
    }

    /**
     * Gets the type of a variable.
     * <p>
     *     This is <i>not</i> equivalent to {@link #classOf(String)}. That
     *     method gets the {@link TypeObject type} whose name is the given
     *     string, while this method gets the type of any variable. It should,
     *     however, be the case that if {@link #classOf(String)} returns a
     *     non-{@link Optional#empty() empty} value {@code t}, then this method
     *     should return <code>t.{@link TypeObject#getType() getType()}</code>.
     * </p>
     *
     * @param variable The variable name to get the type from
     * @return The type of the variable
     * @see #classOf(String)
     */
    public Optional<TypeObject> getType(String variable) {
        var info = varHolder.varInfo(variable);
        return info.isEmpty()
                ? Builtins.constantOf(variable).map(LangConstant::getType)
                : info.map(VariableInfo::getType);
    }

    /**
     * Checks whether or not the variable is defined.
     *
     * @param name The name of the variable
     * @return If the variable is defined
     */
    public boolean varIsUndefined(String name) {
        return varHolder.varInfo(name).isEmpty() && !Builtins.BUILTIN_MAP.containsKey(name);
    }

    public Iterable<String> definedNames() {
        return varHolder.definedNames();
    }

    /**
     * Throws an exception if there is already a variable defined in the
     * current frame.
     *
     * @param name The name of the variable
     * @param info The info to use if there is an error
     */
    public void checkDefinition(String name, Lined info) {
        if (varHolder.varDefinedInCurrentFrame(name)) {
            var declInfo = varHolder.varInfo(name).orElseThrow().getDeclarationInfo();;
            throw CompilerException.doubleDef(name, declInfo, info.getLineInfo());
        }
    }

    /**
     * Add a new set of variable names to the stack.
     *
     * @see VariableHolder#addStackFrame()
     */
    public void addStackFrame() {
        varHolder.addStackFrame();
    }

    /**
     * Remove the current level of variable declarations from the stack.
     *
     * @see VariableHolder#removeStackFrame()
     */
    public void removeStackFrame() {
        varHolder.removeStackFrame();
    }

    /**
     * Add a variable with a constant value to the stack.
     *
     * @param name The name of the variable
     * @param type The type of the variable
     * @param constValue The constant value the variable has
     */
    public void addVariable(String name, TypeObject type, LangConstant constValue, @NotNull Lined info) {
        varHolder.addVariable(name, type, constValue, info);
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
        return varHolder.addVariable(name, type, isConst, info);
    }

    /**
     * Add a variable to the stack.
     *
     * @param name The name of the variable
     * @param type The type of the variable
     */
    public void addVariable(String name, TypeObject type, @NotNull Lined info) {
        varHolder.addVariable(name, type, info);
    }

    private void addVariable(String name, VariableInfo info) {
         varHolder.addVariable(name, info);
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
        var varInfo = varHolder.varInfo(name).orElseThrow();
        var constant = varInfo.constValue();
        var constIndex = constIndex(constant);
        setReserved(constIndex, value);
        varHolder.replaceVarInfo(name, new VariableInfo(value.getType(), value, varInfo.getDeclarationInfo()));
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
        var index = globalInfo.claimStaticVar();
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
        return varHolder.varInfo(name).map(VariableInfo::hasConstValue).orElse(true);
    }

    /**
     * Checks if a variable is immutable.
     *
     * @param name The name of the variable to check
     * @return If the variable is immutable
     */
    public boolean variableIsImmutable(String name) {
        return varHolder.varInfo(name).map(VariableInfo::isConst).orElse(true);
    }

    /**
     * Checks if a variable is static.
     *
     * @param name The name of the variable to check
     * @return If the variable is static
     */
    public boolean variableIsStatic(String name) {
        var info = varHolder.varInfo(name);
        return info.map(VariableInfo::isStatic).orElse(false);
    }

    /**
     * The index of the variable in the variable stack.
     *
     * @param node The node representing the variable
     * @return The index in the stack
     */
    public short varIndex(@NotNull VariableNode node) {
        return varHolder.varInfo(node.getName()).orElseThrow(
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
        return varHolder.varInfo(node.getName()).orElseThrow(
                () -> CompilerException.format("Unknown variable '%s'", node, node.getName())
        ).getStaticLocation();
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
        return String.format("lambda$%d", globalInfo.getAnonymous());
    }

    public String generatorName() {
        return String.format("generator$%d", globalInfo.getAnonymous());
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
        return accessHandler().accessLevel(obj);
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
        return varHolder.accessHandler();
    }

    public int addSwitchTable(SwitchTable val) {
        return globalInfo.addTable(val);
    }

    public List<SwitchTable> getTables() {
        return globalInfo.getTables();
    }

    /**
     * Add the predeclared {@link TypeObject type objects} to the info.
     *
     * This function may only be called once.
     *
     * @param types The types to add
     */
    void addPredeclaredTypes(@NotNull Map<String, Pair<TypeObject, Lined>> types) {
        assert !linked;
        varHolder.addPredeclaredTypes(types);
    }
}
