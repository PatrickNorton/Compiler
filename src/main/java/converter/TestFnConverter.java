package main.java.converter;

import main.java.parser.FunctionDefinitionNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class TestFnConverter {
    public static void convertTestFunction(@NotNull CompilerInfo info, FunctionDefinitionNode node) {
        if (info.globalInfo().isTest()) {
            var constant = FunctionDefinitionConverter.convertWithConstant(info, node);
            var fnInfo = info.fnInfo(constant.getName()).orElseThrow();
            if (!fnInfo.getArgs().matches()) {
                throw CompilerException.of("Test functions must have an empty argument list", node);
            }
            info.globalInfo().addTestFunction(constant);
        }
    }

    public static int convertTestStart(@NotNull GlobalCompilerInfo info) {
        var testFunctions = info.getTestFunctions();
        List<Byte> bytes = new ArrayList<>();
        bytes.add(Bytecode.LOAD_CONST.value);
        bytes.addAll(Util.shortToBytes(info.constIndex(Builtins.testConstant())));
        for (var constant : testFunctions) {
            bytes.add(Bytecode.LOAD_CONST.value);
            bytes.addAll(Util.shortToBytes(info.constIndex(constant)));
        }
        bytes.add(Bytecode.CALL_TOS.value);
        bytes.addAll(Util.shortToBytes((short) testFunctions.size()));
        var fnInfo = new FunctionInfo("__default__$test");
        var function = new Function(fnInfo, bytes);
        return info.addFunction(function);
    }
}
