package main.java.converter;

import main.java.parser.Lined;
import main.java.parser.TopNode;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A static class containing the methods needed to find modules for compilation.
 *
 * @author Patrick Norton
 */
public final class Converter {
    private static final FilenameFilter EXPORT_FILTER = (f, s) -> s.equals(Util.EXPORTS_FILENAME);

    private static File destFile = null;

    private Converter() {}

    /**
     * Compiles a source file, with a path and a {@link TopNode} representing
     * the parsed contents, and writes it to a file.
     * <p>
     *     This method may not be called more than once per compilation.
     * </p>
     *
     * @throws CompilerInternalError if called more than once
     * @param file The name of the file compiled
     * @param node The AST node to compile
     */
    public static void convertToFile(@NotNull File file, TopNode node) {
        if (destFile != null) {
            throw CompilerInternalError.of(
                    "Cannot call Converter#convertToFile more than once per compilation", node
            );
        }
        destFile = file.getParentFile();
        var info = new CompilerInfo(node, new GlobalCompilerInfo()).link();
        ImportHandler.compileAll(info);
        info.writeToFile(file);
    }

    /**
     * Finds the path to a non-local module given its name from the source code.
     * <p>
     *     The module name is resolved as follows: If the internal cache of
     *     pre-compiled modules contains the name, it uses it. Otherwise, it
     *     uses the environment variable {@code NEWLANG_PATH} and searches for
     *     an identically-named file there. Otherwise, it checks the builtins
     *     folder.
     * </p>
     * @param name The name of the module
     * @return The {@link Path path} to the module
     */
    @NotNull
    public static Path findPath(String name, Lined info) {
        var path = System.getenv("NEWLANG_PATH");
        for (String filename : path.split(":")) {
            if (!filename.isEmpty()) {
                try (var walker = Files.walk(Path.of(filename))) {
                    var result = walker.filter(Converter::isModule)
                            .filter(f -> f.endsWith(name + Util.FILE_EXTENSION) || f.endsWith(name))
                            .collect(Collectors.toList());
                    if (!result.isEmpty()) {
                        return getPath(result, name, info);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        var builtinPath = builtinPath().toFile().list();
        assert builtinPath != null;
        for (var builtin : builtinPath) {
            var finalName = Path.of(builtin).getFileName().toString();
            if (isModule(builtinPath().resolve(builtin)) && nameMatches(name, finalName)) {
                return getPath(List.of(builtinPath().resolve(builtin)), name, info);
            }
        }
        throw CompilerException.of("Cannot find module " + name, info);
    }

    /**
     * Finds the path to a local module given its name and the path to the
     * current file's parent folder.
     * <p>
     *     Local modules are found by searching the parent file for the name
     *     given. More dots at the beginning means higher-up files are
     *     searched.
     * </p>
     *
     * @param parentPath The path to the parent file
     * @param name The name of the module
     * @param lineInfo The info for the line of the import
     * @return The {@link Path path} to the file
     */
    @NotNull
    public static Path localModulePath(@NotNull Path parentPath, String name, Lined lineInfo) {
        List<Path> result = new ArrayList<>();
        for (var file : Objects.requireNonNull(parentPath.toFile().listFiles())) {
            var path = file.toPath();
            if (isModule(path) && path.endsWith(name + Util.FILE_EXTENSION) || path.endsWith(name)) {
                result.add(path);
            }
        }
        if (!result.isEmpty()) {
            return getPath(result, name, lineInfo);
        }
        throw CompilerException.of("Cannot find module " + name, lineInfo);
    }

    @NotNull
    static File resolveFile(String name) {
        return destFile.toPath().resolve(name + Util.BYTECODE_EXTENSION).toFile();
    }

    private static boolean isModule(Path path) {
        if (Files.isRegularFile(path)) {
            return path.toString().endsWith(Util.FILE_EXTENSION);
        } else {
            var files = path.toFile().list(EXPORT_FILTER);
            return files != null && files.length > 0;
        }
    }

    @NotNull
    public static Path builtinPath() {
        return new File("Lib").toPath().toAbsolutePath();
    }

    @NotNull
    private static Path getPath(@NotNull List<Path> result, String name, Lined lineInfo) {
        var endFile = result.get(0).toFile();
        if (endFile.isDirectory()) {
            var exportFiles = endFile.listFiles(EXPORT_FILTER);
            assert exportFiles != null;
            if (exportFiles.length == 0) {
                throw CompilerException.format("No exports file for module %s", lineInfo, name);
            }
            assert exportFiles.length == 1;
            endFile = exportFiles[0];
        }
        return endFile.toPath();
    }

    private static boolean nameMatches(@NotNull String wantedName, @NotNull String actualName) {
        return wantedName.equals(actualName) || (wantedName + Util.FILE_EXTENSION).equals(actualName);
    }
}
