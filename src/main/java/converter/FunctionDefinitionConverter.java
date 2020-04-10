package main.java.converter;

import main.java.parser.FunctionDefinitionNode;
import main.java.parser.TypedArgumentNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FunctionDefinitionConverter implements BaseConverter {
    private final CompilerInfo info;
    private final FunctionDefinitionNode node;

    public FunctionDefinitionConverter(CompilerInfo info, FunctionDefinitionNode node) {
        this.info = info;
        this.node = node;
    }

    @NotNull
    @Override
    public @Unmodifiable List<Byte> convert(int start) {
        List<Byte> bytes = new ArrayList<>();
        var retTypes =  info.typesOf(node.getRetval());
        var fnInfo = new FunctionInfo(node.getName().getName(), convertArgs(), retTypes);
        int index = info.addFunction(new Function(fnInfo, bytes));
        var constVal = new FunctionConstant(node.getName().getName(), index);
        info.addVariable(node.getName().getName(), fnInfo.toCallable(), constVal);
        info.addStackFrame();
        info.addFunctionReturns(retTypes);
        for (var arg : node.getArgs()) {
            info.addVariable(arg.getName().getName(), info.getType(arg.getType()));
        }
        for (var statement : node.getBody()) {
            bytes.addAll(BaseConverter.bytes(bytes.size(), statement, info));
        }
        info.removeStackFrame();
        info.popFnReturns();
        return Collections.emptyList();
    }

    @NotNull
    private ArgumentInfo convertArgs() {
        var args = node.getArgs();
        var kwargs = convert(args.getNameArgs());
        var normalArgs = convert(args.getArgs());
        var posArgs = convert(args.getPositionArgs());
        return new ArgumentInfo(kwargs, normalArgs, posArgs);
    }

    @NotNull
    private Argument[] convert(@NotNull TypedArgumentNode[] args) {
        var converted = new Argument[args.length];
        for (int i = 0; i < args.length; i++) {
            var arg = args[i];
            converted[i] = new Argument(arg.getName().getName(), info.getType(arg.getType()));
        }
        return converted;
    }
}
