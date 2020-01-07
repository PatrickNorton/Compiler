package main.java.converter;

import main.java.parser.TypeLikeNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CompilerInfo {
    private FileInfo parent;
    private int loopLevel;
    private Map<Integer, Set<Integer>> danglingPointers;
    private List<Integer> loopStarts;

    private List<Map<String, VariableInfo>> variables;
    private Map<String, TypeObject> typeMap;

    public CompilerInfo(FileInfo parent) {
        this.parent = parent;
        this.loopLevel = 0;
        this.danglingPointers = new HashMap<>();
        this.loopStarts = new ArrayList<>();
        this.variables = new ArrayList<>();
        this.typeMap = new HashMap<>();
    }

    /**
     * Enter another loop, implying another level of break/continue statements.
     *
     * @param listStart The index of the beginning of the list relative to the
     *                  start of the function
     * @param bytes The list of bytes
     */
    public void enterLoop(int listStart, @NotNull List<Byte> bytes) {
        loopLevel++;
        assert loopStarts.size() == loopLevel - 1;
        loopStarts.add(listStart + bytes.size());
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
        loopLevel--;
        int endLoop = listStart + bytes.size();
        loopStarts.remove(loopStarts.size() - 1);
        for (int i : danglingPointers.getOrDefault(loopLevel, Collections.emptySet())) {
            setPointer(listStart - i, bytes, endLoop);
        }
        danglingPointers.remove(loopLevel);
        removeStackFrame();
    }

    /**
     * Add a break statement to the pool of un-linked statements.
     *
     * @param levels The number of levels to break
     * @param location The location (absolute, by start of function)
     */
    public void addBreak(int levels, int location) {
        danglingPointers.computeIfAbsent(loopLevel - levels, k -> new HashSet<>());
        danglingPointers.get(loopLevel - levels).add(location);
    }

    /**
     * Add a continue statement's pointer to the list.
     *
     * @param levels The number of levels to continue
     * @param bytes The list of bytes to adjust
     * @param location The location (relative to the start of the list)
     */
    public void addContinue(int levels, List<Byte> bytes, int location) {
        setPointer(location, bytes, loopStarts.get(loopLevel - levels));
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
        var value = typeMap.get(type.strName());
        if (value == null) {
            throw new RuntimeException("Unknown type " + type);
        } else {
            return value;
        }
    }

    /**
     * Get the type of a variable.
     *
     * @param variable The variable name to get the type from
     * @return The type of the variable
     */
    public TypeObject getType(String variable) {
        return varInfo(variable).getType();
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
            parent.deScopeVariable(pair.getLocation());
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
    public void addExport(String name, TypeObject type) {
        parent.addExport(name, type);
    }

    /**
     * Add a variable with a constant value to the stack.
     *
     * @param name The name of the variable
     * @param type The type of the variable
     * @param constValue The constant value the variable has
     */
    public void addVariable(String name, TypeObject type, LangConstant constValue) {
        addVariable(name, new VariableInfo(type, constValue, -1));
    }

    /**
     * Add a variable to the stack.
     *
     * @param name The name of the variable
     * @param type The type of the variable
     */
    public void addVariable(String name, TypeObject type) {
        addVariable(name, new VariableInfo(type, parent.newVariableIndex()));
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
        return varInfo(name).isConst();
    }

    private VariableInfo varInfo(String name) {
        for (int i = variables.size() - 1; i >= 0; i--) {
            var map = variables.get(i);
            if (map.containsKey(name)) {
                return map.get(name);
            }
        }
        throw new RuntimeException("Unknown variable");
    }

    public TypeObject importType(String name) {
        return parent.importType(name);
    }

    public TypeObject exportType(String name) {
        return parent.exportType(name);
    }

    /**
     * The index of the variable in the variable stack.
     *
     * @param name The name of the variable
     * @return The index in the stack
     */
    public int varIndex(String name) {
        return varInfo(name).getLocation();
    }

    @NotNull
    private List<TypeObject> getTypes(@NotNull TypeLikeNode... types) {
        List<TypeObject> typeObjects = new ArrayList<>(types.length);
        for (var type : types) {
            typeObjects.add(getType(type));
        }
        return typeObjects;
    }

    @NotNull
    @Contract(pure = true)
    private TypeObject[] typesOf(@NotNull TypeLikeNode... types) {
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
        return constIndex(variableInfo.constValue());
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
