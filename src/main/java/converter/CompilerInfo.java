package main.java.converter;

import main.java.parser.TypeLikeNode;
import main.java.parser.TypeNode;
import main.java.parser.TypeUnionNode;
import main.java.parser.TypewiseAndNode;
import main.java.util.IndexedHashSet;
import main.java.util.IndexedSet;
import main.java.util.Pair;
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
    private IndexedSet<LangConstant> constantPool;

    private List<Map<String, Pair<TypeObject, Integer>>> variables;
    private int varCount;
    private Map<String, TypeObject> typeMap;

    public CompilerInfo(FileInfo parent) {
        this.parent = parent;
        this.loopLevel = 0;
        this.danglingPointers = new HashMap<>();
        this.loopStarts = new ArrayList<>();
        this.constantPool = new IndexedHashSet<>();
        this.variables = new ArrayList<>();
        this.varCount = 0;
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

    public TypeObject getType(String variable) {
        return varInfo(variable).getKey();
    }

    @NotNull
    @Contract("null, _ -> fail")
    public TypeObject newType(TypeLikeNode type, List<TypeObject> supers) {
        TypeObject endType;
        if (type instanceof TypeNode) {
            endType = new StdTypeObject(type.strName(), supers, getTypes(type.getSubtypes()));
        } else if (type instanceof TypeUnionNode) {
            endType = new UnionTypeObject(typesOf(type.getSubtypes()));
        } else if (type instanceof TypewiseAndNode) {
            endType = new IntersectionTypeObject(typesOf(type.getSubtypes()));
        } else {
            throw new UnsupportedOperationException("Unknown type of parameter 'type': " + type.getClass());
        }
        return type.isOptional() ? new OptionalTypeObject(endType) : endType;
    }

    public void addStackFrame() {
        variables.add(new HashMap<>());
    }

    public void removeStackFrame() {
        variables.remove(variables.size() - 1);
    }

    public int addImport(String name) {
        return parent.addImport(name);
    }

    public void addExport(String name, TypeObject type) {
        parent.addExport(name, type);
    }

    public void addVariable(String name, TypeObject type) {
        variables.get(variables.size() - 1).put(name, Pair.of(type, varCount++));
    }

    private Pair<TypeObject, Integer> varInfo(String name) {
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

    private int constIndex(LangConstant value) {
        return constantPool.indexOf(value);
    }

    @NotNull
    public List<Byte> bringToTop(@NotNull String... varNames) {
        // FIXME: Does not deal with duplicate variables well
        List<Byte> bytes = new ArrayList<>();
        for (int i = varNames.length - 1; i >= 0; i--) {
            bytes.add(Bytecode.LOAD_VALUE.value);
            bytes.addAll(Util.shortToBytes(varInfo(varNames[i]).getValue().shortValue()));
        }
        return bytes;
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
