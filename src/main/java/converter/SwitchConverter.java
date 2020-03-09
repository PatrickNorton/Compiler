package main.java.converter;

import main.java.parser.CaseStatementNode;
import main.java.parser.SwitchStatementNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class SwitchConverter extends LoopConverter implements TestConverter {
    private SwitchStatementNode node;
    private Deque<Deque<Integer>> locations;
    private int retCount;  // TODO: Make switch expressions work

    public SwitchConverter(CompilerInfo info, SwitchStatementNode node, int retCount) {
        super(info, false);
        this.node = node;
        this.retCount = retCount;
        this.locations = new ArrayDeque<>();
    }

    @NotNull
    public List<Byte> trueConvert(int start) {
        assert locations.isEmpty();
        var switched = TestConverter.bytes(start, node.getSwitched(), info, 1);
        List<Byte> bytes = new ArrayList<>(switched);  // FIXME: Check that all values are equal
        for (var caseStatement : node.getCases()) {
            addCaseCond(caseStatement, start, bytes);
        }
        for (var caseStatement : node.getCases()) {
            addCase(caseStatement, start, bytes);
        }
        assert locations.isEmpty();
        return bytes;
    }

    @NotNull
    @Override
    public TypeObject[] returnType() {
        var cases = node.getCases();
        TypeObject[] types = new TypeObject[cases.length];
        for (int i = 0; i < cases.length; i++) {
            types[i] = null;  // TODO: Get actual types
        }
        return new TypeObject[] {TypeObject.union(types)};
    }

    private void addCaseCond(@NotNull CaseStatementNode stmt, int start, @NotNull List<Byte> bytes) {
        Deque<Integer> jumpLocations = new ArrayDeque<>();
        for (var label : stmt.getLabel()) {
            bytes.addAll(TestConverter.bytes(start + bytes.size(), label, info, 1));
            bytes.add(Bytecode.JUMP_FALSE.value);
            jumpLocations.push(bytes.size());
            bytes.addAll(Util.zeroToBytes());
        }
        locations.add(jumpLocations);
    }

    private void addCase(@NotNull CaseStatementNode stmt, int start, @NotNull List<Byte> bytes) {
        var jumpLocations = locations.pop();
        for (int loc : jumpLocations) {
            adjustPointer(loc, start + bytes.size(), bytes);
        }
        var bodyBytes = BaseConverter.bytes(start + bytes.size(), stmt.getBody(), info);
        bytes.addAll(bodyBytes);
        bytes.add(Bytecode.JUMP.value);
        bytes.addAll(Util.zeroToBytes());
        info.addBreak(1, start + bytes.size());
    }

    private void adjustPointer(int location, int value, List<Byte> bytes) {
        var ptrBytes = Util.intToBytes(value);
        for (int i = 0; i < ptrBytes.size(); i++) {
            bytes.set(location + i, ptrBytes.get(i));
        }
    }
}
