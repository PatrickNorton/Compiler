package main.java.converter;

import main.java.parser.LambdaNode;
import main.java.parser.TestNode;
import main.java.parser.TypedArgumentNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LambdaConverter implements TestConverter {
    private LambdaNode node;
    private CompilerInfo info;
    private int retCount;

    public LambdaConverter(CompilerInfo info, LambdaNode node, int retCount) {
        this.node = node;
        this.info = info;
        this.retCount = retCount;
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        return new TypeObject[] {new FunctionInfo(convertArgs(), lambdaReturnType()).toCallable()};
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        if (retCount == 0) {
            CompilerWarning.warn("Unused lambda", node);
            return Collections.emptyList();
        }
        assert retCount == 1;
        List<Byte> bytes = new ArrayList<>();
        var name = info.lambdaName();
        var fnInfo = new FunctionInfo(name, convertArgs(), info.typesOf(node.getReturns()));
        int fnIndex = info.addFunction(new Function(fnInfo, convertBody()));
        bytes.add(Bytecode.MAKE_FUNCTION.value);
        bytes.addAll(Util.shortToBytes((short) fnIndex));
        return bytes;
    }

    private TypeObject[] lambdaReturnType() {
        if (node.isArrow() && node.getReturns().length == 0) {
            info.addStackFrame();
            for (var arg : node.getArgs()) {
                info.addVariable(arg.getName().getName(), info.getType(arg.getType()));
            }
            var retType = TestConverter.returnType((TestNode) node.getBody().get(0), info, 1);
            info.removeStackFrame();
            return retType;
        } else {
            return info.typesOf(node.getReturns());
        }
    }

    @NotNull
    private List<Byte> convertBody() {
        info.addStackFrame();
        info.addFunctionReturns(lambdaReturnType());
        for (var arg : node.getArgs()) {
            info.addVariable(arg.getName().getName(), info.getType(arg.getType()));
        }
        List<Byte> fnBytes = new ArrayList<>(node.isArrow()
                ? TestConverter.bytes(0, (TestNode) node.getBody().get(0), info, lambdaReturnType().length)
                : BaseConverter.bytes(0, node.getBody(), info));
        info.removeStackFrame();
        info.popFnReturns();
        return fnBytes;
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
