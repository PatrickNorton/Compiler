package main.java.converter;

import main.java.parser.ClassDefinitionNode;
import main.java.parser.DefinitionNode;
import main.java.parser.DescriptorNode;
import main.java.parser.EnumDefinitionNode;
import main.java.parser.ImportExportNode;
import main.java.parser.InterfaceDefinitionNode;
import main.java.parser.LineInfo;
import main.java.parser.Parser;
import main.java.parser.TopNode;
import main.java.parser.TypedefStatementNode;
import main.java.parser.UnionDefinitionNode;
import main.java.parser.VariableNode;
import main.java.util.IndexedHashSet;
import main.java.util.IndexedSet;
import main.java.util.Pair;
import main.java.util.Zipper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The class for handling imports, exports, and the list of files involved in a
 * program.
 *
 * @author Patrick Norton
 * @see ImportExportConverter
 * @see CompilerInfo
 */
public final class ImportHandler {
    private static final Map<Path, CompilerInfo> ALL_FILES = new HashMap<>();
    private static List<Pair<CompilerInfo, File>> toCompile = new ArrayList<>();

    public static final Map<InterfaceType, Optional<Pair<CompilerInfo, InterfaceDefinitionNode>>>
            ALL_DEFAULT_INTERFACES = new HashMap<>();

    static {
        for (var val : Builtins.DEFAULT_INTERFACES) {
            ALL_DEFAULT_INTERFACES.put(val, Optional.empty());
        }
    }

    private final CompilerInfo info;
    private final Map<String, TypeObject> exports = new HashMap<>();
    private final Map<String, Path> fromExports = new HashMap<>();
    private final Map<Path, ImportInfo> imports = new HashMap<>();
    private final IndexedSet<String> importStrings = new IndexedHashSet<>();
    private final Set<Path> wildcardExports = new HashSet<>();

    public ImportHandler(CompilerInfo info) {
        this.info = info;
    }

    /**
     * Register all the files upon which this depends and load all type
     * definitions.
     * <p>
     *     This is essential in the pre-linking process. Its job is many-fold:
     *     Firstly, it adds all the files on which this depends on to {@link
     *     ImportHandler#ALL_FILES} and calls this method on all of them, if
     *     not already done. Secondly, it loads all type-like definitions
     *     (types, interfaces, unions, etc.) without any constituent methods
     *     attached. Thirdly, it adds those definitions which represent default
     *     interfaces to {@link ImportHandler#ALL_DEFAULT_INTERFACES}.
     * </p>
     *
     * @param node The node representing the file
     */
    public void registerDependents(@NotNull TopNode node) {
        Map<String, TypeObject> types = new HashMap<>();
        Map<String, LineInfo> lineInfos = new HashMap<>();
        boolean isModule = false;
        Optional<InterfaceDefinitionNode> hasAuto = Optional.empty();
        Deque<TypedefStatementNode> typedefs = new ArrayDeque<>();
        for (var stmt : node) {
            if (stmt instanceof ImportExportNode) {
                var ieNode = (ImportExportNode) stmt;
                switch (ieNode.getType()) {
                    case TYPEGET:
                    case IMPORT:
                        registerImports(ieNode);
                        break;
                    case EXPORT:
                        isModule = true;
                        registerExports(ieNode);
                        break;
                }
            } else if (stmt instanceof DefinitionNode) {
                if (stmt instanceof ClassDefinitionNode) {
                    var cls = (ClassDefinitionNode) stmt;
                    var strName = cls.strName();
                    if (types.containsKey(strName)) {
                        throw CompilerException.doubleDef(strName, stmt.getLineInfo(), lineInfos.get(strName));
                    }
                    var generics = GenericInfo.parseNoTypes(info, cls.getName().getSubtypes());
                    types.put(strName, new StdTypeObject(strName, generics));
                    lineInfos.put(strName, cls.getLineInfo());
                } else if (stmt instanceof EnumDefinitionNode) {
                    var cls = (EnumDefinitionNode) stmt;
                    var strName = cls.getName().strName();
                    if (types.containsKey(strName)) {
                        throw CompilerException.doubleDef(strName, stmt.getLineInfo(), lineInfos.get(strName));
                    }
                    var generics = GenericInfo.parseNoTypes(info, cls.getName().getSubtypes());
                    types.put(strName, new StdTypeObject(strName, generics));
                    lineInfos.put(strName, cls.getLineInfo());
                } else if (stmt instanceof InterfaceDefinitionNode) {
                    var cls = (InterfaceDefinitionNode) stmt;
                    var strName = cls.getName().strName();
                    if (types.containsKey(strName)) {
                        throw CompilerException.doubleDef(strName, stmt.getLineInfo(), lineInfos.get(strName));
                    }
                    var generics = GenericInfo.parseNoTypes(info, cls.getName().getSubtypes());
                    var type = new InterfaceType(strName, generics);
                    types.put(strName, type);
                    lineInfos.put(strName, cls.getLineInfo());
                    if (cls.getDescriptors().contains(DescriptorNode.AUTO)) {
                        ALL_DEFAULT_INTERFACES.put(type, Optional.of(Pair.of(info, cls)));
                        hasAuto = Optional.of(cls);
                    }
                } else if (stmt instanceof UnionDefinitionNode) {
                    var cls = (UnionDefinitionNode) stmt;
                    var strName = cls.getName().strName();
                    if (types.containsKey(strName)) {
                        throw CompilerException.doubleDef(strName, stmt.getLineInfo(), lineInfos.get(strName));
                    }
                    var generics = GenericInfo.parseNoTypes(info, cls.getName().getSubtypes());
                    types.put(strName, new StdTypeObject(strName, generics));
                    lineInfos.put(strName, cls.getLineInfo());
                } else if (stmt instanceof TypedefStatementNode) {
                    typedefs.push((TypedefStatementNode) stmt);
                }
            }
        }
        if (!isModule && hasAuto.isPresent()) {
            throw CompilerException.of("Cannot (yet?) have 'auto' interfaces in non-module file", hasAuto.get());
        }
        for (var stmt : typedefs) {
            var type = stmt.getType();
            var name = stmt.getName();
            types.put(name.strName(), info.getType(type).typedefAs(name.strName()));
        }
        for (var export : exports.entrySet()) {
            if (types.containsKey(export.getKey())) {
                export.setValue(Builtins.TYPE.generify(types.get(export.getKey())));
            }
        }
        if (isModule) {
            info.addPredeclaredTypes(types);
        }
    }

