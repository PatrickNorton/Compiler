package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * The class representing a numeric literal.
 * @author Patrick Norton
 */
public class NumberNode implements AtomicNode {
    private BigDecimal integer;

    /**
     * Create a new instance of BigDecimal.
     * @param integer The value of the decimal
     */
    @Contract(pure = true)
    public NumberNode(BigDecimal integer) {
        this.integer = integer;
    }

    public BigDecimal getInteger() {
        return integer;
    }

    /**
     * Parse a NumberNode from a list of tokens.
     * @param tokens The list of tokens to be parsed destructively
     * @return The freshly parsed NumberNode
     */
    @NotNull
    @Contract("_ -> new")
    static NumberNode parse(@NotNull TokenList tokens) {
        String value = tokens.getFirst().sequence;
        tokens.nextToken();
        if (value.length() < 2) {
            return new NumberNode(new BigDecimal(value));
        }
        int base;
        switch (value.charAt(1)) {
            case 'x':
                base = 16;
                break;
            case 'b':
                base = 2;
                break;
            case 'o':
                base = 8;
                break;
            default:
                try {
                    return new NumberNode(new BigDecimal(value));
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Illegal number " + value);
                }
        }
        try {
            BigDecimal val = parseInt(value.substring(2), base);
            return new NumberNode(val);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Illegal number " + value);
        }
    }

    /**
     * Parse the BigDecimal value of the string given, in non-base-10.
     * @param value The string value of the digit to be parsed
     * @param base The base of the number
     * @return The value of the string
     */
    @NotNull
    @Contract("_, _ -> new")
    private static BigDecimal parseInt(@NotNull String value, int base) {
        try {
            return BigDecimal.valueOf(Long.parseLong(value, base));
        } catch (NumberFormatException e) {
            int dot = value.indexOf('.');
            if (dot == -1) {
                return new BigDecimal(new BigInteger(value, base));
            } else {
                String noDecimalValue = value.replaceFirst("\\.", "");
                BigDecimal unShifted = new BigDecimal(new BigInteger(noDecimalValue, base));
                MathContext precision = new MathContext(0, RoundingMode.UNNECESSARY);
                return unShifted.divide(new BigDecimal(base).pow(value.length() - dot), precision);
            }
        }
    }

    @Override
    public String toString() {
        return integer.toString();
    }
}
