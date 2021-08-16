package main.java.converter;

import main.java.parser.CaseStatementNode;
import main.java.parser.DefaultStatementNode;
import main.java.parser.DottedVariableNode;
import main.java.parser.Lined;
import main.java.parser.SwitchStatementNode;
import main.java.parser.TestNode;
import main.java.parser.VariableNode;
import main.java.util.Pair;
import main.java.util.StringEscape;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class SwitchConverter extends LoopConverter implements TestConverter {
    private final SwitchStatementNode node;
    private final int retCount;

    public SwitchConverter(CompilerInfo info, SwitchStatementNode node, int retCount) {
        super(info, false);
        this.node = node;
        this.retCount = retCount;
    }

    @NotNull
    public List<Byte> trueConvert(int start) {
        return trueConvertWithReturn(start).getKey();
    }

    @Override
    protected Pair<List<Byte>, DivergingInfo> trueConvertWithReturn(int start) {
        var converter = TestConverter.of(info, node.getSwitched(), 1);
        var retType = converter.returnType()[0];
        if (!(retType instanceof UnionTypeObject) && incompleteReturn()) {
            throw CompilerException.format("Cannot get return from switch: Missing 'default' statement", node);
        }
        if (Builtins.intType().isSuperclass(retType)) {
            return convertInt(start);
        } else if (Builtins.str().isSuperclass(retType)) {
            return convertStr(start);
        } else if (Builtins.charType().isSuperclass(retType)) {
            return convertChar(start);
        } else if (retType instanceof UnionTypeObject) {
            return convertUnion(start, (UnionTypeObject) retType);
        }
        var switched = converter.convert(start);
        var retTypes = retCount == 0 ? new TypeObject[0] : returnType();
        boolean hadDefault = false;
        DivergingInfo willReturn = null;
        List<Byte> bytes = new ArrayList<>(switched);
        for (var caseStatement : node.getCases()) {
            if (caseStatement instanceof DefaultStatementNode) {
                hadDefault = true;
            } else if (hadDefault) {
                CompilerWarning.warn(
                        "Default statement before case statement in switch\n" +
                                "Help: In unoptimized switch statements, cases are run through in order" +
                                " and thus this is unreachable",
                        WarningType.UNREACHABLE, info, caseStatement
                );
            }
            willReturn = andWith(willReturn, addCase(caseStatement, start, bytes, retTypes));
        }
        if (willReturn == null) {
            willReturn = new DivergingInfo();
        }
        if (!hadDefault) {
            willReturn.makeUncertain();
        }
        return Pair.of(bytes, willReturn);
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        var cases = node.getCases();
        var types = getUnmergedTypes();
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

    private TypeObject[][] getUnmergedTypes() {
        var cases = node.getCases();
        var switchRet = TestConverter.returnType(node.getSwitched(), info, 1)[0];
        TypeObject[][] types = new TypeObject[cases.length][retCount];
        for (int i = 0; i < cases.length; i++) {
            if (!cases[i].isArrow()) {
                throw CompilerException.of("Switch with returns must be entirely arrows", cases[i]);
            }
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
        return types;
    }

    private DivergingInfo addCase(
            @NotNull CaseStatementNode stmt, int start, @NotNull List<Byte> bytes, TypeObject[] retTypes
    ) {
        // TODO: Ensure 'default' statement is at the end
        var label = stmt.getLabel();
        List<Integer> jumpLocations = new ArrayList<>(label.length);
        if (!stmt.getAs().isEmpty()) {
            throw CompilerException.of("'as' clause not allowed here", stmt.getAs());
        }
        if (!(stmt instanceof DefaultStatementNode)) {
            assert label.length != 0;
            List<Integer> tmpJumps = new ArrayList<>(label.length - 1);
            for (int i = 0; i < label.length; i++) {
                bytes.add(Bytecode.DUP_TOP.value);
                bytes.addAll(TestConverter.bytes(start + bytes.size(), label[i], info, 1));
                bytes.add(Bytecode.EQUAL.value);
                bytes.add(Bytecode.JUMP_FALSE.value);
                jumpLocations.add(bytes.size());
                bytes.addAll(Util.zeroToBytes());
                if (i != label.length - 1) {
                    bytes.add(Bytecode.JUMP_TRUE.value);
                    tmpJumps.add(bytes.size());
                    bytes.addAll(Util.zeroToBytes());
                }
            }
            for (var jump : tmpJumps) {
                Util.emplace(bytes, Util.intToBytes(start + bytes.size()), jump);
            }
        }
        bytes.add(Bytecode.POP_TOP.value);
        var willReturn = convertBody(start, bytes, stmt, retTypes);
        bytes.add(Bytecode.JUMP.value);
        info.loopManager().addBreak(1, start + bytes.size());
        bytes.addAll(Util.zeroToBytes());
        var endCase = Util.intToBytes(start + bytes.size());
        for (var jumpLoc : jumpLocations) {
            Util.emplace(bytes, endCase, jumpLoc);
        }
        return willReturn;
    }

    private <T> Pair<List<Byte>, DivergingInfo> convertTbl(
            int start, BiFunction<TestConverter, Lined, T> addToMap,
            Function<T, String> errorEscape,
            BiFunction<Map<T, Integer>, Integer, SwitchTable> createTable
    ) {
        Map<T, Integer> jumps = new HashMap<>();
        int defaultVal = 0;
        DivergingInfo willReturn = null;
        var retTypes = retCount == 0 ? new TypeObject[0] : returnType();
        List<Byte> bytes = tblHeader(start);
        int tblPos = bytes.size();
        bytes.addAll(Util.shortZeroBytes());
        for (var stmt : node.getCases()) {
            if (!stmt.getAs().isEmpty()) {
                throw asException(stmt.getAs());
            }
            if (stmt instanceof DefaultStatementNode) {
                var pair = getDefaultVal(start, defaultVal, bytes, stmt, retTypes);
                defaultVal = pair.getKey();
                willReturn = andWith(willReturn, pair.getValue());
                continue;
            }
            if (stmt.getLabel().length == 0) {
                throw emptyLabelException(stmt);
            }
            for (var label : stmt.getLabel()) {
                var lblConverter = TestConverter.of(info, label, 1);
                var value = addToMap.apply(lblConverter, label);
                if (!jumps.containsKey(value)) {
                    jumps.put(value, start + bytes.size());
                } else {
                    throw CompilerException.format(
                            "Cannot define %s twice in switch statement", node, errorEscape.apply(value)
                    );
                }
            }
            willReturn = andWith(willReturn, convertBody(start, bytes, stmt, retTypes));
            bytes.add(Bytecode.JUMP.value);
            info.loopManager().addBreak(1, start + bytes.size());
            bytes.addAll(Util.zeroToBytes());
        }
        if (willReturn == null) {
            willReturn = new DivergingInfo();
        }
        if (defaultVal == 0) {
            willReturn.makeUncertain();
        }
        var switchTable = createTable.apply(jumps, defaultVal == 0 ? start + bytes.size() : defaultVal);
        int tblIndex = info.addSwitchTable(switchTable);
        Util.emplace(bytes, Util.shortToBytes((short) tblIndex), tblPos);
        return Pair.of(bytes, willReturn);
    }

    private Pair<List<Byte>, DivergingInfo> convertInt(int start) {
        BiFunction<TestConverter, Lined, BigInteger> addToMap = (lblConverter, label) -> {
            var constant = lblConverter.constantReturn().orElseThrow(() -> literalException("int", label));
            return IntArithmetic.convertConst(constant).orElseThrow(() -> literalException("int", label));
        };
        return convertTbl(start, addToMap, BigInteger::toString, this::getTbl);
    }

    private Pair<List<Byte>, DivergingInfo> convertStr(int start) {
        BiFunction<TestConverter, Lined, String> addToMap = (lblConverter, label) -> {
            var constant = lblConverter.constantReturn().orElseThrow(() -> literalException("string", label));
            if (constant instanceof StringConstant) {
                return ((StringConstant) constant).getValue();
            } else {
                throw literalException("string", label);
            }
        };
        return convertTbl(start, addToMap, StringEscape::escape, this::strTbl);
    }

    private Pair<List<Byte>, DivergingInfo> convertChar(int start) {
        BiFunction<TestConverter, Lined, Integer> addToMap = (lblConverter, label) -> {
            var constant = lblConverter.constantReturn().orElseThrow(() -> literalException("char", label));
            if (constant instanceof CharConstant) {
                return ((CharConstant) constant).getValue();
            } else if (constant instanceof StringConstant) {
                throw literalException("char", label, "Try prefixing the literal with 'c'");
            } else {
                throw literalException("char", label);
            }
        };
        return convertTbl(start, addToMap, CharConstant::name, this::charTbl);
    }

    @NotNull
    private List<Byte> tblHeader(int start) {
        var converter = TestConverter.of(info, node.getSwitched(), 1);
        var constant = converter.constantReturn();
        if (constant.isPresent() && constant.orElseThrow().strValue().isPresent()) {
            CompilerWarning.warnf(
                    "Switch conditional always evaluates to %s",
                    WarningType.TRIVIAL_VALUE, info, node.getSwitched(),
                    constant.orElseThrow().strValue().orElseThrow()
            );
        }
        List<Byte> bytes = new ArrayList<>(converter.convert(start));
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

    private SwitchTable charTbl(Map<Integer, Integer> jumps, int defaultVal) {
        return new CharSwitchTable(jumps, defaultVal);
    }

    @Contract(value = "_, _ -> new", pure = true)
    @NotNull
    private SwitchTable strTbl(Map<String, Integer> jumps, int defaultVal) {
        return new StringSwitchTable(jumps, defaultVal);
    }

    private DivergingInfo convertBody(int start, @NotNull List<Byte> bytes,
                              @NotNull CaseStatementNode stmt, @NotNull TypeObject[] retTypes) {
        if (stmt.isArrow()) {
            return convertArrow(start, bytes, stmt, retTypes);
        } else {
            if (retCount > 0) {
                throw CompilerInternalError.of("Statements requiring 'break as' not supported yet", stmt);
            }
            info.addStackFrame();
            var pair = BaseConverter.bytesWithReturn(start + bytes.size(), stmt.getBody(), info);
            bytes.addAll(pair.getKey());
            info.removeStackFrame();
            return pair.getValue();
        }
    }

    private DivergingInfo convertArrow(int start, @NotNull List<Byte> bytes,
                              @NotNull CaseStatementNode stmt, @NotNull TypeObject[] retTypes) {
        assert stmt.isArrow();
        var converter = TestConverter.of(info, (TestNode) stmt.getBody().get(0), retCount);
        var pair = converter.convertAndReturn(start);
        bytes.addAll(pair.getKey());
        var converterRet = converter.returnType();
        for (int i = 0; i < retTypes.length; i++) {
            if (OptionTypeObject.needsMakeOption(retTypes[i], converterRet[i])) {
                addSwap(bytes, retTypes.length - i - 1);
                bytes.add(Bytecode.MAKE_OPTION.value);
                addSwap(bytes, retTypes.length - i - 1);
            }
        }
        return pair.getValue();
    }

    private Pair<Integer, DivergingInfo> getDefaultVal(int start, int defaultVal, List<Byte> bytes,
                              CaseStatementNode stmt, TypeObject[] retTypes) {
        if (defaultVal != 0) {
            throw defaultException(stmt);
        }
        return convertDefault(start, bytes, (DefaultStatementNode) stmt, retTypes);
    }

    private  Pair<Integer, DivergingInfo> convertDefault(int start, @NotNull List<Byte> bytes,
                               @NotNull DefaultStatementNode stmt, TypeObject[] retTypes) {
        var defaultVal = start + bytes.size();
        DivergingInfo willReturn;
        if (stmt.isArrow()) {
            willReturn = convertArrow(start, bytes, stmt, retTypes);
        } else {
            var pair = BaseConverter.bytesWithReturn(start + bytes.size(), stmt.getBody(), info);
            bytes.addAll(pair.getKey());
            willReturn = pair.getValue();
        }
        bytes.add(Bytecode.JUMP.value);
        info.loopManager().addBreak(1, start + bytes.size());
        bytes.addAll(Util.zeroToBytes());
        return Pair.of(defaultVal, willReturn);
    }

    @NotNull
    private Pair<List<Byte>, DivergingInfo> convertUnion(int start, UnionTypeObject union) {
        Map<Integer, Integer> jumps = new HashMap<>();
        boolean hasAs = anyHasAs();
        int defaultVal = 0;
        DivergingInfo willReturn = null;
        var retTypes = retCount == 0 ? new TypeObject[0] : returnType();
        List<Byte> bytes = new ArrayList<>(TestConverter.bytes(start, node.getSwitched(), info, 1));
        if (hasAs) {
            bytes.add(Bytecode.DUP_TOP.value);
        }
        bytes.add(Bytecode.VARIANT_NO.value);
        bytes.add(Bytecode.SWITCH_TABLE.value);
        int tblPos = bytes.size();
        bytes.addAll(Util.shortZeroBytes());
        boolean hasDefault = false;
        Set<Integer> usedVariants = new HashSet<>();
        for (var stmt : node.getCases()) {
            if (stmt instanceof DefaultStatementNode) {
                if (defaultVal != 0) {
                    throw defaultException(stmt);
                }
                assert stmt.getAs().isEmpty();
                hasDefault = true;
                if (hasAs) {
                    bytes.add(Bytecode.POP_TOP.value);
                }
                var pair = convertDefault(start, bytes, (DefaultStatementNode) stmt, retTypes);
                defaultVal = pair.getKey();
                willReturn = andWith(willReturn, pair.getValue());
                continue;
            }
            var stmtHasAs = !stmt.getAs().isEmpty();
            if (stmt.getLabel().length == 0) {
                throw emptyLabelException(stmt);
            } else if (stmt.getLabel().length > 1 && stmtHasAs) {
                // TODO? As clause with multiple labels of same type
                throw CompilerException.of("Cannot use 'as' clause with more than one label", stmt);
            }
            for (var label : stmt.getLabel()) {
                var lblNo = labelToVariantNo(label, union);
                if (usedVariants.contains(lblNo)) {
                    var name = union.variantName(lblNo).orElseThrow();
                    throw CompilerException.format(
                            "Variant %s defined twice in switch statement", stmt, name
                    );
                }
                usedVariants.add(lblNo);
                jumps.put(lblNo, start + bytes.size());
                if (stmtHasAs) {  // Will work b/c there must only be one label if there is an 'as' clause
                    assert stmt.getLabel().length == 1;
                    var as = stmt.getAs();
                    bytes.add(Bytecode.GET_VARIANT.value);
                    bytes.addAll(Util.shortToBytes((short) lblNo));
                    bytes.add(Bytecode.UNWRAP_OPTION.value);
                    info.addStackFrame();
                    info.addVariable(as.getName(), labelToType(label, union), as);
                    bytes.add(Bytecode.STORE.value);
                    bytes.addAll(Util.shortToBytes(info.varIndex(as)));
                }
            }
            if (hasAs && !stmtHasAs) {
                bytes.add(Bytecode.POP_TOP.value);
            }
            willReturn = andWith(willReturn, convertBody(start, bytes, stmt, retTypes));
            bytes.add(Bytecode.JUMP.value);
            info.loopManager().addBreak(1, start + bytes.size());
            bytes.addAll(Util.zeroToBytes());
            if (!stmt.getAs().isEmpty()) {
                info.removeStackFrame();
            }
        }
        if (willReturn == null) {
            willReturn = new DivergingInfo();
        }
        if (!hasDefault && retCount > 0) {
            var missingUnion = incompleteUnion(union, usedVariants);
            if (missingUnion.isPresent()) {
                var missing = missingUnion.orElseThrow();
                var missingVariants = String.join(", ", missing);
                throw CompilerException.format(
                        "Cannot get return type of switch: Missing union variant%s %s",
                        node, missing.length == 1 ? "" : "s", missingVariants
                );
            }
        } else if (hasDefault) {
            if (incompleteUnion(union, usedVariants).isEmpty()) {
                var defaultInfo = defaultStmt().orElseThrow(
                        () -> CompilerInternalError.of(
                                "defaultVal is true but no default statement was found", node
                        )
                );
                CompilerWarning.warn(
                        "Default statement in switch with all variants covered",
                        WarningType.UNREACHABLE, info, defaultInfo
                );
            }
        } else {
            if (incompleteUnion(union, usedVariants).isPresent()) {
                willReturn.makeUncertain();
            }
        }
        var switchTable = smallTbl(jumps, defaultVal == 0 ? start + bytes.size() : defaultVal);
        int tblIndex = info.addSwitchTable(switchTable);
        Util.emplace(bytes, Util.shortToBytes((short) tblIndex), tblPos);
        return Pair.of(bytes, willReturn);
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

    private Optional<String[]> incompleteUnion(UnionTypeObject obj, Set<Integer> variants) {
        List<Boolean> containsVariant = new ArrayList<>(Collections.nCopies(obj.variantCount(), false));
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
    }

    private boolean incompleteReturn() {
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
                return;
            default:
                bytes.add(Bytecode.SWAP_STACK.value);
                bytes.addAll(Util.shortZeroBytes());
                bytes.addAll(Util.shortToBytes((short) distFromTop));
        }
    }

    private Optional<Lined> defaultStmt() {
        for (var stmt : node.getCases()) {
            if (stmt instanceof DefaultStatementNode) {
                return Optional.of(stmt);
            }
        }
        return Optional.empty();
    }

    @NotNull
    private static CompilerException literalException(String literalType, Lined label) {
        return CompilerException.format(
                "'switch' on a %1$s requires a %1$s literal in each case statement",
                label, literalType
        );
    }

    @Contract("_, _, _ -> new")
    @NotNull
    private static CompilerException literalException(String literalType, Lined label, String note) {
        return CompilerException.format(
                "'switch' on a %1$s requires a %1$s literal in each case statement\n" +
                        "Note: %s",
                label, literalType, note
        );
    }

    private static CompilerException defaultException(Lined stmt) {
        return CompilerException.of("Cannot have more than one 'default' statement in a switch", stmt);
    }

    private static CompilerException asException(Lined as) {
        return CompilerException.of(
                "'as' clauses in a switch are only allowed when the switched value is a union", as
        );
    }

    private static CompilerException emptyLabelException(Lined stmt) {
        return CompilerException.format("Case statements must have at least one label", stmt);
    }

    private static DivergingInfo andWith(@Nullable DivergingInfo info1, DivergingInfo info2) {
        if (info1 != null) {
            info1.andWith(info2);
            return info1;
        } else {
            return info2;
        }
    }
}
