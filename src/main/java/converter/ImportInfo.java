package main.java.converter;

import main.java.parser.LineInfo;
import main.java.parser.Lined;

import java.util.List;
import java.util.Optional;

public final class ImportInfo implements Lined {
    private final LineInfo lineInfo;
    private final int index;
    private final List<String> names;
    private final List<String> asNames;

    public ImportInfo(LineInfo lineInfo, int index, List<String> names) {
        this(lineInfo, index, names, null);
    }

    public ImportInfo(LineInfo lineInfo, int index, List<String> names, List<String> asNames) {
        this.index = index;
        this.names = names;
        this.asNames = asNames;
        this.lineInfo = lineInfo;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
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
