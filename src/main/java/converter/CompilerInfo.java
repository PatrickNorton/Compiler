package main.java.converter;

import main.java.parser.TypeLikeNode;
import main.java.parser.TypeNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CompilerInfo {
    private int loopLevel;
    private Map<Integer, Set<Integer>> danglingPointers;
    private List<Integer> loopStarts;
    private Set<LangConstant> constantPool;

    private List<Map<String, TypeObject>> variables;
    private Map<String, TypeObject> typeMap;

    public CompilerInfo() {
        this.loopLevel = 0;
        this.danglingPointers = new HashMap<>();
        this.loopStarts = new ArrayList<>();
        this.constantPool = new LinkedHashSet<>();  // Relies on constant iteration order
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
        constantPool.add(value);
        return constIndex(value);
    }

    public Set<LangConstant> constants() {
        return constantPool;
    }

    public void addType(@NotNull TypeLikeNode type, List<TypeObject> supers) {
        if (!typeMap.containsKey(type.strName())) {
            var typeObject = newType(type, supers);
            typeMap.put(typeObject.name(), typeObject);
        }
    }

    @Nullable
    @Contract(pure = true)
    public TypeObject getType(@NotNull TypeLikeNode type) {
        return typeMap.get(type.strName());
    }

    @NotNull
    @Contract("null, _ -> fail")
    public TypeObject newType(TypeLikeNode type, List<TypeObject> supers) {
        if (type instanceof TypeNode) {
            var typeNode = (TypeNode) type;
            return new StdTypeObject(typeNode.strName(),
                    supers,
                    getTypes(typeNode.getSubtypes())
            );
        } else {
            throw new UnsupportedOperationException("Unknown type of parameter 'type': " + type.getClass());
        }
    }

    public void addStackFrame() {
        variables.add(new HashMap<>());
    }

    public void removeStackFrame() {
        variables.remove(variables.size() - 1);
    }

    @NotNull
    private List<TypeObject> getTypes(@NotNull TypeLikeNode... types) {
        List<TypeObject> typeObjects = new ArrayList<>(types.length);
        for (var type : types) {
            typeObjects.add(getType(type));
        }
        return typeObjects;
    }

    private int constIndex(LangConstant value) {
        int index = 0;
        for (var v : constantPool) {
            if (v.equals(value)) return index;
            index++;
        }
        return -1;
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
