package main.java.converter;

import main.java.parser.BaseNode;
import main.java.parser.LambdaNode;
import main.java.parser.ReturnStatementNode;
import main.java.parser.StatementBodyNode;
import main.java.parser.TestListNode;
import main.java.parser.TestNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LambdaConverter implements TestConverter {
    private final LambdaNode node;
    private final CompilerInfo info;
    private final int retCount;

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
        } else if (retCount > 1) {
            throw CompilerException.format("Lambda literal only returns one value, not %d", node, retCount);
        }
        List<Byte> bytes = new ArrayList<>();
        var name = info.lambdaName();
        var fnInfo = new FunctionInfo(name, convertArgs(), info.typesOf(node.getReturns()));
        int fnIndex = info.addFunction(new Function(node, fnInfo, convertBody()));
        bytes.add(Bytecode.MAKE_FUNCTION.value);
        bytes.addAll(Util.shortToBytes((short) fnIndex));
        return bytes;
    }

    private TypeObject[] lambdaReturnType() {
        if (node.isArrow() && node.getReturns().length == 0) {
            info.addStackFrame();
            for (var arg : node.getArgs()) {
                info.addVariable(arg.getName().getName(), info.getType(arg.getType()), arg);
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
        var retInfo = info.getFnReturns();
        retInfo.addFunctionReturns(lambdaReturnType());
        for (var arg : node.getArgs()) {
            info.addVariable(arg.getName().getName(), info.getType(arg.getType()), arg);
        }
        List<Byte> fnBytes = new ArrayList<>(BaseConverter.bytes(0, fnBody(), info ));
        info.removeStackFrame();
        retInfo.popFnReturns();
        return fnBytes;
    }

    private BaseNode fnBody() {
        if (node.isArrow()) {
            var body = (TestNode) node.getBody().get(0);
            var list = new TestListNode(body);
            return new StatementBodyNode(new ReturnStatementNode(body.getLineInfo(), list, TestNode.empty()));
        } else {
            return node.getBody();
        }
    }

    @NotNull
    private ArgumentInfo convertArgs() {
        var args = node.getArgs();
        var kwargs = ArgumentInfo.getArgs(info, args.getNameArgs());
        var normalArgs = ArgumentInfo.getArgs(info, args.getArgs());
        var posArgs = ArgumentInfo.getArgs(info, args.getPositionArgs());
        return new ArgumentInfo(kwargs, normalArgs, posArgs);
    }
}
