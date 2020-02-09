package main.java.converter;

import main.java.parser.LineInfo;
import main.java.parser.TypeLikeNode;
import main.java.parser.TypeNode;
import main.java.parser.TypeUnionNode;
import main.java.parser.TypewiseAndNode;
import main.java.util.IntAllocator;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    private FileInfo parent;
    private Deque<Boolean> loopLevel;
    private Map<Integer, Set<Integer>> breakPointers;
    private Map<Integer, Set<Integer>> continuePointers;
    private Deque<Integer> continueLocations;

    private List<Map<String, VariableInfo>> variables;
    private Map<String, StdTypeObject> typeMap;
    private IntAllocator varNumbers;

    public CompilerInfo(FileInfo parent) {
        this.parent = parent;
        this.loopLevel = new ArrayDeque<>();
        this.breakPointers = new HashMap<>();
        this.continuePointers = new HashMap<>();
        this.continueLocations = new ArrayDeque<>();
        this.variables = new ArrayList<>();
        this.typeMap = new HashMap<>();
        this.varNumbers = new IntAllocator();
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
     * Add a constant to the constant pool.
     *
     * @param value The value to add
     */
    public int addConstant(LangConstant value) {
        return parent.addConstant(value);
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
            return new UnionTypeObject(typesOf(union.getSubtypes()));
        } else if (type instanceof TypewiseAndNode) {
            var union = (TypewiseAndNode) type;
            return new IntersectionTypeObject(typesOf(union.getSubtypes()));
        } else {
            assert type instanceof TypeNode;
            var value = typeMap.get(type.strName());
            if (value == null) {
                var builtin = Builtins.BUILTIN_MAP.get(type.strName());
                if (builtin instanceof TypeObject) {
                    return (TypeObject) builtin;
                } else {
                    throw new RuntimeException("Unknown type " + type);
                }
            } else {
                return value;
            }
        }
    }

    public void addType(StdTypeObject type) {
        typeMap.put(type.name(), type);
    }

    public int addClass(ClassInfo info) {
        return parent.addClass(info);
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
     * Add an import to the import list.
     *
     * @param name The name of the import to add
     * @return The index of the import
     */
    public int addImport(String name) {
        return parent.addImport(name);
    }

    /**
     * Add an export to the export list.
     *
     * @param name The name of the export to add
     * @param type The type of the export
     */
    public void addExport(String name, TypeObject type, LineInfo info) {
        parent.addExport(name, type, info);
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
        addVariable(name, new VariableInfo(type, constValue, -1));
    }

    public void addVariable(String name, TypeObject type, boolean isConst) {
        addVariable(name, new VariableInfo(type, isConst, varNumbers.getNext()));
    }

    /**
     * Add a variable to the stack.
     *
     * @param name The name of the variable
     * @param type The type of the variable
     */
    public void addVariable(String name, TypeObject type) {
        addVariable(name, new VariableInfo(type, varNumbers.getNext()));
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
    private VariableInfo varInfo(String name) {
        for (int i = variables.size() - 1; i >= 0; i--) {
            var map = variables.get(i);
            if (map.containsKey(name)) {
                return map.get(name);
            }
        }
        return null;
    }

    public TypeObject importType(String name) {
        return parent.importType(name);
    }

    /**
     * The index of the variable in the variable stack.
     *
     * @param name The name of the variable
     * @return The index in the stack
     */
    public int varIndex(String name) {
        return Objects.requireNonNull(varInfo(name)).getLocation();
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
     * The index of a constant in the constant stack.
     *
     * @param value The name of the variable
     * @return The index in the stack
     */
    public int constIndex(LangConstant value) {
        return parent.constIndex(value);
    }

    /**
     * The index of a constant variable in the constant stack.
     *
     * @param name The name of the variable
     * @return The index in the stack
     */
    public int constIndex(String name) {
        var variableInfo = varInfo(name);
        return constIndex(variableInfo != null
                ? variableInfo.constValue()
                : Builtins.constantOf(name));
    }

    public int addFunction(Function info) {
        return parent.addFunction(info);
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
