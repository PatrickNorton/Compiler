package main.java.converter;

import java.util.List;
import java.util.Optional;

public final class ImportInfo {
    private final int index;
    private final List<String> names;
    private final List<String> asNames;

    public ImportInfo(int index, List<String> names) {
        this(index, names, null);
    }

    public ImportInfo(int index, List<String> names, List<String> asNames) {
        this.index = index;
        this.names = names;
        this.asNames = asNames;
    }

    public int getIndex() {
        return index;
    }

    public List<String> getNames() {
        return names;
    }

    public Optional<List<String>> getAsNames() {
        return Optional.ofNullable(asNames);
    }
}