    public void setFromLinker(@NotNull Linker linker) {
        var exports = linker.getExports();
        var globals = linker.getGlobals();
        for (var entry : exports.entrySet()) {
            var exportName = entry.getValue().getKey();
            var exportType = globals.get(entry.getKey());
            if (exportType == null) {
                var lineInfo = entry.getValue().getValue();
                throw CompilerException.of("Undefined name for export: " + exportName, lineInfo);
            }
            assert exports.containsKey(exportName);
            this.exports.put(exportName, exportType);
        }
    }

    /**
     * Adds a non-top level import to the pool.
     *
     * @param node The node to parse into an import
     * @return A map of strings to the import digits from this node
     */
    @NotNull
    public Map<String, Integer> addImport(@NotNull ImportExportNode node) {
        assert node.getType() == ImportExportNode.IMPORT || node.getType() == ImportExportNode.TYPEGET;
        if (node.isWildcard()) {
            var path = loadFile(moduleName(node, 0), node);
            var file = ALL_FILES.get(path);
            var importValues = new ArrayList<>(file.importHandler().exports.keySet());
            imports.put(path, new ImportInfo(imports.size(), importValues));
            Map<String, Integer> result = new HashMap<>();
            for (var val : importValues) {
                var fromStr = node.getFrom().toString() + "." + val;
                importStrings.add(fromStr);
                result.put(val, importStrings.indexOf(fromStr));
            }
            return result;
        } else if (node.getFrom().isEmpty()) {
            checkAs(node);
            Map<String, Integer> result = new HashMap<>();
            for (int i = 0; i < node.getValues().length; i++) {
                var val = node.getValues()[i];
                var preDot = ((VariableNode) val.getPreDot()).getName();
                assert val.getPostDots().length == 0;
                var path = loadFile(preDot, node);
                var valStr = val.toString();
                var as = node.getAs().length == 0 ? valStr : node.getAs()[i].toString();
                imports.put(path, new ImportInfo(imports.size(), List.of()));
                importStrings.add(valStr);
                result.put(as, importStrings.indexOf(valStr));
            }
            return result;
        } else {
            checkAs(node);
            var from = node.getFrom();
            List<String> values = new ArrayList<>();
            Map<String, Integer> result = new HashMap<>();
            for (int i = 0; i < node.getValues().length; i++) {
                var value = node.getValues()[i];
                values.add(value.toString());
                var valStr = from.toString() + "." + value.toString();
                var as = node.getAs().length == 0 ? value.toString() : node.getAs()[i].toString();
                importStrings.add(valStr);
                result.put(as, importStrings.indexOf(valStr));
            }
            var path = loadFile(from.toString(), node);
            imports.put(path, new ImportInfo(imports.size(), values));
            return result;
        }
    }

