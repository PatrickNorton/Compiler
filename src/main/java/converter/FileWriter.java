package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * The class that writes a compiled source to a file.
 * <p>
 *     For information on the file layout, see {@link #writeToFile}.
 * </p>
 *
 * @author Patrick Norton
 * @see #writeToFile
 */
public final class FileWriter {
    private final CompilerInfo info;

    public FileWriter(CompilerInfo info) {
        this.info = info;
    }

    /**
     * Writes the compiled file to the file specified.
     * <p>
     *     The file layout is as follows:
     * <code><pre>
     * Magic number: 0xABADE66
     * Imports:
     *     Name of import
     *     Name of import
     * Exports:
     *     Name of export
     *     Index of constant
     * Constants:
     *     Byte representation of each constant ({@link LangConstant#toBytes})
     * Functions:
     *     Function name
     *     Whether it is a generator or not
     *     Number of local variables (currently unused)
     *     Length of the bytecode
     *     Bytecode
     * Classes:
     *     Byte representation of the class ({@link ClassInfo#toBytes})
     * Tables:
     *     Byte representation of the table ({@link SwitchTable#toBytes})
     * </pre></code>
     * </p>
     *
     * @param file The file to write to
     */
    public void writeToFile(@NotNull File file) {
        printDisassembly();
        if (!file.getParentFile().exists()) {
            if (!file.getParentFile().mkdir()) {
                throw new RuntimeException("Could not create file " + file);
            }
        }
        try (var writer = Files.newOutputStream(file.toPath())) {
            writer.write(Util.MAGIC_NUMBER);
            writer.write(Util.zeroByteArray());  // In statically linked file, there are no imports or exports
            writer.write(Util.zeroByteArray());
            writer.flush();
            var constants = info.getConstants();
            writer.write(Util.toByteArray(constants.size()));
            for (var constant : constants) {
                var byteArray = Util.toByteArray(constant.toBytes());
                writer.write(byteArray);
            }
            writer.flush();
            var functions = info.getFunctions();
            writer.write(Util.toByteArray(functions.size()));
            for (var function : functions) {
                var byteArray = Util.toByteArray(function.getBytes());
                writer.write(Util.toByteArray(StringConstant.strBytes(function.getName())));
                writer.write(function.isGenerator() ? 1 : 0);
                writer.write(Util.toByteArray((short) 0));  // TODO: Put variable count
                writer.write(Util.toByteArray(byteArray.length));
                writer.write(byteArray);
            }
            writer.flush();
            var classes = info.getClasses();
            writer.write(Util.toByteArray(classes.size()));
            for (var cls : classes) {
                writer.write(Util.toByteArray(cls.toBytes()));
            }
            writer.flush();
            var tables = info.getTables();
            writer.write(Util.toByteArray(tables.size()));
            for (var tbl : tables) {
                writer.write(Util.toByteArray(tbl.toBytes()));
            }
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("Error in writing bytecode to file:\n" + e.getMessage());
        }
    }

    private void printDisassembly() {
        System.out.println(info.sourceFile());
        System.out.println("Constants:");
        var constants = info.getConstants();
        for (var constant : constants) {
            System.out.printf("%d: %s%n", constants.indexOf(constant), constant.name(constants));
        }
        System.out.println();
        var functions = info.getFunctions();
        for (var function : functions) {
            System.out.printf("%s:%n", function.getName());
            System.out.println(Bytecode.disassemble(info, function.getBytes()));
        }
        var classes = info.getClasses();
        for (var cls : classes) {
            for (var fnPair : cls.getMethodDefs().entrySet()) {
                System.out.printf("%s.%s:%n", cls.getType().name(), fnPair.getKey());
                System.out.println(Bytecode.disassemble(info, fnPair.getValue().getBytes()));
            }
            for (var fnPair : cls.getStaticMethods().entrySet()) {
                System.out.printf("%s.%s:%n", cls.getType().name(), fnPair.getKey());
                System.out.println(Bytecode.disassemble(info, fnPair.getValue().getBytes()));
            }
            for (var opPair : cls.getOperatorDefs().entrySet()) {
                System.out.printf("%s.%s:%n", cls.getType().name(), opPair.getKey().toString());
                System.out.println(Bytecode.disassemble(info, opPair.getValue().getBytes()));
            }
            for (var propPair : cls.getProperties().entrySet()) {
                System.out.printf("%s.%s.get:%n", cls.getType().name(), propPair.getKey());
                System.out.println(Bytecode.disassemble(info, propPair.getValue().getKey().getBytes()));
                System.out.printf("%s.%s.set:%n", cls.getType().name(), propPair.getKey());
                System.out.println(Bytecode.disassemble(info, propPair.getValue().getValue().getBytes()));
            }
        }
        for (int i = 0; i < info.getTables().size(); i++) {
            System.out.printf("Table %d:%n", i);
            System.out.println(info.getTables().get(i).strDisassembly());
        }
    }
}
