package NameResolver;

import Parser.BodyNode;
import Parser.ComplexStatementNode;
import Parser.ElifStatementNode;
import Parser.ExportStatementNode;
import Parser.IfStatementNode;
import Parser.ImportStatementNode;
import Parser.IndependentNode;
import Parser.Parser;
import Parser.TopNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class Package {
    private TopNode contents;
    private ImportStatementNode[] imports;
    private ExportStatementNode[] exports;
    private File location;
    private String name;

    private static final Map<File, Package> cache = new HashMap<>();

    @Contract(pure = true)
    private Package(File location) {
        this(location, "__main__");
    }

    @Contract(pure = true)
    private Package(File location, String name) {
        this.location = location;
        this.contents = Parser.parse(location);
        setImports();
        setExports();
        this.name = name;
    }

    private void setImports() {
        final LinkedList<ImportStatementNode> median = new LinkedList<>();
        for (IndependentNode i : contents) {
            if (i instanceof ImportStatementNode) {
                median.add((ImportStatementNode) i);
            }
        }
        this.imports = median.toArray(new ImportStatementNode[0]);
    }

    @NotNull
    private static ArrayList<ImportStatementNode> getImports(@NotNull ComplexStatementNode stmt) {
        return getImports(stmt.getBody());
    }

    @NotNull
    private static ArrayList<ImportStatementNode> getImports(@NotNull BodyNode body) {
        ArrayList<ImportStatementNode> imports = new ArrayList<>();
        for (IndependentNode i : body) {
            if (i instanceof ImportStatementNode) {
                imports.add((ImportStatementNode) i);
            } else if (i instanceof IfStatementNode) {
                imports.addAll(getImports((IfStatementNode) i));
            } else if (i instanceof ComplexStatementNode) {
                imports.addAll(getImports((ComplexStatementNode) i));
            }
        }
        return imports;
    }

    @NotNull
    private static ArrayList<ImportStatementNode> getImports(@NotNull IfStatementNode stmt) {
        ArrayList<ImportStatementNode> imports = new ArrayList<>(getImports(stmt.getBody()));
        for (ElifStatementNode e : stmt.getElifs()) {
            imports.addAll(getImports(e.getBody()));
        }
        imports.addAll(getImports(stmt.getElse_stmt()));
        return imports;
    }

    private void setExports() {
        final LinkedList<ExportStatementNode> median = new LinkedList<>();
        for (IndependentNode i : contents) {
            if (i instanceof ExportStatementNode) {
                median.add((ExportStatementNode) i);
            }
        }
        this.exports = median.toArray(new ExportStatementNode[0]);
    }

    public static Package find(File f) {
        if (cache.containsKey(f)) {
            return cache.get(f);
        } else {
            Package p = new Package(f);  // TODO: Names for packages
            cache.put(f, p);
            return p;
        }
    }
}
