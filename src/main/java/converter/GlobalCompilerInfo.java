package main.java.converter;

import main.java.util.IndexedHashSet;
import main.java.util.IndexedSet;
import main.java.util.IntAllocator;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private final IndexedSet<LangConstant> constants = new IndexedHashSet<>();
    private final List<SwitchTable> tables = new ArrayList<>();

    private final IntAllocator staticVarNumbers = new IntAllocator();
    private final IntAllocator anonymousNums = new IntAllocator();

    private final List<List<Byte>> defaultFunctions = new ArrayList<>();
    private final List<Function> functions = new ArrayList<>(Collections.singletonList(null));  // Reserve for default

    public void addConstant(LangConstant value) {
        constants.add(value);
    }

    public int indexOf(LangConstant value) {
        return constants.indexOf(value);
    }

    public boolean containsConst(LangConstant value) {
        return constants.contains(value);
    }

    public LangConstant getConstant(int index) {
        return constants.get(index);
    }

    public void setConstant(int index, LangConstant value) {
        constants.set(index, value);
    }

    public IndexedSet<LangConstant> getConstants() {
        return constants;
    }

    public int addTable(SwitchTable value) {
        tables.add(value);
        return tables.size() - 1;
    }

    public List<SwitchTable> getTables() {
        return tables;
    }

    public short claimStaticVar() {
        return (short) staticVarNumbers.getNext();
    }

    public int getAnonymous() {
        return anonymousNums.getNext();
    }

    public int addFunction(@NotNull Function info) {
        functions.add(info);
        return functions.size() - 1;
    }

    public Function getFunction(int index) {
        return functions.get(index);
    }

    // Assumes this isn't used until file-writing
    public List<Function> getFunctions() {
        if (functions.get(0) == null) {
            var bytes = defaultFunctions.size() == 1 ? defaultFunctions.get(0) : createDefaultFn();
            functions.set(0, new Function(new FunctionInfo("__default__", new ArgumentInfo()), bytes));
        }
        return functions;
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

    public int reserveStatic() {
        defaultFunctions.add(null);
        return defaultFunctions.size() - 1;
    }

    public void setStatic(int index, List<Byte> bytes) {
        assert defaultFunctions.get(index) == null;
        defaultFunctions.set(index, bytes);
    }

    {  // Prevent "non-updating" compiler warning
        anonymousNums.remove(0);
        staticVarNumbers.remove(0);
    }
}
