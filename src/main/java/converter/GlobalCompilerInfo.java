package main.java.converter;

import main.java.util.IndexedHashSet;
import main.java.util.IndexedSet;
import main.java.util.IntAllocator;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The class for compiler information that is shared between all files.
 * <p>
 *     This should only be constructed once, as a static member of {@link
 *     CompilerInfo}. Creating another one will cause bad things to happen.
 * </p>
 *
 * @apiNote Yes, I know global state is bad, but I'm not sure of a better way
 *          to do this yet, so here it stays.
 * @author Patrick Norton
 * @see CompilerInfo
 */
public final class GlobalCompilerInfo {
    private final File destFile;

    private final IndexedSet<LangConstant> constants = new IndexedHashSet<>();
    private final List<SwitchTable> tables = new ArrayList<>();

    private final IntAllocator staticVarNumbers = new IntAllocator();
    private final IntAllocator anonymousNums = new IntAllocator();

    private final List<List<Byte>> defaultFunctions = new ArrayList<>();
    private final List<Function> functions = new ArrayList<>(Collections.singletonList(null));  // Reserve for default
    private final List<ClassInfo> classes = new ArrayList<>();
    private final Map<BaseType, Integer> classMap = new HashMap<>();
    private final ErrorCounter warnings = new ErrorCounter();

    public GlobalCompilerInfo(File destFile) {
        this.destFile = destFile;
    }

    /**
     * Gets the file that will be written to at the end of compilation.
     *
     * @return The destination file
     */
    public File destFile() {
        return this.destFile;
    }

    /**
     * Adds a constant to the global constant pool.
     *
     * @param value The constant to add
     */
    public void addConstant(LangConstant value) {
        constants.add(value);
    }

    /**
     * Gets the index in the constant pool of the constant given, or {@code -1}
     * if not present.
     *
     * @param value The constant to check
     * @return The index in the constant pool.
     */
    public int indexOf(LangConstant value) {
        return constants.indexOf(value);
    }

    /**
     * If the global constant pool contains the given constant.
     *
     * @param value The constant to check
     * @return If the global pool contains the given constant
     */
    public boolean containsConst(LangConstant value) {
        return constants.contains(value);
    }

    /**
     * Gets the constant at the given index.
     *
     * @param index The index to get the constant at
     * @return The constant at the given index
     */
    public LangConstant getConstant(int index) {
        return constants.get(index);
    }

    /**
     * Sets the constant at the given index to a certain value.
     *
     * @param index The index to set
     * @param value The value to set
     */
    public void setConstant(int index, LangConstant value) {
        constants.set(index, value);
    }

    /**
     * Gets the global pool of constants.
     *
     * @return The constant pool
     */
    public IndexedSet<LangConstant> getConstants() {
        return constants;
    }

    /**
     * Gets the global warning and error counter.
     *
     * @return The error counter
     */
    public ErrorCounter getWarnings() {
        return warnings;
    }

    /**
     * Adds a switch table to the global list of tables.
     *
     * @param value The table to add
     * @return The index of the given table
     */
    public int addTable(SwitchTable value) {
        tables.add(value);
        return tables.size() - 1;
    }

    /**
     * Gets the global list of switch tables.
     *
     * @return All the switch tables
     */
    public List<SwitchTable> getTables() {
        return tables;
    }

    /**
     * Claims the given static index for further use.
     * <p>
     *     This is for use with static local variables. Once claimed, this
     *     method will never return that index again.
     * </p>
     *
     * @return The static index
     */
    public short claimStaticVar() {
        return (short) staticVarNumbers.getNext();
    }

    /**
     * Gets the next anonymous variable number.
     * <p>
     *     This is for claiming anonymous variables; the returned number will
     *     not be returned in subsequent calls.
     * </p>
     *
     * @return The next anonymous number
     */
    public int getAnonymous() {
        return anonymousNums.getNext();
    }

    /**
     * Adds a function to the global function pool.
     *
     * @param info The function to add
     * @return The index of the function
     */
    public int addFunction(@NotNull Function info) {
        functions.add(info);
        return functions.size() - 1;
    }

    /**
     * Gets the function at the given index.
     *
     * @param index The index of the function
     * @return The function at that index
     */
    public Function getFunction(int index) {
        return functions.get(index);
    }

    /**
     * Gets the global list of functions.
     * <p>
     *     This assumes this is not used until file-writing time. As such, it
     *     also creates the {@code __default__} function and places it into the
     *     list at position 0.
     * </p>
     *
     * @return The list of global functions
     */
    // Assumes this isn't used until file-writing
    public List<Function> getFunctions() {
        if (functions.get(0) == null) {
            var defaultNo = singleDefaultPos();
            var bytes = defaultNo != -1 ? defaultFunctions.get(defaultNo) : createDefaultFn();
            functions.set(0, new Function(new FunctionInfo("__default__", new ArgumentInfo()), bytes));
        }
        return functions;
    }

