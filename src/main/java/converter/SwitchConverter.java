package main.java.converter;

import main.java.parser.CaseStatementNode;
import main.java.parser.DefaultStatementNode;
import main.java.parser.DottedVariableNode;
import main.java.parser.SwitchStatementNode;
import main.java.parser.TestNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SwitchConverter extends LoopConverter implements TestConverter {
    private final SwitchStatementNode node;
    private final int retCount;  // TODO: Make switch expressions work

    public SwitchConverter(CompilerInfo info, SwitchStatementNode node, int retCount) {
        super(info, false);
        this.node = node;
        this.retCount = retCount;
    }

    @NotNull
    public List<Byte> trueConvert(int start) {
        var converter = TestConverter.of(info, node.getSwitched(), 1);
        var retType = converter.returnType()[0];
        if (Builtins.INT.isSuperclass(retType)) {
            return convertTbl(start, retType);
        }
        var switched = converter.convert(start);
        List<Byte> bytes = new ArrayList<>(switched);
        for (var caseStatement : node.getCases()) {
            addCase(caseStatement, start, bytes);
        }
        return bytes;
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        var cases = node.getCases();
        TypeObject[][] types = new TypeObject[cases.length][retCount];
        for (int i = 0; i < cases.length; i++) {
            assert cases[i].isArrow();
            types[i] = TestConverter.returnType((TestNode) cases[i].getBody().get(0), info, retCount);
        }
        TypeObject[] finalTypes = new TypeObject[retCount];
        for (int i = 0; i < retCount; i++) {
            TypeObject[] posArray = new TypeObject[cases.length];
            for (int j = 0; j < cases.length; j++) {
                posArray[j] = types[j][i];
            }
            finalTypes[i] = TypeObject.union(posArray);
        }
        return finalTypes;
    }

    private void addCase(@NotNull CaseStatementNode stmt, int start, @NotNull List<Byte> bytes) {
        // TODO: Ensure 'default' statement is at the end
        var label = stmt.getLabel();
        List<Integer> jumpLocations = new ArrayList<>(label.length);
        if (!(stmt instanceof DefaultStatementNode)) {
            assert label.length != 0;
            if (label.length == 1) {
                bytes.add(Bytecode.DUP_TOP.value);
                bytes.addAll(TestConverter.bytes(start + bytes.size(), label[0], info, 1));
                bytes.add(Bytecode.EQUAL.value);
                bytes.add(Bytecode.JUMP_FALSE.value);
                jumpLocations.add(bytes.size());
                bytes.addAll(Util.zeroToBytes());
                bytes.add(Bytecode.POP_TOP.value);
            } else {
                throw new UnsupportedOperationException("Multiple clauses in switch not supported yet");
            }
        } else {
            bytes.add(Bytecode.POP_TOP.value);
        }
        if (stmt.isArrow()) {
            bytes.addAll(TestConverter.bytes(start + bytes.size(), (TestNode) stmt.getBody().get(0), info, retCount));
        } else {
            if (retCount > 0) {
                throw new UnsupportedOperationException("Statements requiring 'break as' not supported yet");
            }
            bytes.addAll(BaseConverter.bytes(start + bytes.size(), stmt.getBody(), info));
        }
        bytes.add(Bytecode.JUMP.value);
        info.loopManager().addBreak(1, start + bytes.size());
        bytes.addAll(Util.zeroToBytes());
        var endCase = Util.intToBytes(start + bytes.size());
        for (var jumpLoc : jumpLocations) {
            Util.emplace(bytes, endCase, jumpLoc);
        }
    }

    @NotNull
    private List<Byte> convertTbl(int start, TypeObject retType) {
        Map<BigInteger, Integer> jumps = new HashMap<>();
        int defaultVal = 0;
        List<Byte> bytes = new ArrayList<>(TestConverter.bytes(start, node.getSwitched(), info, 1));
        bytes.add(Bytecode.SWITCH_TABLE.value);
        int tblPos = bytes.size();
        bytes.addAll(Util.shortZeroBytes());
        for (var stmt : node.getCases()) {
            if (!stmt.getAs().isEmpty()) {
                throw CompilerException.of(
                        "'as' clauses in a switch are only allowed when the switched value is a union", stmt.getAs()
                );
            }
            if (stmt instanceof DefaultStatementNode) {
                assert defaultVal == 0;
                defaultVal = convertDefault(start, bytes, (DefaultStatementNode) stmt);
                continue;
            }
            var lblConverter = TestConverter.of(info, stmt.getLabel()[0], 1);
            if (!lblConverter.returnType()[0].isSuperclass(retType)) {
                CompilerWarning.warnf(
                        "Switch statement has argument of type '%s', " +
                                "but case statement has type '%s', which will never be taken",
                        stmt, retType.name(), lblConverter.returnType()[0].name()
                );
            }
            assert lblConverter instanceof ConstantConverter;  // TODO: Variable switch arguments
            var constant = ((ConstantConverter) lblConverter).constant();
            if (constant instanceof IntConstant) {
                jumps.put(BigInteger.valueOf(((IntConstant) constant).getValue()), start + bytes.size());
            } else if (constant instanceof BigintConstant) {
                jumps.put(((BigintConstant) constant).getValue(), start + bytes.size());
            } else {
                throw new UnsupportedOperationException();
            }
            bytes.addAll(BaseConverter.bytes(start + bytes.size(), stmt.getBody(), info));
            bytes.add(Bytecode.JUMP.value);
            info.loopManager().addBreak(1, start + bytes.size());
            bytes.addAll(Util.zeroToBytes());
        }
        var switchTable = getTbl(jumps, defaultVal == 0 ? start + bytes.size() : defaultVal);
        int tblIndex = info.addSwitchTable(switchTable);
        Util.emplace(bytes, Util.shortToBytes((short) tblIndex), tblPos);
        return bytes;
    }

    @Contract("_, _ -> new")
    @NotNull
    private SwitchTable getTbl(@NotNull Map<BigInteger, Integer> jumps, int defaultVal) {
        var threshold = 2 * (long) jumps.size();
        var max = Collections.max(jumps.keySet());
        if (max.compareTo(BigInteger.valueOf(threshold)) > 0
                || max.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) >= 0) {
            return new BigSwitchTable(jumps, defaultVal);
        } else {
            var tblSize = max.intValueExact() + 1;
            List<Integer> table = new ArrayList<>(tblSize);
            for (int i = 0; i < tblSize; i++) {
                table.add(jumps.getOrDefault(BigInteger.valueOf(i), defaultVal));
            }
            return new CompactSwitchTable(table, defaultVal);
        }
    }

    private int convertDefault(int start, @NotNull List<Byte> bytes, @NotNull DefaultStatementNode stmt) {
        var defaultVal = start + bytes.size();
        bytes.addAll(BaseConverter.bytes(start + bytes.size(), stmt.getBody(), info));
        bytes.add(Bytecode.JUMP.value);
        info.loopManager().addBreak(1, start + bytes.size());
        bytes.addAll(Util.zeroToBytes());
        return defaultVal;
    }

    @NotNull
    private List<Byte> convertUnion(int start, StdTypeObject union) {
        Map<Integer, Integer> jumps = new HashMap<>();
        boolean hasAs = anyHasAs();
        int defaultVal = 0;
        List<Byte> bytes = new ArrayList<>(TestConverter.bytes(start, node.getSwitched(), info, 1));
        if (hasAs) {
            bytes.add(Bytecode.DUP_TOP.value);
        }
        bytes.add(Bytecode.VARIANT_NO.value);
        bytes.add(Bytecode.SWITCH_TABLE.value);
        int tblPos = bytes.size();
        bytes.addAll(Util.shortZeroBytes());
        for (var stmt : node.getCases()) {
            if (stmt instanceof DefaultStatementNode) {
                assert defaultVal == 0;
                assert stmt.getAs().isEmpty();
                if (hasAs) {
                    bytes.add(Bytecode.POP_TOP.value);
                }
                defaultVal = convertDefault(start, bytes, (DefaultStatementNode) stmt);
                continue;
            }
            var lblConverter = stmt.getLabel()[0];
            var lblNo = labelToVariantNo(lblConverter, union);
            jumps.put(lblNo, start + bytes.size());
            if (!stmt.getAs().isEmpty()) {
                var as = stmt.getAs();
                bytes.add(Bytecode.GET_VARIANT.value);
                bytes.addAll(Util.shortToBytes((short) lblNo));
                bytes.add(Bytecode.UNWRAP_OPTION.value);
                info.addStackFrame();
                info.addVariable(as.getName(), labelToType(lblConverter, union), as);
                bytes.add(Bytecode.STORE.value);
                bytes.addAll(Util.shortToBytes(info.varIndex(as.getName())));
            } else if (hasAs) {
                bytes.add(Bytecode.POP_TOP.value);
            }
            bytes.addAll(BaseConverter.bytes(start + bytes.size(), stmt.getBody(), info));
            bytes.add(Bytecode.JUMP.value);
            info.loopManager().addBreak(1, start + bytes.size());
            bytes.addAll(Util.zeroToBytes());
            if (!stmt.getAs().isEmpty()) {
                info.removeStackFrame();
            }
        }
        var switchTable = smallTbl(jumps, defaultVal == 0 ? start + bytes.size() : defaultVal);
        int tblIndex = info.addSwitchTable(switchTable);
        Util.emplace(bytes, Util.shortToBytes((short) tblIndex), tblPos);
        return bytes;
    }

    private boolean anyHasAs() {
        for (var stmt : node.getCases()) {
            if (!stmt.getAs().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private int labelToVariantNo(TestNode label, StdTypeObject switchedType) {
        if (label instanceof DottedVariableNode) {
            var dottedLbl = (DottedVariableNode) label;
            var lblFirst = dottedLbl.getPreDot();
            var lblSecond = dottedLbl.getPostDots();
            var firstConverter = TestConverter.of(info, lblFirst, 1);
            var firstRetType = firstConverter.returnType()[0];
            if (firstRetType instanceof TypeTypeObject && lblSecond.length == 1) {
                var firstType = (TypeTypeObject) firstRetType;
                var retType = firstType.representedType();
                if (retType.equals(switchedType)) {
                    // TODO: Get label
                    return 0;
                } else {
                    throw CompilerException.of("Mismatched types in label", label);
                }
            }
        }
        throw CompilerException.of("Switch on a union must have properly-formed variants", label);
    }

    @NotNull
    private TypeObject labelToType(@NotNull TestNode label, StdTypeObject switchedType) {
        if (label instanceof DottedVariableNode) {
            var dottedLbl = (DottedVariableNode) label;
            var lblFirst = dottedLbl.getPreDot();
            var lblSecond = dottedLbl.getPostDots();
            var firstConverter = TestConverter.of(info, lblFirst, 1);
            var firstRetType = firstConverter.returnType()[0];
            if (firstRetType instanceof TypeTypeObject && lblSecond.length == 1) {
                var firstType = (TypeTypeObject) firstRetType;
                var retType = firstType.representedType();
                if (retType.equals(switchedType)) {
                    return retType;
                } else {
                    throw CompilerException.of("Mismatched types in label", label);
                }
            }
        }
        throw CompilerException.of("Switch on a union must have properly-formed variants", label);
    }

    @NotNull
    @Contract("_, _ -> new")
    private SwitchTable smallTbl(@NotNull Map<Integer, Integer> jumps, int defaultVal) {
        var max = Collections.max(jumps.keySet());
        var tblSize = max + 1;
        List<Integer> table = new ArrayList<>(tblSize);
        for (int i = 0; i < tblSize; i++) {
            table.add(jumps.getOrDefault(i, defaultVal));
        }
        return new CompactSwitchTable(table, defaultVal);
    }
}
