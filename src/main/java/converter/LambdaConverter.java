package main.java.converter;

import main.java.parser.BaseNode;
import main.java.parser.LambdaNode;
import main.java.parser.ReturnStatementNode;
import main.java.parser.StatementBodyNode;
import main.java.parser.TestListNode;
import main.java.parser.TestNode;
import main.java.parser.TypedArgumentNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class LambdaConverter implements TestConverter {
    private final LambdaNode node;
    private final CompilerInfo info;
    private final int retCount;
    private final TypeObject[] expectedReturns;

    public LambdaConverter(CompilerInfo info, LambdaNode node, int retCount, TypeObject[] expectedReturns) {
        this.node = node;
        this.info = info;
        this.retCount = retCount;
        this.expectedReturns = expectedReturns;
    }

    public LambdaConverter(CompilerInfo info, LambdaNode node, int retCount) {
        this.node = node;
        this.info = info;
        this.retCount = retCount;
        this.expectedReturns = null;
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        return new TypeObject[] {new FunctionInfo(convertArgs(), lambdaReturnType()).toCallable()};
    }

    @NotNull
    @Override
    public List<Byte> convert(int start) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public BytecodeList convert() {
        if (retCount == 0) {
            CompilerWarning.warn("Unused lambda", WarningType.UNUSED, info, node);
            return new BytecodeList();
        } else if (retCount > 1) {
            throw CompilerException.format("Lambda literal only returns one value, not %d", node, retCount);
        }
        BytecodeList bytes = new BytecodeList();
        var name = info.lambdaName();
        var fnInfo = new FunctionInfo(name, convertArgs(), info.typesOf(node.getReturns()));
        int fnIndex = info.addFunction(new Function(node, fnInfo, convertBody()));
        bytes.add(Bytecode.MAKE_FUNCTION, fnIndex);
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
    private BytecodeList convertBody() {
        info.addStackFrame();
        var retInfo = info.getFnReturns();
        retInfo.addFunctionReturns(lambdaReturnType());
        for (var arg : node.getArgs()) {
            info.addVariable(arg.getName().getName(), info.getType(arg.getType()), arg);
        }
        var fnBytes = new BytecodeList(BaseConverter.bytes(fnBody(), info ));
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
        var kwargs = getArgs(args.getNameArgs());
        var normalArgs = getArgs(args.getArgs());
        var posArgs = getArgs(args.getPositionArgs());
        return new ArgumentInfo(kwargs, normalArgs, posArgs);
    }

    private Argument[] getArgs(@NotNull TypedArgumentNode... args) {
        var result = new Argument[args.length];
        var expected = (expectedReturns == null || expectedReturns.length == 0) ? null : expectedReturns[0];
        for (int i = 0; i < args.length; i++) {
            var arg = args[i];
            var argType = arg.getType();
            if (argType.isDecided()) {
                result[i] = new Argument(
                        arg.getName().getName(), info.getType(arg.getType()),
                        arg.getVararg(), arg.getLineInfo()
                );
            } else if (expected == null) {
                throw CompilerException.of("Cannot deduce type of lambda argument", arg.getName());
            } else {
                if (expected.sameBaseType(Builtins.callable())) {
                    var generics = expected.getGenerics();
                    assert generics.size() == 2;
                    var expectedArgs = (ListTypeObject) generics.get(0);
                    result[i] = new Argument(
                            arg.getName().getName(), expectedArgs.get(i),
                        arg.getVararg(), arg.getLineInfo()
                    );
                } else {
                    throw CompilerTodoError.of("Cannot deduce lambda types from non-Callable", arg);
                }
            }
        }
        return result;
    }
}
