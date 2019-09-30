package Parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The class representing a numeric literal.
 * @author Patrick Norton
 */
public class NumberNode implements AtomicNode {
    private BigDecimal integer;

    private static final String DIGITS = "0123456789abcdef";

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
        int digit_size;
        switch (value.charAt(1)) {
            case 'x':
                digit_size = 16;
                break;
            case 'b':
                digit_size = 2;
                break;
            case 'o':
                digit_size = 8;
                break;
            default:
                try {
                    return new NumberNode(new BigDecimal(value));
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Illegal number given");
                }
        }
        BigDecimal val = parseInt(value.substring(2), DIGITS.substring(0, digit_size));
        return new NumberNode(val);
    }

    /**
     * Parse the BigDecimal value of the string given, in non-base-10.
     * @param value The string value of the digit to be parsed
     * @param digits The valid digits that can be used
     * @return The value of the string
     */
    private static BigDecimal parseInt(@NotNull String value, @NotNull String digits) {
        int dot = value.indexOf('.');
        int exp_size = dot >= 0 ? dot - 1 : value.length() - 1;
        BigDecimal base = BigDecimal.valueOf(digits.length());
        value = value.replaceAll("\\.", "");
        BigDecimal val = new BigDecimal(0);
        for (int i = 0; i < value.length(); i++) {
            char digit = value.charAt(i);
            if (digits.indexOf(digit) == -1) {
                throw new ParserException(digit + " is not a valid digit");
            }
            BigDecimal digit_val = BigDecimal.valueOf(digits.indexOf(digit));
            if (exp_size - i >= 0) {
                val = val.add(base.pow(exp_size - i).multiply(digit_val));
            } else {
                BigDecimal placeValue = BigDecimal.ONE.divide(base.pow(i - exp_size),
                        3*(value.length() - exp_size), RoundingMode.UNNECESSARY);
                val = val.add(placeValue.multiply(digit_val));
            }
        }
        return val;
    }

    @Override
    public String toString() {
        return integer.toString();
    }
}
