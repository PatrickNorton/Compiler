package main.java.converter;

import main.java.parser.FormattedStringNode;
import main.java.util.IndexedSet;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class FormatConstant implements LangConstant {
    private final FormattedStringNode.FormatInfo info;

    public FormatConstant(
            FormattedStringNode.FormatInfo info
    ) {
        this.info = info;
    }

    @Override
    @NotNull
    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>();
        bytes.add((byte) ConstantBytes.FORMAT.ordinal());
        bytes.addAll(Util.intToBytes(info.getFill()));
        bytes.add((byte) align());
        bytes.add((byte) sign());
        bytes.add(hashZeroByte());
        bytes.addAll(Util.intToBytes(info.getMinWidth()));
        bytes.addAll(Util.intToBytes(info.getPrecision()));
        bytes.add((byte) type());
        return bytes;
    }

    @Override
    @NotNull
    public TypeObject getType() {
        throw new UnsupportedOperationException();
    }

    private char align() {
        return info.getAlign() == '\0' ? '<' : info.getAlign();
    }

    private char sign() {
        return info.getSign() == '\0' ? '-' : info.getSign();
    }

    private char type() {
        return info.getType() == '\0' ? 's' : info.getType();
    }

    private byte hashZeroByte() {
        return (byte) ((info.isHash() ? 0b1 : 0b0) | (info.isZero() ? 0b10 : 0b00));
    }

    public static FormatConstant fromFormatInfo(FormattedStringNode.FormatInfo info) {
        return new FormatConstant(info);
    }

    @Override
    @NotNull
    public String name(IndexedSet<LangConstant> constants) {
        return info.toString();
    }
}
