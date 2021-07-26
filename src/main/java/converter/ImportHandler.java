package main.java.converter;

import main.java.parser.BaseClassNode;
import main.java.parser.ClassDefinitionNode;
import main.java.parser.DescriptorNode;
import main.java.parser.EnumDefinitionNode;
import main.java.parser.ImportExportNode;
import main.java.parser.IndependentNode;
import main.java.parser.InterfaceDefinitionNode;
import main.java.parser.LineInfo;
import main.java.parser.Lined;
import main.java.parser.Parser;
import main.java.parser.TopNode;
import main.java.parser.TypedefStatementNode;
import main.java.parser.UnionDefinitionNode;
import main.java.parser.VariableNode;
import main.java.util.IndexedHashSet;
import main.java.util.IndexedSet;
import main.java.util.OptionalUint;
import main.java.util.Pair;
import main.java.util.Zipper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
 * <p>
 *     Because of the complexity of the linking process, in which this plays an
 *     essential role, the code in here can be somewhat opaque (read:
 *     horrifically broken in ways I may not have found yet). In particular,
 *     there are several methods which only expect to be called once per object.
 *     Read their documentation before calling to make sure you're not breaking
 *     any precious invariants.
 * </p>
 *
 * @author Patrick Norton
 * @see ImportExportConverter
 * @see CompilerInfo
 */
