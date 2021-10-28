package main.java.converter;

import main.java.converter.bytecode.ArgcBytecode;
import main.java.parser.FunctionDefinitionNode;
import org.jetbrains.annotations.NotNull;

public final class TestFnConverter {
    public static void convertTestFunction(@NotNull CompilerInfo info, FunctionDefinitionNode node) {
        if (info.globalInfo().isTest()) {
            var constant = FunctionDefinitionConverter.convertWithConstant(info, node);
            var fnInfo = info.fnInfo(constant.getName()).orElseThrow();
            if (!fnInfo.matches()) {
                throw CompilerException.of("Test functions must have an empty argument list", node);
            }
            info.globalInfo().addTestFunction(constant);
        }
    }

    public static int convertTestStart(@NotNull GlobalCompilerInfo info) {
        var testFunctions = info.getTestFunctions();
        BytecodeList bytes = new BytecodeList();
        bytes.loadConstant(Builtins.testConstant(), info);
        for (var constant : testFunctions) {
            bytes.loadConstant(constant, info);
        }
        bytes.add(Bytecode.CALL_TOS, new ArgcBytecode((short) testFunctions.size()));
        var fnInfo = new FunctionInfo("__default__$test");
        var function = new Function(fnInfo, bytes);
        return info.addFunction(function);
    }
}
