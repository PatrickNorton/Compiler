package main.java.converter;

import main.java.parser.LineInfo;
import main.java.parser.Lined;

import java.util.ArrayList;
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

    /**
     * Gets the result of this with added imports (and their corresponding
     * '{@code as}' names)
     * <p>
     *     The second parameter, {@code asNames}, is nullable. Passing {@code
     *     null} to this variable is the same as doing so to the constructor,
     *     e.g. it will assume an {@code as} clause was not included, and thus
     *     all variables were imported as their exported names.
     * </p>
     * <p>
     *     This is a pure function, and does not modify the object on which it
     *     was called.
     * </p>
     *
     * @param names The additional names exported
     * @param asNames Optionally, the names specified by an {@code as} clause
     * @return The new {@link ImportInfo}
     */
    public ImportInfo merge(List<String> names, List<String> asNames) {
        var newNames = new ArrayList<>(this.names);
        newNames.addAll(names);
        if (this.asNames == null && asNames == null) {
            return new ImportInfo(lineInfo, index, newNames);
        } else {
            // If one thing has an 'as' name, everything has to...
            var newAsNames = new ArrayList<>(this.asNames == null ? this.names : this.asNames);
            newAsNames.addAll(asNames == null ? names : asNames);
            return new ImportInfo(lineInfo, index, newNames, newAsNames);
        }
    }
}