    private int singleDefaultPos() {
        assert !defaultFunctions.isEmpty();
        if (defaultFunctions.size() == 1) {
            return 0;
        }
        int foundOne = -1;
        for (int i = 0; i < defaultFunctions.size(); i++) {
            var fn = defaultFunctions.get(i);
            if (!fn.isEmpty()) {
                if (foundOne != -1) {
                    return -1;
                } else {
                    foundOne = i;
                }
            }
        }
        return foundOne == -1 ? 0 : foundOne;  // If no non-zero functions, we can just use any empty one
    }

    @NotNull
    private List<Byte> createDefaultFn() {
        List<Byte> result = new ArrayList<>();
        for (int i = defaultFunctions.size() - 1; i >= 0; i--) {
            var func = defaultFunctions.get(i);
            if (!func.isEmpty()) {
                var fnName = String.format("__default__$%d", i);
                var fn = new Function(new FunctionInfo(fnName, new ArgumentInfo()), func);
                functions.add(fn);
                result.add(Bytecode.CALL_FN.value);
                result.addAll(Util.shortToBytes((short) (functions.size() - 1)));
                result.addAll(Util.shortZeroBytes());
            }
        }
        return result;
    }

    /**
     * Reserves a static function slot.
     * <p>
     *     This slot is for the __default__ functions that are auto-generated
     *     as a part of each file. When {@link #getFunctions()} is called, they
     *     are added to the global function pool and the main {@code
     *     __default__} function is created in slot 0. Note that this is not to
     *     be confused with either static variables (see {@link
     *     #claimStaticVar()}), or static methods (see {@link
     *     main.java.converter.classbody.MethodConverter MethodConverter}).
     * </p>
     *
     * @return The index of the function in the static function pool
     * @see #setStatic
     */
    public int reserveStatic() {
        defaultFunctions.add(null);
        return defaultFunctions.size() - 1;
    }

    /**
     * Sets the given static function index to the given bytecode.
     * <p>
     *     The given index should only be set once (in debug mode, this will
     *     currently panic if it is overwriting another function), and the
     *     value given in {@code bytes} should be valid executable bytecode.
     * </p>
     *
     * @param index The index to set
     * @param bytes The bytecode to insert in the location
     * @see #reserveStatic
     */
    public void setStatic(int index, List<Byte> bytes) {
        assert defaultFunctions.get(index) == null;
        defaultFunctions.set(index, bytes);
    }

    /**
     * Adds a class to the global class pool.
     * <p>
     *     This does not make the class visible from any files, but provides a
     *     base to link against when methods are called. The same class should
     *     not be added multiple times, or linking errors may happen.
     * </p>
     *
     * @param info The class information to add to the pool
     * @return The class's index in the global pool
     */
    public int addClass(ClassInfo info) {
        classes.add(info);
        assert !classMap.containsKey(new BaseType(info.getType()));
        classMap.put(new BaseType(info.getType()), classes.size() - 1);
        return classes.size() - 1;
    }

    /**
     * Reserves space for a class to be added later.
     * <p>
     *     The class reserved should be set using {@link #setClass} before
     *     {@link #getClasses()} is called.
     * </p>
     *
     * @param type The type to reserve
     * @return The index in the class pool
     * @see #setClass
     */
    public int reserveClass(UserType<?> type) {
        classes.add(null);
        classMap.put(new BaseType(type), classes.size() - 1);
        return classes.size() - 1;
    }

    /**
     * Sets the reserved class to the given info.
     * <p>
     *     The class ought to have been reserved using {@link #reserveClass},
     *     using the type given by {@link ClassInfo#getType() info.getType()}.
     * </p>
     *
     * @param info The class info to set from the reserve.
     * @return The index in the class pool
     * @see #reserveClass
     */
    public int setClass(@NotNull ClassInfo info) {
        int index = classMap.get(new BaseType(info.getType()));
        assert classes.get(index) == null;
        classes.set(index, info);
        return index;
    }

    /**
     * Gets the index in the class pool of the given type.
     *
     * @param type The type to get the index of
     * @return The index in the class pool
     */
    public int classIndex(UserType<?> type) {
        return classMap.get(new BaseType(type));
    }

    /**
     * Gets the global list of defined classes.
     * <p>
     *     By the time this is called, all of the classes reserved (using
     *     {@link #reserveClass}) should be set (using {@link #setClass}. When
     *     assertions are turned on, this will throw an exception if that
     *     invariant is not upheld.
     * </p>
     *
     * @return The list of classes defined in the program
     */
    public List<ClassInfo> getClasses() {
        assert !classes.contains(null) : String.format("Class no. %d is null", classes.indexOf(null));
        return classes;
    }

    {  // Prevent "non-updating" compiler warning
        anonymousNums.remove(0);
        staticVarNumbers.remove(0);
    }
}
