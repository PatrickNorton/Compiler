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

    public BytecodeList(@NotNull BytecodeList other) {
        this.values = new ArrayList<>(other.values);
    }

    public BytecodeList(int initialCapacity) {
        this.values = new ArrayList<>(initialCapacity);
    }

    public void add(Bytecode bytecode) {
        this.values.add(new Value(bytecode));
    }

    public void add(Bytecode bytecode, int firstParam) {
        this.values.add(new Value(bytecode, firstParam));
    }

    public void add(Bytecode bytecode, Label label) {
        this.values.add(new Value(bytecode, label));
    }

    public void add(Bytecode bytecode, int firstParam, int secondParam) {
        this.values.add(new Value(bytecode, firstParam, secondParam));
    }

    public void add(Bytecode bytecode, Label firstParam, int secondParam) {
        this.values.add(new Value(bytecode, firstParam, secondParam));
    }

    public void addLabel(Label label) {
        this.values.add(Value.label(label));
    }

    public void addAll(@NotNull BytecodeList other) {
        this.values.addAll(other.values);
    }

    public void addFirst(Bytecode bytecode) {
        this.values.add(0, new Value(bytecode));
    }

    public void addFirst(Bytecode bytecode, int firstParam) {
        this.values.add(0, new Value(bytecode, firstParam));
    }

    public void addFirst(Bytecode bytecode, int firstParam, int secondParam) {
        this.values.add(0, new Value(bytecode, firstParam, secondParam));
    }

    @NotNull
    public List<Byte> convertToBytes() {
        List<Byte> bytes = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            var value = values.get(i);
            if (value.isLabel()) {
                value.getLabel().setValue(bytes.size());
            } else if (value.getLabel() != null) {
                var label = value.getLabel();
                if (label.getValue() == -1) {
                    label.setValue(findLabelIndex(label, i, bytes.size()));
                }
                var bytecode = value.getBytecodeType();
                bytes.addAll(bytecode.assemble(label.getValue(), value.getSecondParam()));
            } else {
                var bytecode = value.getBytecodeType();
                bytes.addAll(bytecode.assemble(value.getFirstParam(), value.getSecondParam()));
            }
        }
        return bytes;
    }

    @NotNull
    public Map<Integer, Integer> getLabelMap() {
        int index = 0;
        Map<Integer, Integer> result = new HashMap<>();
        for (var value : values) {
            if (value.isLabel()) {
                result.put(value.getFirstParam(), index);
            } else {
                index += value.getBytecodeType().size();
            }
        }
        return result;
    }

    private int findLabelIndex(Label label, int currentIndex, int byteLen) {
        int bytecodeLen = byteLen;
        for (int i = currentIndex; i < values.size(); i++) {
            var value = values.get(i);
            if (value.isLabel()) {
                value.getLabel().setValue(bytecodeLen);
                if (value.getLabel().equals(label)) {
                    return bytecodeLen;
                }
            } else {
                bytecodeLen += value.bytecodeType.size();
            }
        }
        throw CompilerInternalError.format("Unknown label number: %s", LineInfo.empty(), label);
    }

    private static final class Value {
        private final boolean isLabel;
        private final Bytecode bytecodeType;
        private final Label label;
        private final int firstParam;
        private final int secondParam;

        public Value(Bytecode bytecode) {
            this(false, bytecode, null, -1, -1);
        }

        public Value(Bytecode bytecode, int firstParam) {
            this(false, bytecode, null, firstParam, -1);
        }

        public Value(Bytecode bytecode, Label label) {
            this(false, bytecode, label, -1, -1);
        }

        public Value(Bytecode bytecode, int firstParam, int secondParam) {
            this(false, bytecode, null, firstParam, secondParam);
        }

        public Value(Bytecode bytecode, Label label, int secondParam) {
            this(false, bytecode, label, -1, secondParam);
        }

        private Value(boolean isLabel, Bytecode bytecodeType, Label label, int firstParam, int secondParam) {
            this.isLabel = isLabel;
            this.bytecodeType = bytecodeType;
            this.label = label;
            this.firstParam = firstParam;
            this.secondParam = secondParam;
        }

        @Contract(value = "_ -> new", pure = true)
        @NotNull
        public static Value label(Label label) {
            return new Value(true, Bytecode.NOP, label, 0, -1);
        }

        public boolean isLabel() {
            return isLabel;
        }

        public Bytecode getBytecodeType() {
            return bytecodeType;
        }

        public Label getLabel() {
            return label;
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
