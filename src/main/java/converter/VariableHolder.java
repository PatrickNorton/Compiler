package main.java.converter;

import main.java.parser.LineInfo;
import main.java.parser.Lined;
import main.java.parser.TypeLikeNode;
import main.java.parser.TypeNode;
import main.java.util.IntAllocator;
import main.java.util.IterJoin;
import main.java.util.Levenshtein;
import main.java.util.OptionalUint;
import main.java.util.Pair;
import main.java.util.Zipper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

final class VariableHolder {
    private final GlobalCompilerInfo globalInfo;

    private final AccessHandler accessHandler = new AccessHandler();

    private final List<Map<String, VariableInfo>> variables = new ArrayList<>();
    private final Map<String, TypeObject> typeMap = new HashMap<>();
    private final List<LocalTypeFrame> localTypes = new ArrayList<>();
    private final IntAllocator varNumbers = new IntAllocator();
    private int maxVarSize = 0;

    public VariableHolder(GlobalCompilerInfo globalInfo) {
        this.globalInfo = globalInfo;
        variables.add(new HashMap<>());
    }

    public AccessHandler accessHandler() {
        return accessHandler;
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
            if (!pair.hasConstValue() && !pair.isStatic()) {
                varNumbers.remove(pair.getLocation());
            }
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

    public void addVariable(String name, VariableInfo info) {
         variables.get(variables.size() - 1).put(name, info);
    }

    public void removeVariable(String name) {
        for (int i = variables.size() - 1; i >= 0; i--) {
            var frame = variables.get(i);
            if (frame.containsKey(name)) {
                var value = frame.remove(name);
                if (value.getLocation() != -1) {
                    varNumbers.remove(value.getLocation());
                }
                return;
            }
        }
        throw CompilerInternalError.format("Variable %s not defined", LineInfo.empty(), name);
    }

    public int resetMax() {
        var result = maxVarSize;
        maxVarSize = Collections.max(varNumbers);
        return result;
    }

    /**
     * Adds a type to the map.
     *
     * @param type The type to add
     */
    public void addType(TypeObject type) {
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
     * @param values The list of values to add to the map
     * @see #addStackFrame()
     * @see #removeLocalTypes()
     */
    public void addLocalTypes(TypeObject parent, Map<String, TypeObject> values) {
        localTypes.add(new LocalTypeFrame(parent, values));
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
     */
    public void removeLocalTypes() {
        localTypes.remove(localTypes.size() - 1);
    }

    /**
     * Checks whether the given name is defined in the current variable frame.
     * <p>
     *     The main purpose of this method is checking for double-definition
     *     errors, not whether or not the variable is accessible (for that, see
     *     {@link CompilerInfo#varIsUndefined(String)}).
     * </p>
     *
     * @param name The name of the variable to check
     * @return If the variable is defined in the current frame
     * @see CompilerInfo#varIsUndefined(String)
     */
    public boolean varDefinedInCurrentFrame(String name) {
        return variables.get(variables.size() - 1).containsKey(name);
    }

    @NotNull
    public Optional<VariableInfo> varInfo(String name) {  // TODO: Universally accessible globals
        for (int i = variables.size() - 1; i >= 0; i--) {
            var map = variables.get(i);
            if (map.containsKey(name)) {
                return Optional.of(map.get(name));
            }
        }
        return Optional.empty();
    }

    public void replaceVarInfo(String name, VariableInfo varInfo) {
        for (int i = variables.size() - 1; i >= 0; i--) {
            var map = variables.get(i);
            if (map.containsKey(name)) {
                map.put(name, varInfo);
                return;
            }
        }
    }

    public Iterable<String> definedNames() {
        return DefinedIterator::new;
    }

    public void addGlobals(Map<String, TypeObject> globals, Map<String, Integer> constants) {
        var varFrame = variables.get(0);
        for (var pair : globals.entrySet()) {
            var name = pair.getKey();
            var type = pair.getValue();
            if (!varFrame.containsKey(name)) {
                if (constants.containsKey(name)) {
                    var constant = globalInfo.getConstant(constants.get(name).shortValue());
                    varFrame.put(name, new VariableInfo(type, constant, LineInfo.empty()));
                } else {
                    throw new UnsupportedOperationException();
                }
            }
        }
    }

    public void addLocals(ImportHandler importHandler, WarningHolder warnings) {
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
                        var constIndex = importHandler.importedConstant(info, path, name);
                        varMap.put(asName, getVariableInfo(info, type, constIndex, warnings));
                    }
                }
            } else if (!info.getNames().get(0).equals("*")) {
                for (var name : info.getNames()) {
                    if (!varMap.containsKey(name)) {
                        var type = importHandler.importedType(info, path, name);
                        var constIndex = importHandler.importedConstant(info, path, name);
                        varMap.put(name, getVariableInfo(info, type, constIndex, warnings));
                    }
                }
            } else {
                var handler = ImportHandler.ALL_FILES.get(path).importHandler();
                for (var export : handler.exportTypes()) {
                    var name = export.getKey();
                    var type = export.getValue();
                    var constIndex = importHandler.importedConstant(info, path, name);
                    varMap.put(name, getVariableInfo(info, type, constIndex, warnings));
                }
            }
        }
    }

    @NotNull
    private VariableInfo getVariableInfo(
            ImportInfo info, TypeObject type, @NotNull OptionalUint constIndex, WarningHolder warnings
    ) {
        if (constIndex.isPresent()) {
            var constant = globalInfo.getConstant(constIndex.orElseThrow());
            return new VariableInfo(type, constant, info.getLineInfo());
        } else {
            CompilerWarning.warn(
                    "Import is not a compile-time constant, may fail at runtime", WarningType.NO_TYPE, warnings, info
            );
            return new VariableInfo(type, true, (short) varNumbers.getNext(), info.getLineInfo());
        }
    }

    /**
     * Get the compiler's type from a {@link TypeLikeNode}.
     *
     * @param type The node to translate
     * @return The compiler's type
     */
    public TypeObject getType(@NotNull TypeLikeNode type, WarningHolder warnings) {
        if (!type.isDecided()) {
            throw CompilerInternalError.format("Cannot call 'getType' on 'var'", type);
        }
        assert type instanceof TypeNode;
        var node = (TypeNode) type;
        var name = node.getName().toString();
        switch (name) {
            case "null":
                assert node.getSubtypes().length == 0;
                if (node.isOptional()) {
                    CompilerWarning.warn(
                            "Type 'null?' is equivalent to null", WarningType.TRIVIAL_VALUE, warnings, type
                    );
                }
                return Builtins.nullType();
            case "cls":
                var cls = accessHandler.getCls();
                if (cls == null) {
                    throw CompilerException.of("Type 'cls' is not defined in this scope", node);
                }
                return wrap(accessHandler.getCls(), node);
            case "super":
                if (accessHandler.getSuper() == null) {
                    throw CompilerException.of("Type 'super' is not defined in this scope", node);
                }
                return wrap(accessHandler.getSuper(), node);
            case "":
                return TypeObject.list(typesOf(warnings, node.getSubtypes()));
        }
        var value = typeMap.get(type.strName());
        if (value == null) {
            for (var localType : localTypes) {
                var children = localType.getChildren();
                if (children.containsKey(type.strName())) {
                    return wrap(children.get(type.strName()), node);
                }
            }
            var builtin = Builtins.BUILTIN_MAP.get(type.strName());
            if (builtin instanceof TypeObject) {
                var typeObj = (TypeObject) builtin;
                var endType = type.getSubtypes().length == 0
                        ? typeObj
                        : typeObj.generify(type, typesOf(warnings, type.getSubtypes()));
                return wrap(endType, node);
            } else {
                var names = IterJoin.from(typeMap.keySet(), LocalTypeIterator::new, Builtins.BUILTIN_MAP.keySet());
                var suggestion = Levenshtein.closestName(type.strName(), () -> names);
                if (suggestion.isPresent()) {
                    throw CompilerException.format(
                            "Unknown type '%s'. Did you mean '%s'?", type, type.strName(), suggestion.orElseThrow()
                    );
                } else {
                    throw CompilerException.of("Unknown type " + type, type);
                }
            }
        } else {
            var endType = type.getSubtypes().length == 0 ? value
                    : value.generify(type, typesOf(warnings, type.getSubtypes()));
            return wrap(endType, node);
        }
    }

    public Optional<TypeObject> localParent(TypeObject typ) {
        for (int i = localTypes.size() - 1; i >= 0; i--) {
            var frame = localTypes.get(i);
            if (frame.getChildren().containsKey(typ.baseName())) {
                return Optional.of(frame.getParentType());
            }
        }
        return Optional.empty();
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

    public TypeObject[] typesOf(WarningHolder warningHolder, @NotNull TypeLikeNode... types) {
        var typeObjects = new TypeObject[types.length];
        for (int i = 0; i < types.length; i++) {
            typeObjects[i] = getType(types[i], warningHolder);
        }
        return typeObjects;
    }

    /**
     * Add the predeclared {@link TypeObject type objects} to the info.
     *
     * This function may only be called once.
     *
     * @param types The types to add
     */
    void addPredeclaredTypes(@NotNull Map<String, Pair<TypeObject, Lined>> types) {
        for (var pair : types.entrySet()) {
            var name = pair.getKey();
            var valPair = pair.getValue();
            var obj = valPair.getKey();
            if (typeMap.containsKey(name)) {
                throw CompilerException.doubleDef(name, valPair.getValue().getLineInfo(), LineInfo.empty());
            }
            typeMap.put(name, obj);
        }
    }

    public short addConstant(LangConstant value) {
        globalInfo.addConstant(value);
        if (globalInfo.indexOf(value) > Short.MAX_VALUE) {
            throw new RuntimeException("Too many constants");
        }
        return (short) globalInfo.indexOf(value);
    }

    private final class DefinedIterator implements Iterator<String> {
        private int frameNo = variables.size() - 1;
        private Iterator<String> frameIter = variables.get(frameNo).keySet().iterator();

        @Override
        public boolean hasNext() {
            if (!frameIter.hasNext()) updateIter();
            return frameNo >= 0;
        }

        @Override
        public String next() {
            if (!frameIter.hasNext()) {
                updateIter();
                if (frameNo < 0) {
                    throw new NoSuchElementException();
                }
            }
            return frameIter.next();
        }

        private void updateIter() {
            while (!frameIter.hasNext() && frameNo > 0) {
                frameNo--;
                frameIter = variables.get(frameNo).keySet().iterator();
            }
            if (!frameIter.hasNext()) {
                frameNo--;
            }
        }
    }

    private static final class LocalTypeFrame {
        private final TypeObject parentType;
        private final Map<String, TypeObject> children;

        public LocalTypeFrame(TypeObject parentType, Map<String, TypeObject> children) {
            this.parentType = parentType;
            this.children = children;
        }

        public TypeObject getParentType() {
            return parentType;
        }

        public Map<String, TypeObject> getChildren() {
            return children;
        }
    }

    private final class LocalTypeIterator implements Iterator<String> {
        private int frameNo = localTypes.size() - 1;
        private Iterator<String> frameIter = frameNo >= 0
                ? localTypes.get(frameNo).getChildren().keySet().iterator() : Collections.emptyIterator();

        @Override
        public boolean hasNext() {
            if (!frameIter.hasNext()) updateIter();
            return frameNo >= 0;
        }

        @Override
        public String next() {
            if (!frameIter.hasNext()) {
                updateIter();
                if (frameNo < 0) {
                    throw new NoSuchElementException();
                }
            }
            return frameIter.next();
        }

        private void updateIter() {
            while (!frameIter.hasNext() && frameNo > 0) {
                frameNo--;
                frameIter = localTypes.get(frameNo).getChildren().keySet().iterator();
            }
            if (!frameIter.hasNext()) {
                frameNo--;
            }
        }
    }
}