    private void registerImports(@NotNull ImportExportNode node) {
        assert node.getType() == ImportExportNode.IMPORT || node.getType() == ImportExportNode.TYPEGET;
        if (node.isWildcard()) {
            var path = loadFile(moduleName(node, 0), node);
            imports.put(path, new ImportInfo(imports.size(), List.of("*")));
        } else if (node.getFrom().isEmpty()) {
            checkAs(node);
            for (int i = 0; i < node.getValues().length; i++) {
                var val = node.getValues()[i];
                var preDot = ((VariableNode) val.getPreDot()).getName();
                assert val.getPostDots().length == 0;
                var path = loadFile(preDot, node);
                var valStr = val.toString();
                imports.put(path, new ImportInfo(imports.size(), List.of()));
                importStrings.add(valStr);
            }
        } else {
            checkAs(node);
            var from = node.getFrom();
            List<String> values = new ArrayList<>();
            for (int i = 0; i < node.getValues().length; i++) {
                var value = node.getValues()[i];
                values.add(value.toString());
                var valStr = from.toString() + "." + value.toString();
                importStrings.add(valStr);
            }
            List<String> asNames;
            if (node.getAs().length != 0) {
                asNames = new ArrayList<>(node.getAs().length);
                for (var name : node.getAs()) {
                    asNames.add(((VariableNode) name.getPreDot()).getName());
                }
            } else {
                asNames = null;
            }
            var path = loadFile(from.toString(), node);
            imports.put(path, new ImportInfo(imports.size(), values, asNames));
        }
    }

    private void checkAs(@NotNull ImportExportNode node) {
        if (node.getAs().length != 0 && node.getAs().length != node.getValues().length) {
            throw CompilerException.format(
                    "Import statement had %d 'as' clauses, expected %d (== to # of imported names)",
                    node, node.getAs().length, node.getValues().length
            );
        }
    }

    @Contract(pure = true)
    @NotNull
    public Collection<String> getExports() {
        return exports.keySet();
    }

    private void registerWildcardImport(String moduleName, @NotNull ImportExportNode node) {
        var path = loadFile(moduleName, node);
        imports.put(path, new ImportInfo(imports.size(), List.of("*")));
        // FIXME: Add to importStrings
    }

    private Path loadFile(String moduleName, @NotNull ImportExportNode node) {
        Path path;
        if (node.getPreDots() > 0) {
            var parentPath = info.path();
            for (int i = 0; i < node.getPreDots(); i++) {
                parentPath = parentPath.getParent();
            }
            path = Converter.localModulePath(parentPath, moduleName, node);
        } else {
            path = Converter.findPath(moduleName, node);
        }
        CompilerInfo f;
        if (ALL_FILES.containsKey(path)) {
            f = ALL_FILES.get(path);
        } else {
            f = new CompilerInfo(Parser.parse(path.toFile()));
            ALL_FILES.put(path, f);
            toCompile.add(Pair.of(f, Converter.resolveFile(moduleName)));
        }
        f.loadDependents();
        return path;
    }

    private void registerExports(@NotNull ImportExportNode node) {
        assert node.getType() == ImportExportNode.EXPORT;
        boolean notRenamed = node.getAs().length == 0;
        boolean isFrom = !node.getFrom().isEmpty();
        if (node.isWildcard()) {
            if (node.getFrom().isEmpty()) {
                throw CompilerException.of("Cannot 'export *' without a 'from' clause", node);
            }
            var moduleName = moduleName(node, 0);
            registerWildcardExport(moduleName, node);
            return;
        }
        for (int i = 0; i < node.getValues().length; i++) {
            var value = node.getValues()[i];
            if (isFrom) {
                registerWildcardImport(moduleName(node, i), node);
            }
            var as = notRenamed ? value : node.getAs()[i];
            if (!(value.getPreDot() instanceof VariableNode) || value.getPostDots().length > 0) {
                throw CompilerException.of("Illegal export " + value, value);
            }
            var name = ((VariableNode) value.getPreDot()).getName();
            var asName = as.isEmpty() ? name : ((VariableNode) as.getPreDot()).getName();
            if (exports.containsKey(asName)) {
                throw CompilerException.format("Name %s already exported", node, asName);
            } else {
                exports.put(name, null);
                if (isFrom) {
                    var path = loadFile(moduleName(node, i), node);
                    fromExports.put(name, path);
                }
            }
        }
    }

