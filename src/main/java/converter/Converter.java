package main.java.converter;

import main.java.parser.LineInfo;
import main.java.parser.Parser;
import main.java.parser.TopNode;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class Converter {
    private static final FilenameFilter EXPORT_FILTER = (f, s) -> s.equals(Util.EXPORTS_FILENAME);
    private static final Map<String, CompilerInfo> modules = new HashMap<>();

    private static File destFile = null;

    private Converter() {}

    public static void convertToFile(@NotNull File file, TopNode node) {
        assert destFile == null || destFile.equals(file.getParentFile());
        setDestFile(file.getParentFile());
        new CompilerInfo(node).compile(file);
    }

    public static CompilerInfo findModule(String name) {
        if (modules.containsKey(name)) {
            return modules.get(name);
        }
        var path = System.getenv("NEWLANG_PATH");
        for (String filename : path.split(":")) {
            try (var walker = Files.walk(Path.of(filename))) {
                var result = walker.filter(Converter::isModule)
                        .filter(f -> f.endsWith(name + Util.FILE_EXTENSION) || f.endsWith(name))
                        .collect(Collectors.toList());
                if (!result.isEmpty()) {
                    return getInfo(result, name);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        var builtinPath = builtinPath().toFile().list();
        assert builtinPath != null;
        for (var builtin : builtinPath) {
            var finalName = Path.of(builtin).getFileName().toString();
            if (isModule(builtinPath().resolve(builtin)) && nameMatches(name, finalName)) {
                return getInfo(List.of(builtinPath().resolve(builtin)), name);
            }
        }
        throw CompilerException.of("Cannot find module " + name, LineInfo.empty());
    }

    @NotNull
    public static CompilerInfo findLocalModule(@NotNull Path parentPath, String name) {
        List<Path> result = new ArrayList<>();
        for (var file : Objects.requireNonNull(parentPath.toFile().listFiles())) {
            var path = file.toPath();
            if (isModule(path) && path.endsWith(name + Util.FILE_EXTENSION)) {
                result.add(path);
            }
        }
        if (!result.isEmpty()) {
            return getInfo(result, name);
        }
        throw CompilerException.of("Cannot find module " + name, LineInfo.empty());
    }

    static File getDestFile() {
        return destFile;
    }

    private static boolean isModule(Path path) {
        if (Files.isRegularFile(path)) {
            return path.toString().endsWith(Util.FILE_EXTENSION);
        } else {
            var files = path.toFile().list(EXPORT_FILTER);
            return files != null && files.length > 0;
        }
    }

    private static void setDestFile(File file) {
        assert destFile == null;
        destFile = file;
    }

    @NotNull
    private static Path builtinPath() {
        return new File("Lib").toPath().toAbsolutePath();
    }

    @NotNull
    private static CompilerInfo getInfo(@NotNull List<Path> result, String name) {
        var endFile = result.get(0).toFile();
        if (endFile.isDirectory()) {
            var exportFiles = endFile.listFiles(EXPORT_FILTER);
            assert exportFiles != null && exportFiles.length == 1;
            endFile = exportFiles[0];
        }
        var info = new CompilerInfo(Parser.parse(endFile));
        modules.put(name, info);
        return info;
    }

    private static boolean nameMatches(@NotNull String wantedName, @NotNull String actualName) {
        return wantedName.equals(actualName) || (wantedName + Util.FILE_EXTENSION).equals(actualName);
    }
}
