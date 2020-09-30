package main.java.converter;

import main.java.util.IndexedHashSet;
import main.java.util.IndexedSet;
import main.java.util.IntAllocator;

import java.util.ArrayList;
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

    {  // Prevent "non-updating" compiler warning
        anonymousNums.remove(0);
        staticVarNumbers.remove(0);
    }
}
