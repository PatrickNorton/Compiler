package main.java.converter;

import main.java.parser.CaseStatementNode;
import main.java.parser.DefaultStatementNode;
import main.java.parser.SwitchStatementNode;
import main.java.parser.TestNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class SwitchConverter extends LoopConverter implements TestConverter {
    private SwitchStatementNode node;
    private int retCount;  // TODO: Make switch expressions work

    public SwitchConverter(CompilerInfo info, SwitchStatementNode node, int retCount) {
        super(info, false);
        this.node = node;
        this.retCount = retCount;
    }

    @NotNull
    public List<Byte> trueConvert(int start) {
        var switched = TestConverter.bytes(start, node.getSwitched(), info, 1);
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
        info.addBreak(1, start + bytes.size());
        bytes.addAll(Util.zeroToBytes());
        var endCase = Util.intToBytes(start + bytes.size());
        for (var jumpLoc : jumpLocations) {
            Util.emplace(bytes, endCase, jumpLoc);
        }
    }
}
