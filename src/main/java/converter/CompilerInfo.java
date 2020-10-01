package main.java.converter;

import main.java.parser.ImportExportNode;
import main.java.parser.LineInfo;
import main.java.parser.Lined;
import main.java.parser.TopNode;
import main.java.parser.TypeLikeNode;
import main.java.parser.TypeNode;
import main.java.parser.VariableNode;
import main.java.util.IndexedSet;
import main.java.util.IntAllocator;
import main.java.util.Pair;
import main.java.util.Zipper;
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
    private static final GlobalCompilerInfo GLOBAL_INFO = new GlobalCompilerInfo();

    private final TopNode node;
    private final ImportHandler importHandler = new ImportHandler(this);
    private final int staticIndex;
    private final Map<String, Integer> fnIndices = new HashMap<>();
    private final LoopManager loopManager = new LoopManager();

    private final List<Map<String, VariableInfo>> variables = new ArrayList<>();
    private final Map<String, TypeObject> typeMap = new HashMap<>();
    private final List<Map<String, TypeObject>> localTypes = new ArrayList<>();
    private final IntAllocator varNumbers = new IntAllocator();

    private final FunctionReturnInfo fnReturns = new FunctionReturnInfo();

    private final AccessHandler accessHandler = new AccessHandler();

    private boolean linked = false;
    private boolean compiled = false;
    private boolean dependentsFound = false;

    public CompilerInfo(TopNode node) {
        this.node = node;
        this.staticIndex = GLOBAL_INFO.reserveStatic();
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
        GLOBAL_INFO.setStatic(staticIndex, bytes);
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
        int index = GLOBAL_INFO.addFunction(info);
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
        return index == null ? Optional.empty() : Optional.of(GLOBAL_INFO.getFunction(index).getInfo());
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
        return GLOBAL_INFO.getFunctions();
    }

    /**
     * Add a constant to the constant pool.
     *
     * @param value The value to add
     */
    public short addConstant(LangConstant value) {
        GLOBAL_INFO.addConstant(value);
        if (GLOBAL_INFO.indexOf(value) > Short.MAX_VALUE) {
            throw new RuntimeException("Too many constants");
        }
        return (short) GLOBAL_INFO.indexOf(value);
    }

    /**
     * The index of a constant in the constant stack.
     *
     * @param value The name of the variable
     * @return The index in the stack
     */
    public short constIndex(LangConstant value) {
        return GLOBAL_INFO.containsConst(value) ? (short) GLOBAL_INFO.indexOf(value) : addConstant(value);
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
        var constant = GLOBAL_INFO.getConstant(index);
        if (!(constant instanceof TempConst)) {
            throw CompilerInternalError.of("Cannot redefine constant not a TempConst", LineInfo.empty());
        }
        assert constant.getType().equals(value.getType());
        GLOBAL_INFO.setConstant(index, value);
    }

    /**
     * Gets the {@link LangConstant} given the index.
     * Currently used only for {@link Bytecode} formatting.
     *
     * @param index The index of the constant
     * @return The constant itself
     */
    public LangConstant getConstant(short index) {
        return GLOBAL_INFO.getConstant(index);
    }

    public IndexedSet<LangConstant> getConstants() {
        return GLOBAL_INFO.getConstants();
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
        return GLOBAL_INFO.addClass(info);
    }

    public IndexedSet<ClassInfo> getClasses() {
        return GLOBAL_INFO.getClasses();
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

    private void addLocals() {
        for (var pair : importHandler.importInfos().entrySet()) {
            var path = pair.getKey();
            var info = pair.getValue();
            var varMap = variables.get(0);
            if (info.getAsNames().isPresent()) {
                for (var pair2 : Zipper.of(info.getNames(), info.getAsNames().get())) {
                    var name = pair2.getKey();
                    var asName = pair2.getValue();
                    if (!varMap.containsKey(asName)) {
                        var type = importHandler.importedType(info, path, name);
                        varMap.put(asName, new VariableInfo(type, true, (short) varMap.size(), info.getLineInfo()));
                    }
                }
            } else if (!info.getNames().get(0).equals("*")) {
                for (var name : info.getNames()) {
                    if (!varMap.containsKey(name)) {
                        var type = importHandler.importedType(info, path, name);
                        varMap.put(name, new VariableInfo(type, true, (short) varMap.size(), info.getLineInfo()));
                    }
                }
            } else {
                var handler = ImportHandler.ALL_FILES.get(path).importHandler;
                for (var export : handler.exportTypes()) {
                    var name = export.getKey();
                    var type = export.getValue();
                    varMap.put(name, new VariableInfo(type, true, (short) varMap.size(), info.getLineInfo()));
                }
            }
        }
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
                var cls = accessHandler.getCls();
                if (cls == null) {
                    throw CompilerException.of("No cls", node);
                }
                return wrap(accessHandler.getCls(), node);
            case "super":
                return wrap(accessHandler.getSuper(), node);
            case "":
                return TypeObject.list(typesOf(node.getSubtypes()));
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
        var mutNode = MutableType.fromNullable(node.getMutability().orElse(null));
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

    /**
     * Gets the constant representing a type.
     * <p>
     *     This is still pretty finicky; in particular there is not support yet
     *     for "complex" types (those with generics, list types, etc.).
     * </p>
     *
     * @param lineInfo The line information to use in case of an exception
     * @param type The type from which to retrieve a constant
     * @return The constant for the type
     */
    @NotNull
    public LangConstant typeConstant(Lined lineInfo, @NotNull TypeObject type) {
        var name = type.baseName();
        if (name.isEmpty()) {
            throw CompilerInternalError.of(
                    "Error in literal conversion: Lists of non-nameable types not complete yet", lineInfo
            );
        }
        if (Builtins.BUILTIN_MAP.containsKey(name) && Builtins.BUILTIN_MAP.get(name) instanceof TypeObject) {
            return Builtins.constantOf(name).orElseThrow(
                    () -> CompilerException.format("Type %s not found", lineInfo, name)
            );
        } else {
            var constants = GLOBAL_INFO.getConstants();
            for (int i = 0; i < constants.size(); i++) {
                var constant = constants.get(i);
                if (constant.getType() instanceof TypeTypeObject && constant.name().equals(name)) {
                    return constant;
                }
            }
            throw CompilerException.format("Type %s not found", lineInfo, name);
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
     * If the type has been previously been defined locally.
     * <p>
     *     Unlike {@link #classOf(String)}, this does not check {@link
     *     Builtins}, as its purpose is for checking if the type has been
     *     linked, and is not for general use.
     * </p>
     *
     * @param typeName The name of the type
     * @return If the type has been defined
     */
    public boolean hasType(String typeName) {
        return typeMap.containsKey(typeName);
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
     */
    public TypeObject getTypeObj(String typeName) {
        return typeMap.get(typeName);
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
     * @param vals The list of values to add to the map
     * @see #addStackFrame()
     * @see #removeLocalTypes()
     */
    public void addLocalTypes(Map<String, TypeObject> vals) {
        localTypes.add(vals);
    }

    /**
     * Removes a frame of local types from the stack.
     * <p>
     *     This method is meant to be used in conjunction with {@link
     *     #addLocalTypes(Map)}. Calls to this method should have a
     *     corresponding call to the other.
     * </p>
     *
     * @see #removeStackFrame()
     * @see #addLocalTypes(Map)
     */
    public void removeLocalTypes() {
        localTypes.remove(localTypes.size() - 1);
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
    public TypeObject getType(String variable) {
        var info = varInfo(variable);
        return info.isEmpty() ? Builtins.constantOf(variable).orElseThrow().getType() : info.orElseThrow().getType();
    }

    /**
     * Checks whether or not the variable is defined.
     *
     * @param name The name of the variable
     * @return If the variable is defined
     */
    public boolean varIsUndefined(String name) {
        return varInfo(name).isEmpty() && !Builtins.BUILTIN_MAP.containsKey(name);
    }

    /**
     * Checks whether the given name is defined in the current variable frame.
     * <p>
     *     The main purpose of this method is checking for double-definition
     *     errors, not whether or not the variable is accessible (for that, see
     *     {@link #varIsUndefined(String)}).
     * </p>
     *
     * @param name The name of the variable to check
     * @return If the variable is defined in the current frame
     * @see #varIsUndefined(String)
     */
    public boolean varDefinedInCurrentFrame(String name) {
        return variables.get(variables.size() - 1).containsKey(name);
    }

    /**
     * Throws an exception if there is already a variable defined in the
     * current frame.
     *
     * @param name The name of the variable
     * @param info The info to use if there is an error
     */
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
        var index = GLOBAL_INFO.claimStaticVar();
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
        return String.format("lambda$%d", GLOBAL_INFO.getAnonymous());
    }

    public String generatorName() {
        return String.format("generator$%d", GLOBAL_INFO.getAnonymous());
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
        return GLOBAL_INFO.addTable(val);
    }

    public List<SwitchTable> getTables() {
        return GLOBAL_INFO.getTables();
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
        var varFrame = variables.get(variables.size() - 1);
        for (var pair : types.entrySet()) {
            var name = pair.getKey();
            var valPair = pair.getValue();
            var obj = valPair.getKey();
            var lined = valPair.getValue();
            typeMap.put(name, obj);
            var varInfo = new VariableInfo(
                    Builtins.TYPE.generify(obj), true, (short) varFrame.size(), lined.getLineInfo()
            );
            varFrame.put(pair.getKey(), varInfo);
        }
    }
}