public final class ImportHandler {
    public static final Map<Path, CompilerInfo> ALL_FILES = new HashMap<>();
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
    private final Map<String, OptionalUint> exportConstants = new HashMap<>();
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
     *     This method assumes it is only called once per object and will
     *     almost surely give weird results if broken. Caution is advised when
     *     adding callees.
     * </p>
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
        Map<String, Pair<TypeObject, Lined>> types = new HashMap<>();
        Map<String, LineInfo> lineInfos = new HashMap<>();
        Set<TypeObject> definedInFile = new HashSet<>();
        boolean isModule = false;
        Optional<InterfaceDefinitionNode> hasAuto = Optional.empty();
        Deque<TypedefStatementNode> typedefs = new ArrayDeque<>();
        loadInfo(Converter.builtinPath().resolve("__builtins__.newlang"), "__builtins__", PermissionLevel.BUILTIN);
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
            } else if (stmt instanceof InterfaceDefinitionNode) {
                var cls = (InterfaceDefinitionNode) stmt;
                var type = (InterfaceType) registerClass(types, lineInfos, definedInFile, stmt);
                if (cls.getDescriptors().contains(DescriptorNode.AUTO)) {
                    ALL_DEFAULT_INTERFACES.put(type, Optional.of(Pair.of(info, cls)));
                    hasAuto = Optional.of(cls);
                }
            } else if (stmt instanceof BaseClassNode) {
                registerClass(types, lineInfos, definedInFile, stmt);
            } else if (stmt instanceof TypedefStatementNode) {
                typedefs.push((TypedefStatementNode) stmt);
            }
        }
        if (!isModule && hasAuto.isPresent()) {
            throw CompilerException.of("Cannot (yet?) have 'auto' interfaces in non-module file", hasAuto.get());
        }
        for (var stmt : typedefs) {
            var type = stmt.getType();
            var name = stmt.getName();
            var cls = info.getType(type).typedefAs(name.strName());
            types.put(name.strName(), Pair.of(cls, stmt));
        }
        for (var export : exports.entrySet()) {
            if (types.containsKey(export.getKey())) {
                export.setValue(Builtins.type().generify(types.get(export.getKey()).getKey()));
            }
        }
        if (isModule) {
            info.addPredeclaredTypes(types);
        }
        info.accessHandler().setDefinedInFile(definedInFile);
    }

    private UserType<?> registerClass(
            Map<String, Pair<TypeObject, Lined>> types,
            Map<String, LineInfo> lineInfos,
            Set<TypeObject> definedInFile,
            IndependentNode stmt
    ) {
        var cls = (BaseClassNode) stmt;
        var strName = cls.getName().strName();
        if (types.containsKey(strName)) {
            throw CompilerException.doubleDef(strName, stmt.getLineInfo(), lineInfos.get(strName));
        }
        var generics = GenericInfo.parseNoTypes(cls.getName().getSubtypes());
        UserType<?> type;
        if (stmt instanceof ClassDefinitionNode || stmt instanceof EnumDefinitionNode) {
            type = new StdTypeObject(strName, generics);
        } else if (stmt instanceof UnionDefinitionNode) {
            type = new UnionTypeObject(strName, generics);
        } else if (stmt instanceof InterfaceDefinitionNode) {
            type = new InterfaceType(strName, generics);
        } else {
            throw CompilerInternalError.format("Unknown class type %s", stmt, stmt.getClass());
        }
        generics.setParent(type);
        var isBuiltin = AnnotationConverter.isBuiltin(cls, info, cls.getAnnotations());
        if (isBuiltin.isPresent()) {
            var builtin = isBuiltin.orElseThrow();
            Builtins.setBuiltin(builtin.getName(), builtin.getIndex(), builtin.isHidden(), type);
        } else {
            types.put(strName, Pair.of(type, cls));
            lineInfos.put(strName, cls.getLineInfo());
            definedInFile.add(type);
        }
        return type;
    }


    /**
     * Takes the already-linked {@link Linker linker} for this file and uses it
     * to set export information.
     * <p>
     *     This is called as part of the process in {@link
     *     CompilerInfo#compile()} and probably should not be used anywhere
     *     else. It assumes it is only called once per object, but <i>probably
     *     </i> won't break if you do otherwise. If you do put in another call
     *     site, double-check this won't do anything weird.
     * </p>
     *
     * @param linker The linker from which to set exports
     */
    public void setFromLinker(@NotNull Linker linker) {
        var globals = linker.getGlobals();
        var constants = linker.getConstants();
        for (var entry : globals.entrySet()) {
            var name = entry.getKey();
            var type = entry.getValue();
            if (exports.containsKey(name)) {
                exports.put(name, type);
                exportConstants.put(name, OptionalUint.ofNullable(constants.get(name)));
            }
        }
        for (var exportName : this.exports.keySet()) {
            if (exportConstants.get(exportName) == null) {
                exportConstants.put(exportName, OptionalUint.ofNullable(constants.get(exportName)));
            }
        }
    }

    /**
     * Adds a non-top level import to the pool.
     * <p>
     *     As with almost everything in this class, it is probably horrifically
     *     broken in several ways. If you use it and everything starts breaking,
     *     check here first.
     * </p>
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
            imports.put(path, new ImportInfo(node.getLineInfo(), imports.size(), importValues));
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
                imports.put(path, new ImportInfo(node.getLineInfo(), imports.size(), List.of()));
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
                var valueStr = value.toString();
                values.add(valueStr);
                var valStr = from.toString() + "." + valueStr;
                var as = node.getAs().length == 0 ? valueStr : node.getAs()[i].toString();
                importStrings.add(valStr);
                result.put(as, importStrings.indexOf(valStr));
            }
            var path = loadFile(from.toString(), node);
            imports.put(path, new ImportInfo(node.getLineInfo(), imports.size(), values));
            return result;
        }
    }

    private void registerImports(@NotNull ImportExportNode node) {
        assert node.getType() == ImportExportNode.IMPORT || node.getType() == ImportExportNode.TYPEGET;
        if (node.isWildcard()) {
            var path = loadFile(moduleName(node, 0), node);
            imports.put(path, new ImportInfo(node.getLineInfo(), imports.size(), List.of("*")));
        } else if (node.getFrom().isEmpty()) {
            checkAs(node);
            for (int i = 0; i < node.getValues().length; i++) {
                var val = node.getValues()[i];
                var preDot = ((VariableNode) val.getPreDot()).getName();
                assert val.getPostDots().length == 0;
                var path = loadFile(preDot, node);
                var valStr = val.toString();
                imports.put(path, new ImportInfo(node.getLineInfo(), imports.size(), List.of()));
                importStrings.add(valStr);
            }
        } else {
            checkAs(node);
            var from = node.getFrom();
            List<String> values = new ArrayList<>();
            for (int i = 0; i < node.getValues().length; i++) {
                var value = node.getValues()[i];
                var valueStr = value.toString();
                values.add(valueStr);
                var valStr = from.toString() + "." + valueStr;
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
            if (!imports.containsKey(path)) {
                imports.put(path, new ImportInfo(node.getLineInfo(), imports.size(), values, asNames));
            } else {
                imports.put(path, imports.get(path).merge(values, asNames));
            }
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

    /**
     * Gets a collection of all the exported names in the file.
     * <p>
     *     This mainly exists for iteration purposes. It is unmodifiable, so
     *     don't even try.
     * </p>
     *
     * @return A view of all the exports in the file
     */
    @Contract(pure = true)
    @NotNull
    @UnmodifiableView
    public Collection<String> getExports() {
        return Collections.unmodifiableSet(exports.keySet());
    }

     /**
     * Gets a collection of all the exported names in the file and their types.
     * <p>
     *     This mainly exists for iteration purposes. It is unmodifiable, so
     *     don't even try.
     * </p>
     *
     * @return A view of all the exports in the file
     */
    @Contract(pure = true)
    @NotNull
    @UnmodifiableView
    public Collection<Map.Entry<String, TypeObject>> exportTypes() {
        return Collections.unmodifiableSet(exports.entrySet());
    }

    public void setExportType(String name, TypeObject type) {
        exports.put(name, type);
    }

    private void registerWildcardImport(String moduleName, @NotNull ImportExportNode node) {
        var path = loadFile(moduleName, node);
        imports.put(path, new ImportInfo(node.getLineInfo(), imports.size(), List.of("*")));
        // FIXME: Add to importStrings
    }

    private Path loadFile(String moduleName, @NotNull ImportExportNode node) {
        Path path;
        boolean isStdlib;
        if (node.getPreDots() > 0) {
            var parentPath = info.path();
            for (int i = 0; i < node.getPreDots(); i++) {
                parentPath = parentPath.getParent();
            }
            path = Converter.localModulePath(parentPath, moduleName, node);
            isStdlib = false;
        } else {
            var pair = Converter.findPath(moduleName, node);
            path = pair.getKey();
            isStdlib = pair.getValue();
        }
        loadInfo(path, moduleName, isStdlib ? PermissionLevel.STDLIB : info.permissions());
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
        if (node.getPreDots() > 0) {
            var path = Converter.localModulePath(info.path().getParent(), moduleName, node);
            loadInfo(path, moduleName, info.permissions());
            wildcardExports.add(path);
        } else {
            var pair = Converter.findPath(moduleName, node);
            loadInfo(pair.getKey(), moduleName, pair.getValue() ? PermissionLevel.STDLIB : info.permissions());
            wildcardExports.add(pair.getKey());
        }
        // FIXME: Register exports accurately
    }

    private void loadInfo(Path path, String moduleName, PermissionLevel level) {
        CompilerInfo f;
        if (ALL_FILES.containsKey(path)) {
            f = ALL_FILES.get(path);
        } else {
            f = new CompilerInfo(Parser.parse(path.toFile()), info.globalInfo(), level);
            ALL_FILES.put(path, f);
            toCompile.add(Pair.of(f, Converter.resolveFile(info.globalInfo().destFile(), moduleName)));
        }
        f.loadDependents();
    }

    /**
     * Get a map of strings to types for all compile-time types imported into
     * the represented file.
     *
     * @return The map of strings
     */
    @NotNull
    public Map<String, Pair<TypeObject, Lined>> importedTypes() {
        Map<String, Pair<TypeObject, Lined>> importedTypes = new HashMap<>();
        for (var pair : imports.entrySet()) {
            var path = pair.getKey();
            var importInfo = pair.getValue();
            var strings = importInfo.getNames();
            var asNames = importInfo.getAsNames().orElseGet(importInfo::getNames);
            var importHandler = ALL_FILES.get(path).importHandler();
            if (strings.size() == 1 && strings.get(0).equals("*")) {
                for (var imp : importHandler.exportedTypes(importInfo.getLineInfo()).entrySet()) {
                    var name = imp.getKey();
                    var value = imp.getValue();
                    importedTypes.put(name, Pair.of(value, importInfo));
                }
            } else for (var names : Zipper.of(strings, asNames)) {
                var str = names.getKey();
                var as = names.getValue();
                var type = importHandler.exportedType(str, importInfo.getLineInfo(), new ArrayList<>());
                type.ifPresent(typeObject -> importedTypes.put(as, Pair.of(typeObject, importInfo)));
            }
        }
        return importedTypes;
    }

    @NotNull
    public TypeObject importedType(@NotNull Lined lineInfo, Path file, String name) {
        var handler = ALL_FILES.get(file);
        return handler.importHandler().typeOfExport(name, lineInfo.getLineInfo(), new ArrayList<>());
    }

    @NotNull
    public OptionalUint importedConstant(@NotNull Lined lineInfo, Path file, String name) {
        var handler = ALL_FILES.get(file);
        return handler.importHandler().importedConst(name, lineInfo.getLineInfo(), new ArrayList<>());
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
        checkCircular(name, previousFiles);
        if (!exports.containsKey(name)) {
            previousFiles.add(Pair.of(lineInfo, name));
            for (var path : wildcardExports) {
                var handler = ALL_FILES.get(path).importHandler();
                try {
                    return handler.exportedType(name, lineInfo, previousFiles);
                } catch (CompilerException ignored) {
                    // If value was not exported, don't fail, just continue
                }
            }
            throw CompilerException.format("No value '%s' was exported from file '%s'", lineInfo, name, info.sourceFile());
        }
        var export = exports.get(name);
        if (export instanceof TypeTypeObject) {
            return Optional.of(((TypeTypeObject) export).representedType());
        } else if (fromExports.containsKey(name)) {
            var path = fromExports.get(name);
            previousFiles.add(Pair.of(lineInfo, name));
            return ALL_FILES.get(path).importHandler().exportedType(name, lineInfo, previousFiles);
        } else {
            return Optional.empty();
        }
    }

    @NotNull
    public TypeObject typeOfExport(
            @NotNull String name, LineInfo lineInfo, @NotNull List<Pair<LineInfo, String>> previousFiles
    ) {
        assert !name.equals("*");
        checkCircular(name, previousFiles);
        if (!exports.containsKey(name)) {
            previousFiles.add(Pair.of(lineInfo, name));
            for (var path : wildcardExports) {
                var handler = ALL_FILES.get(path).importHandler();
                try {
                    return handler.typeOfExport(name, lineInfo, previousFiles);
                } catch (CompilerException ignored) {
                    // If value was not exported, don't fail, just continue
                }
            }
            throw CompilerException.format("No value '%s' was exported from file '%s'", lineInfo, name, info.sourceFile());
        }
        var export = exports.get(name);
        if (export != null) {
            return export;
        } else if (fromExports.containsKey(name)) {
            var path = fromExports.get(name);
            previousFiles.add(Pair.of(lineInfo, name));
            return ALL_FILES.get(path).importHandler().typeOfExport(name, lineInfo, previousFiles);
        } else {
            throw CompilerException.format("No value '%s' was exported from file '%s'", lineInfo, name, info.sourceFile());
        }
    }

    @NotNull
    public OptionalUint importedConst(
            @NotNull String name, LineInfo lineInfo, @NotNull List<Pair<LineInfo, String>> previousFiles
    ) {
        assert !name.equals("*");
        checkCircular(name, previousFiles);
        if (!exports.containsKey(name)) {
            previousFiles.add(Pair.of(lineInfo, name));
            for (var path : wildcardExports) {
                var handler = ALL_FILES.get(path).importHandler();
                try {
                    return handler.importedConst(name, lineInfo, previousFiles);
                } catch (CompilerException ignored) {
                    // If value was not exported, don't fail, just continue
                }
            }
            throw CompilerException.format("No value '%s' was exported from file '%s'", lineInfo, name, info.sourceFile());
        }
        var export = exportConstants.get(name);
        if (export != null && export.isPresent()) {
            return export;
        } else if (fromExports.containsKey(name)) {
            var path = fromExports.get(name);
            previousFiles.add(Pair.of(lineInfo, name));
            return ALL_FILES.get(path).importHandler().importedConst(name, lineInfo, previousFiles);
        } else if (export != null) {
            return export;
        } else {
            throw CompilerException.format("No value '%s' was exported from file '%s'", lineInfo, name, info.sourceFile());
        }
    }

    private void checkCircular(String name, @NotNull List<Pair<LineInfo, String>> previousFiles) {
        for (var pair : previousFiles) {
            var info = pair.getKey();
            if (info.getPath().equals(this.info.path()) && pair.getValue().equals(name)) {
                throw CompilerException.format("Circular import of '%s': name not defined in any file", info, name);
            }
        }
    }

    public static void compileAll(CompilerInfo info) {
        var start = System.nanoTime();
        loadDefaultInterfaces();
        while (!toCompile.isEmpty()) {
            var nextCompilationRound = toCompile;
            toCompile = new ArrayList<>();
            info.link();
            for (var pair : nextCompilationRound) {
                pair.getKey().link();
            }
            info.compile();
            for (var pair : nextCompilationRound) {
                pair.getKey().compile();
            }
        }
        var end = System.nanoTime();
        var elapsed = (end - start) / 1_000_000_000.;
        var counter = info.globalInfo().getWarnings();
        System.out.printf(
                "Compilation finished in %.2fs with %d errors and %d warnings%n",
                elapsed, counter.getErrors(), counter.getWarnings()
        );
    }

    public static void loadDefaultInterfaces() {
        for (var pair : ALL_DEFAULT_INTERFACES.entrySet()) {
            if (pair.getValue().isPresent()) {
                var infoPair = pair.getValue().get();
                InterfaceConverter.completeWithoutReserving(infoPair.getKey(), infoPair.getValue(), pair.getKey());
                pair.setValue(Optional.empty());
            }
        }
    }

    public IndexedSet<String> getImports() {
        return importStrings;
    }

    public Map<Path, ImportInfo> importInfos() {
        return imports;
    }

    private static String moduleName(@NotNull ImportExportNode node, int i) {
        if (!node.getFrom().isEmpty()) {
            return ((VariableNode) node.getFrom().getPreDot()).getName();
        } else {
            return ((VariableNode) node.getValues()[i].getPreDot()).getName();
        }
    }
}
