package main.java.converter;

import main.java.converter.bytecode.ArgcBytecode;
import main.java.converter.bytecode.BytecodeValue;
import main.java.converter.bytecode.ConstantBytecode;
import main.java.converter.bytecode.LocationBytecode;
import main.java.converter.bytecode.OperatorBytecode;
import main.java.parser.LineInfo;
import main.java.parser.OpSpTypeNode;
import main.java.util.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringJoiner;

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

    private BytecodeList(List<Value> values) {
        this.values = values;
    }

    public void add(Bytecode bytecode) {
        this.values.add(new Value(bytecode));
    }

    public void add(Bytecode bytecode, BytecodeValue firstParam) {
        this.values.add(new Value(bytecode, firstParam));
    }

    public void loadConstant(LangConstant constant, CompilerInfo info) {
        this.add(Bytecode.LOAD_CONST, new ConstantBytecode(constant, info));
    }

    public void loadConstant(LangConstant constant, GlobalCompilerInfo info) {
        this.add(Bytecode.LOAD_CONST, new ConstantBytecode(constant, info));
    }

    public void add(Bytecode bytecode, Label label) {
        this.values.add(new Value(bytecode, label));
    }

    public void add(Bytecode bytecode, BytecodeValue firstParam, BytecodeValue secondParam) {
        this.values.add(new Value(bytecode, firstParam, secondParam));
    }

    public void addCallOp(OpSpTypeNode operator, short argc) {
        this.add(Bytecode.CALL_OP, new OperatorBytecode(operator), new ArgcBytecode(argc));
    }

    public void addCallOp(OpSpTypeNode operator) {
        this.add(Bytecode.CALL_OP, new OperatorBytecode(operator), ArgcBytecode.zero());
    }

    public void add(Bytecode bytecode, Label firstParam, BytecodeValue secondParam) {
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

    public void addFirst(Bytecode bytecode, BytecodeValue firstParam) {
        this.values.add(0, new Value(bytecode, firstParam));
    }

    public void addFirst(Bytecode bytecode, BytecodeValue firstParam, BytecodeValue secondParam) {
        this.values.add(0, new Value(bytecode, firstParam, secondParam));
    }

    public void addAll(@NotNull Index index, @NotNull BytecodeList bytecode) {
        this.values.addAll(index.value, bytecode.values);
    }

    public void remove(@NotNull Index index) {
        this.values.remove(index.value);
    }

    public void removeRange(@NotNull Index start, @NotNull Index end) {
        this.values.subList(start.value, end.value).clear();
    }

    public void removeRange(@NotNull Index start) {
        this.values.subList(start.value, values.size()).clear();
    }

    public int bytecodeCount() {
        int result = 0;
        for (var value : values) {
            if (!value.isLabel()) {
                result++;
            }
        }
        return result;
    }

    @Nullable
    public Index nextLabel(@NotNull Index value) {
        for (int i = value.value; i < values.size(); i++) {
            if (values.get(i).isLabel()) {
                return new Index(i);
            }
        }
        return null;
    }

    @Contract("_ -> new")
    public BytecodeValue[] getOperands(@NotNull Index index) {
        assert !values.get(index.value).isLabel();
        var value = values.get(index.value);
        return switch (value.getBytecodeType().operandCount()) {
            case 0 -> new BytecodeValue[0];
            case 1 -> new BytecodeValue[]{value.getFirstParam()};
            case 2 -> new BytecodeValue[]{value.getFirstParam(), value.getSecondParam()};
            default -> throw CompilerInternalError.format(
                    "Unknown number of operands to bytecode: %d (bytecode value %d)",
                    LineInfo.empty(),
                    value.getBytecodeType().operandCount(), value.getBytecodeType().value
            );
        };
    }

    @Contract(pure = true)
    @NotNull
    public Iterable<Pair<Index, Bytecode>> enumerate() {
        return Enumerate::new;
    }

    @Contract(pure = true)
    @NotNull
    public Iterable<Bytecode> bytecodes() {
        return BytecodeIter::new;
    }

    private void addValue(Value value) {
        this.values.add(value);
    }

    @NotNull
    public List<Byte> convertToBytes() {
        setLabels();
        List<Byte> bytes = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            var value = values.get(i);
            if (value.isLabel()) {
                value.getLabel().setValue(bytes.size());
            } else if (value.getFirstParam() instanceof LocationBytecode loc) {
                var label = loc.getLabel();
                if (label.getValue() == -1) {
                    label.setValue(findLabelIndex(label, i, bytes.size()));
                }
                var bytecode = value.getBytecodeType();
                bytes.addAll(bytecode.assemble(value.getFirstParam(), value.getSecondParam()));
            } else {
                var bytecode = value.getBytecodeType();
                bytes.addAll(bytecode.assemble(value.getFirstParam(), value.getSecondParam()));
            }
        }
        return bytes;
    }

    @NotNull
    public BytecodeList copyLabels() {
        BytecodeList result = new BytecodeList(values.size());
        Map<Label, Label> translationMap = new HashMap<>();
        for (var value : values) {
            var label = value.getLabel();
            if (label != null) {
                var newLabel = translationMap.computeIfAbsent(label, x -> new Label());
                result.addValue(value.copyWithLabel(newLabel));
            } else {
                result.addValue(value.getCopy());
            }
        }
        return result;
    }

    public void setLabels() {
        var index = 0;
        for (var value : values) {
            if (value.isLabel()) {
                value.getLabel().setValue(index);
            } else {
                index += value.getBytecodeType().size();
            }
        }
    }

    @NotNull
    public String disassemble(CompilerInfo info) {
        setLabels();
        int i = 0;
        var sb = new StringBuilder();
        for (var value : values) {
            if (!value.isLabel()) {
                if (value.getFirstParam() != null) {
                    sb.append(String.format("%-7d%-16s", i, value.getBytecodeType()));
                    StringJoiner sj = new StringJoiner(", ");
                    sj.add(value.getFirstParam().strValue(info));
                    if (value.getSecondParam() != null) {
                        sj.add(value.getFirstParam().strValue(info));
                    }
                    sb.append(sj);
                    sb.append('\n');
                } else {
                    sb.append(String.format( "%-7d%s%n", i, value.getBytecodeType()));
                }
                i += value.getBytecodeType().size();
            }
        }
        return sb.toString();
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

    public boolean isEmpty() {
        return values.isEmpty();
    }

    @NotNull
    public static BytecodeList of(Bytecode value) {
        return new BytecodeList(List.of(new Value(value)));
    }

    public static final class Index {
        private final int value;

        private Index(int value) {
            this.value = value;
        }

        @Contract(value = " -> new", pure = true)
        @NotNull
        public Index next() {
            return new Index(value + 1);
        }
    }

    private static final class Value {
        private final boolean isLabel;
        private final Bytecode bytecodeType;
        private final Label label;
        private final BytecodeValue firstParam;
        private final BytecodeValue secondParam;

        public Value(Bytecode bytecode) {
            this(false, bytecode, null, null, null);
            assert bytecode.operandsMatch();
        }

        public Value(Bytecode bytecode, BytecodeValue firstParam) {
            this(false, bytecode, null, firstParam, null);
            assert bytecode.operandsMatch(firstParam);
        }

        public Value(Bytecode bytecode, Label label) {
            this(false, bytecode, null, new LocationBytecode(label), null);
            assert bytecode.operandsMatch(new LocationBytecode(label));
        }

        public Value(Bytecode bytecode, BytecodeValue firstParam, BytecodeValue secondParam) {
            this(false, bytecode, null, firstParam, secondParam);
            assert bytecode.operandsMatch(firstParam, secondParam);
        }

        public Value(Bytecode bytecode, Label label, BytecodeValue secondParam) {
            this(false, bytecode, null, new LocationBytecode(label), secondParam);
            assert bytecode.operandsMatch(new LocationBytecode(label), secondParam);
        }

        private Value(boolean isLabel, Bytecode bytecodeType, Label label, BytecodeValue firstParam, BytecodeValue secondParam) {
            this.isLabel = isLabel;
            this.bytecodeType = bytecodeType;
            this.label = label;
            this.firstParam = firstParam;
            this.secondParam = secondParam;
        }

        @Contract(value = "_ -> new", pure = true)
        @NotNull
        public static Value label(Label label) {
            return new Value(true, Bytecode.NOP, label, null, null);
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

        public BytecodeValue getFirstParam() {
            return firstParam;
        }

        public BytecodeValue getSecondParam() {
            return secondParam;
        }

        @Contract(value = " -> new", pure = true)
        @NotNull
        public Value getCopy() {
            return new Value(isLabel, bytecodeType, label, firstParam, secondParam);
        }

        @Contract(value = "_ -> new", pure = true)
        @NotNull
        public Value copyWithLabel(Label label) {
            return new Value(isLabel, bytecodeType, label, firstParam, secondParam);
        }
    }

    private final class Enumerate implements Iterator<Pair<Index, Bytecode>> {
        int index;

        private Enumerate() {
            this.index = 0;
        }

        @Override
        public boolean hasNext() {
            while (index < values.size() && values.get(index).isLabel()) {
                index++;
            }
            return index < values.size();
        }

        @Override
        @NotNull
        public Pair<Index, Bytecode> next() {
            while (index < values.size() && values.get(index).isLabel()) {
                index++;
            }
            if (index >= values.size()) {
                throw new NoSuchElementException();
            } else {
                var oldIndex = index++;
                return Pair.of(new Index(oldIndex), values.get(oldIndex).getBytecodeType());
            }
        }
    }

    private final class EnumerateLabel implements Iterator<Pair<Index, Label>> {
        int index;

        private EnumerateLabel() {
            this.index = 0;
        }

        @Override
        public boolean hasNext() {
            while (index < values.size() && !values.get(index).isLabel()) {
                index++;
            }
            return index < values.size();
        }

        @Override
        @NotNull
        public Pair<Index, Label> next() {
            while (index < values.size() && !values.get(index).isLabel()) {
                index++;
            }
            if (index >= values.size()) {
                throw new NoSuchElementException();
            } else {
                var oldIndex = index++;
                return Pair.of(new Index(oldIndex), values.get(oldIndex).getLabel());
            }
        }
    }

    private final class BytecodeIter implements Iterator<Bytecode> {
        int index;

        private BytecodeIter() {
            this.index = 0;
        }

        @Override
        public boolean hasNext() {
            while (index < values.size() && values.get(index).isLabel()) {
                index++;
            }
            return index < values.size();
        }

        @Override
        @NotNull
        public Bytecode next() {
            while (index < values.size() && values.get(index).isLabel()) {
                index++;
            }
            if (index >= values.size()) {
                throw new NoSuchElementException();
            } else {
                var oldIndex = index++;
                return values.get(oldIndex).bytecodeType;
            }
        }
    }
}
