package main.java.converter;

import main.java.parser.LineInfo;
import main.java.parser.Parser;

import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class Converter {
    private static final FilenameFilter EXPORT_FILTER = (f, s) -> s.equals(Util.EXPORTS_FILENAME);
    private static final Map<String, FileInfo> modules = new HashMap<>();

    private Converter() {}

    public static FileInfo findModule(String name) {
        if (modules.containsKey(name)) {
            return modules.get(name);
        }
        var path = System.getenv("PATH");
        for (String filename : path.split(":")) {
            try (var walker = Files.walk(Path.of(filename))) {
                var result = walker.filter(Converter::isModule)
                        .collect(Collectors.toList());
                if (!result.isEmpty()) {
                    var endFile = result.get(0).toFile();
                    if (endFile.isDirectory()) {
                        var exportFiles = endFile.listFiles(EXPORT_FILTER);
                        assert exportFiles != null && exportFiles.length == 1;
                        endFile = exportFiles[0];
                    }
                    var info = new FileInfo(Parser.parse(endFile));
                    modules.put(name, info);
                    return info;
                }
            } catch (IOException e) {
                // FIXME: Handle this
            }
        }
        throw CompilerException.of("Cannot find module " + name, LineInfo.empty());
    }

    private static boolean isModule(Path path) {
        if (Files.isRegularFile(path)) {
            return path.endsWith(Util.FILE_EXTENSION);
        } else {
            var files = path.toFile().list(EXPORT_FILTER);
            assert files != null;
            return files.length > 0;
        }
    }
}