    private void registerWildcardExport(String moduleName, @NotNull ImportExportNode node) {
        var path = node.getPreDots() > 0
                ? Converter.localModulePath(info.path().getParent(), moduleName, node)
                : Converter.findPath(moduleName, node);
        CompilerInfo f;
        if (ALL_FILES.containsKey(path)) {
            f = ALL_FILES.get(path);
        } else {
            f = new CompilerInfo(Parser.parse(path.toFile()));
            ALL_FILES.put(path, f);
        }
        f.loadDependents();
        wildcardExports.add(path);
        // FIXME: Register exports accurately
    }

    /**
     * Get a map of strings to types for all compile-time types imported into
     * the represented file.
     *
     * @return The map of strings
     */
    @NotNull
    public Map<String, TypeObject> importedTypes() {
        Map<String, TypeObject> importedTypes = new HashMap<>();
        for (var pair : imports.entrySet()) {
            var path = pair.getKey();
            var importInfo = pair.getValue();
            var strings = importInfo.getNames();
            var asNames = importInfo.getAsNames().orElseGet(importInfo::getNames);
            var importHandler = ALL_FILES.get(path).importHandler();
            if (strings.size() == 1 && strings.get(0).equals("*")) {
                for (var imp : importHandler.exportedTypes(LineInfo.empty()).entrySet()) {
                    var name = imp.getKey();
                    var value = imp.getValue();
                    importedTypes.put(name, value);
                }
            } else for (var names : new Zipper<>(strings, asNames)) {
                var str = names.getKey();
                var as = names.getValue();
                var type = importHandler.exportedType(str, LineInfo.empty(), new ArrayList<>());
                type.ifPresent(typeObject -> importedTypes.put(as, typeObject));
            }
        }
        return importedTypes;
    }

    @NotNull
    private Map<String, TypeObject> exportedTypes(LineInfo lineInfo) {
        Map<String, TypeObject> result = new HashMap<>();
        for (var pair : exports.entrySet()) {
            var name = pair.getKey();
            var type = pair.getValue();
            if (type != null) {
                result.put(name, type);
            } else {
                var typeObj = exportedType(name, lineInfo, new ArrayList<>());
                typeObj.ifPresent(typeObject -> result.put(name, typeObject));
            }
        }
        return result;
    }

    private Optional<TypeObject> exportedType(
            @NotNull String name, LineInfo lineInfo, @NotNull List<Pair<LineInfo, String>> previousFiles
    ) {
        assert !name.equals("*");
        for (var pair : previousFiles) {
            var info = pair.getKey();
            if (info.getPath().equals(this.info.path()) && pair.getValue().equals(name)) {
                throw CompilerException.format("Circular import of '%s': name not defined in any file", info, name);
            }
        }
        if (!exports.containsKey(name)) {
            previousFiles.add(Pair.of(LineInfo.empty(), name));
            for (var path : wildcardExports) {
                var handler = ALL_FILES.get(path).importHandler();
                try {
                    return handler.exportedType(name, LineInfo.empty(), previousFiles);
                } catch (CompilerException ignored) {
                    // If value was not exported, don't fail, just continue
                }
            }
            throw CompilerException.format("No value '%s' was exported", lineInfo, name);
        }
        var export = exports.get(name);
        if (export instanceof TypeTypeObject) {
            return Optional.of(((TypeTypeObject) export).representedType());
        } else if (fromExports.containsKey(name)) {
            var path = fromExports.get(name);
            previousFiles.add(Pair.of(LineInfo.empty(), name));
            return ALL_FILES.get(path).importHandler().exportedType(name, LineInfo.empty(), previousFiles);
        } else {
            return Optional.empty();
        }

    }

    public static void compileAll() {
        loadDefaultInterfaces();
        while (!toCompile.isEmpty()) {
            var nextCompilationRound = toCompile;
            toCompile = new ArrayList<>();
            for (var pair : nextCompilationRound) {
                pair.getKey().link();
            }
            for (var pair : nextCompilationRound) {
                pair.getKey().compile(pair.getValue());
            }
        }
    }

    public static void loadDefaultInterfaces() {
        for (var pair : ALL_DEFAULT_INTERFACES.entrySet()) {
            if (pair.getValue().isPresent()) {
                var infoPair = pair.getValue().get();
                InterfaceConverter.completeType(infoPair.getKey(), infoPair.getValue(), pair.getKey());
                pair.setValue(Optional.empty());
            }
        }
    }

    public IndexedSet<String> getImports() {
        return importStrings;
    }

    private static String moduleName(@NotNull ImportExportNode node, int i) {
        if (!node.getFrom().isEmpty()) {
            return ((VariableNode) node.getFrom().getPreDot()).getName();
        } else {
            return ((VariableNode) node.getValues()[i].getPreDot()).getName();
        }
    }
}
