package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The class representing a temporary constant.
 * <p>
 *     This is for use with {@link CompilerInfo#reserveConst}, in which it is
 *     used to reserve a temporary position in the constant heap. It should
 *     never be used on its own, and should <i>always</i> be resolved into a
 *     concrete constant (using {@link CompilerInfo#setReserved}) before the
 *     file is written out.
 * </p>
 *
 * @author Patrick Norton
 * @see CompilerInfo#reserveConst
 */
public final class TempConst implements LangConstant {
    private final TypeObject type;

    public TempConst(TypeObject type) {
        this.type = type;
    }

    @Override
    public @NotNull List<Byte> toBytes() {
        throw new UnsupportedOperationException("All temporary constants must be instantiated");
    }

    @Override
    public @NotNull TypeObject getType() {
        return type;
    }
}
