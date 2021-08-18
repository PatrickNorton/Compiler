package main.java.converter;

import main.java.parser.LineInfo;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BytecodeList {
    private final List<Value> values;

    public BytecodeList() {
        this.values = new ArrayList<>();
    }

    public void add(Bytecode bytecode) {
        this.values.add(new Value(bytecode));
    }

    public void add(Bytecode bytecode, int firstParam) {
        this.values.add(new Value(bytecode, firstParam));
    }

    public void add(Bytecode bytecode, int firstParam, int secondParam) {
        this.values.add(new Value(bytecode, firstParam, secondParam));
    }

    public void addLabel(int labelNo) {
        this.values.add(Value.label(labelNo));
    }

    public void addAll(@NotNull BytecodeList other) {
        this.values.addAll(other.values);
    }

    @NotNull
    public List<Byte> convertToBytes() {
        List<Byte> bytes = new ArrayList<>();
        Map<Integer, Integer> labelIndices = new HashMap<>();
        for (int i = 0; i < values.size(); i++) {
            var value = values.get(i);
            if (value.isLabel()) {
                labelIndices.put(value.getFirstParam(), bytes.size());
            } else {
                var bytecode = value.getBytecodeType();
                if (bytecode.isJump()) {
                    var labelNo = value.getFirstParam();
                    int labelIndex;
                    if (labelIndices.containsKey(labelNo)) {
                        labelIndex = labelIndices.get(labelNo);
                    } else {
                        labelIndex = findLabelIndex(labelNo, labelIndices, i, bytes.size());
                    }
                    bytes.addAll(bytecode.assemble(labelIndex, value.getSecondParam()));
                } else {
                    bytes.addAll(bytecode.assemble(value.getFirstParam(), value.getSecondParam()));
                }
            }
        }
        return bytes;
    }

    private int findLabelIndex(int labelNo, Map<Integer, Integer> labelIndices, int currentIndex, int byteLen) {
        int bytecodeLen = byteLen;
        for (int i = currentIndex; i < values.size(); i++) {
            var value = values.get(i);
            if (value.isLabel()) {
                labelIndices.put(value.getFirstParam(), bytecodeLen);
                if (value.getFirstParam() == labelNo) {
                    return bytecodeLen;
                }
            } else {
                bytecodeLen += value.bytecodeType.size();
            }
        }
        throw CompilerInternalError.format("Unknown label number: %s", LineInfo.empty(), labelNo);
    }

    private static final class Value {
        private final boolean isLabel;
        private final Bytecode bytecodeType;
        private final int firstParam;
        private final int secondParam;

        public Value(Bytecode bytecode) {
            this(false, bytecode, -1, -1);
        }

        public Value(Bytecode bytecode, int firstParam) {
            this(false, bytecode, firstParam, -1);
        }

        public Value(Bytecode bytecode, int firstParam, int secondParam) {
            this(false, bytecode, firstParam, secondParam);
        }

        private Value(boolean isLabel, Bytecode bytecodeType, int firstParam, int secondParam) {
            this.isLabel = isLabel;
            this.bytecodeType = bytecodeType;
            this.firstParam = firstParam;
            this.secondParam = secondParam;
        }

        @Contract(value = "_ -> new", pure = true)
        @NotNull
        public static Value label(int labelNo) {
            return new Value(true, Bytecode.NOP, labelNo, -1);
        }

        public boolean isLabel() {
            return isLabel;
        }

        public Bytecode getBytecodeType() {
            return bytecodeType;
        }

        public int getFirstParam() {
            return firstParam;
        }

        public int getSecondParam() {
            return secondParam;
        }

        public int byteCount() {
            if (isLabel) {
                return 0;
            } else {
                return bytecodeType.size();
            }
        }
    }
}
