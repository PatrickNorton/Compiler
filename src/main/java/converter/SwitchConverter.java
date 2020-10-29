package main.java.converter;

import main.java.parser.CaseStatementNode;
import main.java.parser.DefaultStatementNode;
import main.java.parser.DottedVariableNode;
import main.java.parser.Lined;
import main.java.parser.SwitchStatementNode;
import main.java.parser.TestNode;
import main.java.parser.VariableNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        if (!(retType instanceof UnionTypeObject) && incompleteReturn()) {
            throw CompilerException.format("Cannot get return from switch: Not all cases covered", node);
        }
        if (Builtins.INT.isSuperclass(retType)) {
            return convertTbl(start);
        } else if (Builtins.STR.isSuperclass(retType)) {
            return convertStr(start);
        } else if (retType instanceof UnionTypeObject) {
            return convertUnion(start, (UnionTypeObject) retType);
        }
        var switched = converter.convert(start);
        var retTypes = retCount == 0 ? new TypeObject[0]: returnType();
        List<Byte> bytes = new ArrayList<>(switched);
        for (var caseStatement : node.getCases()) {
            addCase(caseStatement, start, bytes, retTypes);
        }
        return bytes;
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        var cases = node.getCases();
        var switchRet = TestConverter.returnType(node.getSwitched(), info, 1)[0];
        TypeObject[][] types = new TypeObject[cases.length][retCount];
        for (int i = 0; i < cases.length; i++) {
            assert cases[i].isArrow();
            var hasAs = !cases[i].getAs().isEmpty();
            if (hasAs) {
                info.addStackFrame();
                if (!(switchRet instanceof UnionTypeObject)) {
                    throw CompilerException.format(
                            "Switch with 'as' clause must be over a union, not '%s'",
                            node.getSwitched(), switchRet.name()
                    );
                }
                var varType = labelToType(cases[i].getLabel()[0], (UnionTypeObject) switchRet);
                info.addVariable(cases[i].getAs().getName(), varType, cases[i].getAs());
            }
            types[i] = TestConverter.returnType((TestNode) cases[i].getBody().get(0), info, retCount);
            if (hasAs) {
                info.removeStackFrame();
            }
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

    private void addCase(@NotNull CaseStatementNode stmt, int start, @NotNull List<Byte> bytes, TypeObject[] retTypes) {
        // TODO: Ensure 'default' statement is at the end
        var label = stmt.getLabel();
        List<Integer> jumpLocations = new ArrayList<>(label.length);
        if (!stmt.getAs().isEmpty()) {
            throw CompilerException.of("'as' clause not allowed here", stmt.getAs());
        }
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
                throw CompilerTodoError.of("Multiple clauses in switch not supported yet", stmt);
            }
        } else {
            bytes.add(Bytecode.POP_TOP.value);
        }
        convertBody(start, bytes, stmt, retTypes);
        bytes.add(Bytecode.JUMP.value);
        info.loopManager().addBreak(1, start + bytes.size());
        bytes.addAll(Util.zeroToBytes());
        var endCase = Util.intToBytes(start + bytes.size());
        for (var jumpLoc : jumpLocations) {
            Util.emplace(bytes, endCase, jumpLoc);
        }
    }

    @NotNull
    private List<Byte> convertTbl(int start) {
        Map<BigInteger, Integer> jumps = new HashMap<>();
        int defaultVal = 0;
        var retTypes = retCount == 0 ? new TypeObject[0]: returnType();
        List<Byte> bytes = tblHeader(start);
        int tblPos = bytes.size();
        bytes.addAll(Util.shortZeroBytes());
        for (var stmt : node.getCases()) {
            if (!stmt.getAs().isEmpty()) {
                throw asException(stmt.getAs());
            }
            if (stmt instanceof DefaultStatementNode) {
                defaultVal = getDefaultVal(start, defaultVal, bytes, stmt);
                continue;
            }
            var label = stmt.getLabel()[0];
            var lblConverter = TestConverter.of(info, stmt.getLabel()[0], 1);
            var constant = lblConverter.constantReturn().orElseThrow(() -> literalException("int", label));
            BigInteger value;
            if (constant instanceof IntConstant) {
                value = BigInteger.valueOf(((IntConstant) constant).getValue());
            } else if (constant instanceof BigintConstant) {
                value = ((BigintConstant) constant).getValue();
            } else {
                throw literalException("int", label);
            }
            if (jumps.containsKey(value)) {
                throw CompilerException.format("Cannot define number %d twice in switch statement", node, value);
            } else {
                jumps.put(value, start + bytes.size());
            }
            convertBody(start, bytes, stmt, retTypes);
            bytes.add(Bytecode.JUMP.value);
            info.loopManager().addBreak(1, start + bytes.size());
            bytes.addAll(Util.zeroToBytes());
        }
        var switchTable = getTbl(jumps, defaultVal == 0 ? start + bytes.size() : defaultVal);
        int tblIndex = info.addSwitchTable(switchTable);
        Util.emplace(bytes, Util.shortToBytes((short) tblIndex), tblPos);
        return bytes;
    }

    @NotNull
    private List<Byte> convertStr(int start) {
        Map<String, Integer> jumps = new HashMap<>();
        int defaultVal = 0;
        var retTypes = retCount == 0 ? new TypeObject[0]: returnType();
        var bytes = tblHeader(start);
        int tblPos = bytes.size();
        bytes.addAll(Util.shortZeroBytes());
        for (var stmt : node.getCases()) {
            if (!stmt.getAs().isEmpty()) {
                throw asException(stmt.getAs());
            }
            if (stmt instanceof DefaultStatementNode) {
                defaultVal = getDefaultVal(start, defaultVal, bytes, stmt);
                continue;
            }
            var label = stmt.getLabel()[0];
            var lblConverter = TestConverter.of(info, label, 1);
            var constant = lblConverter.constantReturn().orElseThrow(() -> literalException("string", label));
            if (constant instanceof StringConstant) {
                var value = ((StringConstant) constant).getValue();
                if (jumps.containsKey(value)) {
                    throw CompilerException.format("Cannot define str \"%s\" twice in switch statement", node, value);
                } else {
                    jumps.put(value, start + bytes.size());
                }
            } else {
                throw literalException("string", label);
            }
            convertBody(start, bytes, stmt, retTypes);
            bytes.add(Bytecode.JUMP.value);
            info.loopManager().addBreak(1, start + bytes.size());
            bytes.addAll(Util.zeroToBytes());
        }
        var switchTable = strTbl(jumps, defaultVal == 0 ? start + bytes.size() : defaultVal);
        int tblIndex = info.addSwitchTable(switchTable);
        Util.emplace(bytes, Util.shortToBytes((short) tblIndex), tblPos);
        return bytes;
    }

    @NotNull
    private List<Byte> tblHeader(int start) {
        List<Byte> bytes = new ArrayList<>(TestConverter.bytes(start, node.getSwitched(), info, 1));
        bytes.add(Bytecode.SWITCH_TABLE.value);
        return bytes;
    }

    private static final BigInteger BIG_MAX = BigInteger.valueOf(Integer.MAX_VALUE);

    @Contract("_, _ -> new")
    @NotNull
    private SwitchTable getTbl(@NotNull Map<BigInteger, Integer> jumps, int defaultVal) {
        var threshold = 2 * (long) jumps.size();
        var max = Collections.max(jumps.keySet());
        if (max.compareTo(BigInteger.valueOf(threshold)) > 0 || max.compareTo(BIG_MAX) > 0) {
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

    @Contract(value = "_, _ -> new", pure = true)
    @NotNull
    private SwitchTable strTbl(Map<String, Integer> jumps, int defaultVal) {
        return new StringSwitchTable(jumps, defaultVal);
    }

    private void convertBody(int start, @NotNull List<Byte> bytes,
                              @NotNull CaseStatementNode stmt, @NotNull TypeObject[] retTypes) {
        if (stmt.isArrow()) {
            convertArrow(start, bytes, stmt, retTypes);
        } else {
            if (retCount > 0) {
                throw CompilerInternalError.of("Statements requiring 'break as' not supported yet", stmt);
            }
            info.addStackFrame();
            bytes.addAll(BaseConverter.bytes(start + bytes.size(), stmt.getBody(), info));
            info.removeStackFrame();
        }
    }

    private void convertArrow(int start, @NotNull List<Byte> bytes,
                              @NotNull CaseStatementNode stmt, @NotNull TypeObject[] retTypes) {
        assert stmt.isArrow();
        var converter = TestConverter.of(info, (TestNode) stmt.getBody().get(0), retCount);
        bytes.addAll(converter.convert(start + bytes.size()));
        var converterRet = converter.returnType();
        for (int i = 0; i < retTypes.length; i++) {
            if (OptionTypeObject.needsMakeOption(retTypes[i], converterRet[i])) {
                addSwap(bytes, retTypes.length - i - 1);
                bytes.add(Bytecode.MAKE_OPTION.value);
                addSwap(bytes, retTypes.length - i - 1);
            }
        }
    }

    private int getDefaultVal(int start, int defaultVal, List<Byte> bytes, CaseStatementNode stmt) {
        if (defaultVal != 0) {
            throw defaultException(stmt);
        }
        return convertDefault(start, bytes, (DefaultStatementNode) stmt);
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
    private List<Byte> convertUnion(int start, UnionTypeObject union) {
        Map<Integer, Integer> jumps = new HashMap<>();
        boolean hasAs = anyHasAs();
        int defaultVal = 0;
        var retTypes = retCount == 0 ? new TypeObject[0]: returnType();
        List<Byte> bytes = new ArrayList<>(TestConverter.bytes(start, node.getSwitched(), info, 1));
        if (hasAs) {
            bytes.add(Bytecode.DUP_TOP.value);
        }
        bytes.add(Bytecode.VARIANT_NO.value);
        bytes.add(Bytecode.SWITCH_TABLE.value);
        int tblPos = bytes.size();
        bytes.addAll(Util.shortZeroBytes());
        boolean hasDefault = false;
        List<Integer> usedVariants = new ArrayList<>();
        for (var stmt : node.getCases()) {
            if (stmt instanceof DefaultStatementNode) {
                assert defaultVal == 0;
                assert stmt.getAs().isEmpty();
                hasDefault = true;
                if (hasAs) {
                    bytes.add(Bytecode.POP_TOP.value);
                }
                defaultVal = convertDefault(start, bytes, (DefaultStatementNode) stmt);
                continue;
            }
            var lblConverter = stmt.getLabel()[0];
            var lblNo = labelToVariantNo(lblConverter, union);
            usedVariants.add(lblNo);
            jumps.put(lblNo, start + bytes.size());
            if (!stmt.getAs().isEmpty()) {
                var as = stmt.getAs();
                bytes.add(Bytecode.GET_VARIANT.value);
                bytes.addAll(Util.shortToBytes((short) lblNo));
                bytes.add(Bytecode.UNWRAP_OPTION.value);
                info.addStackFrame();
                info.addVariable(as.getName(), labelToType(lblConverter, union), as);
                bytes.add(Bytecode.STORE.value);
                bytes.addAll(Util.shortToBytes(info.varIndex(as)));
            } else if (hasAs) {
                bytes.add(Bytecode.POP_TOP.value);
            }
            convertBody(start, bytes, stmt, retTypes);
            bytes.add(Bytecode.JUMP.value);
            info.loopManager().addBreak(1, start + bytes.size());
            bytes.addAll(Util.zeroToBytes());
            if (!stmt.getAs().isEmpty()) {
                info.removeStackFrame();
            }
        }
        if (!hasDefault) {
            var missingUnion = incompleteUnion(union, usedVariants);
            if (missingUnion.isPresent()) {
                var missingVariants = String.join(", ", missingUnion.orElseThrow());
                throw CompilerException.format(
                        "Cannot get return type of switch: Missing union variants %s",
                        node, missingVariants
                );
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

    private int labelToVariantNo(TestNode label, UnionTypeObject switchedType) {
        if (label instanceof DottedVariableNode) {
            var dottedLbl = (DottedVariableNode) label;
            var lblFirst = dottedLbl.getPreDot();
            var lblSecond = dottedLbl.getPostDots();
            var firstConverter = TestConverter.of(info, lblFirst, 1);
            var firstRetType = firstConverter.returnType()[0];
            if (firstRetType instanceof TypeTypeObject && lblSecond.length == 1) {
                var firstType = (TypeTypeObject) firstRetType;
                var retType = firstType.representedType();
                if (retType.sameBaseType(switchedType)) {
                    if (lblSecond[0].getPostDot() instanceof VariableNode && lblSecond[0].getDotPrefix().isEmpty()) {
                        var name = ((VariableNode) lblSecond[0].getPostDot()).getName();
                        return switchedType.getVariantNumber(name).orElseThrow(() -> CompilerException.format(
                                "Invalid name for union variant: %s", label, name
                        ));
                    }
                } else {
                    throw CompilerException.format(
                            "Mismatched types in label: label has type '%s', switched on type '%s'",
                            label, retType.name(), switchedType.name()
                    );
                }
            }
        }
        throw CompilerException.of("Switch on a union must have properly-formed variants", label);
    }

    @NotNull
    private TypeObject labelToType(@NotNull TestNode label, UnionTypeObject switchedType) {
        if (label instanceof DottedVariableNode) {
            var dottedLbl = (DottedVariableNode) label;
            var lblFirst = dottedLbl.getPreDot();
            var lblSecond = dottedLbl.getPostDots();
            var firstConverter = TestConverter.of(info, lblFirst, 1);
            var firstRetType = firstConverter.returnType()[0];
            if (firstRetType instanceof TypeTypeObject && lblSecond.length == 1) {
                var firstType = (TypeTypeObject) firstRetType;
                var retType = firstType.representedType();
                if (retType.sameBaseType(switchedType)) {
                    var lblName = ((VariableNode) lblSecond[0].getPostDot()).getName();
                    return switchedType.variantType(lblName).orElseThrow(
                            () -> CompilerException.format(
                                    "Invalid variant name in union '%s': '%s",
                                    label, switchedType.name(), lblName
                            )
                    );
                } else {
                    throw CompilerException.format(
                            "Mismatched types in label: Switched on union '%s', label has type '%s'",
                            label, switchedType.name(), retType.name()
                    );
                }
            }
        }
        throw CompilerException.of("Switch on a union must have properly-formed variants", label);
    }

    private Optional<String[]> incompleteUnion(UnionTypeObject obj, List<Integer> variants) {
        List<Boolean> containsVariant = new ArrayList<>(Collections.nCopies(obj.variantCount(), false));
        if (retCount > 0) {
            for (var variantNo : variants) {
                containsVariant.set(variantNo, true);
            }
            if (!containsVariant.contains(false)) {
                return Optional.empty();
            } else {
                List<String> result = new ArrayList<>();
                for (int i = 0; i < containsVariant.size(); i++) {
                    if (!containsVariant.get(i)) {
                        result.add(obj.variantName(i).orElseThrow());
                    }
                }
                return Optional.of(result.toArray(new String[0]));
            }
        } else {
            return Optional.empty();
        }
    }

    private boolean incompleteReturn() {  // TODO: Unions with all cases covered
        if (retCount > 0) {
            for (var stmt : node.getCases()) {
                if (stmt instanceof DefaultStatementNode) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
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

    private void addSwap(List<Byte> bytes, int distFromTop) {
        switch (distFromTop) {
            case 0:
                return;
            case 1:
                bytes.add(Bytecode.SWAP_2.value);
            default:
                bytes.add(Bytecode.SWAP_STACK.value);
                bytes.addAll(Util.shortZeroBytes());
                bytes.addAll(Util.shortToBytes((short) distFromTop));
        }
    }

    @NotNull
    private static CompilerException literalException(String literalType, TestNode label) {
        return CompilerException.format(
                "'switch' on a %1$s requires a %1$s literal in each case statement",
                label, literalType
        );
    }

    private static CompilerException defaultException(Lined stmt) {
        throw CompilerException.of("Cannot have more than one 'default' statement in a switch", stmt);
    }

    private static CompilerException asException(Lined as) {
        throw CompilerException.of(
                "'as' clauses in a switch are only allowed when the switched value is a union", as
        );
    }
}
