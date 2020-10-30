package main.java.parser;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * The class representing a numeric literal.
 * @author Patrick Norton
 */
public class NumberNode implements AtomicNode {
    private static final MathContext PARSE_PRECISION = new MathContext(0, RoundingMode.UNNECESSARY);

    private LineInfo lineInfo;
    private BigDecimal value;

    /**
     * Create a new instance of BigDecimal.
     * @param value The value of the decimal
     */

    public NumberNode(LineInfo lineInfo, BigDecimal value) {
        this.lineInfo = lineInfo;
        this.value = value;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public BigDecimal getValue() {
        return value;
    }

    /**
     * Parse a NumberNode from a list of tokens.
     * @param tokens The list of tokens to be parsed destructively
     * @return The freshly parsed NumberNode
     */

    static NumberNode parse(TokenList tokens) {
        /*
         * How this method works:
         *
         * Numeric literals can come in a variety of forms. Some have a prefix
         * (of either 0x, 0b, or 0o). If either the string's length is < 2 or
         * the number does not start with 0, both meaning that the number must
         * be in base 10, the default BigDecimal constructor is used. Otherwise,
         * the base is attempted to be determined by the second character. If
         * the base is not determined to be non-base-10, the standard base 10
         * constructor is used. Otherwise, it moves on to the non-decimal
         * parser.
         *
         * The special parser is for numbers not in base 10. Firstly, it
         * attempts to parse the number as a long using Long#parseLong. If this
         * does succeed, all is well and the parsed long is used as the value.
         * If it does not succeed, it will not for one of two reasons: either
         * the number is too big to fit in a long, or the number contains a
         * decimal point. If the former is true, and there is no decimal, then
         * the number is parsed as a BigInteger and then converted to a
         * BigDecimal.
         *
         * If this is not the case, then the string representing the number has
         * its decimal point removed, is parsed into a BigInteger, and then
         * divided to put the decimal in the right place.
         */
        assert tokens.tokenIs(TokenType.NUMBER);
        String value = tokens.tokenSequence().replace("_", "");
        LineInfo info = tokens.lineInfo();
        tokens.nextToken();
        if (value.length() < 2 || value.charAt(0) != '0') {
            return new NumberNode(info, new BigDecimal(value));
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
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case '.':
                try {
                    return new NumberNode(info, new BigDecimal(value));
                } catch (NumberFormatException e) {
                    throw ParserInternalError.of("Illegal number " + value, info);
                }
            default:
                throw ParserInternalError.of("Illegal number " + value, info);
        }
        try {
            BigDecimal val = parseInt(value.substring(2), base);
            return new NumberNode(info, val);
        } catch (NumberFormatException e) {
            throw ParserInternalError.of("Illegal number " + value, info);
        }
    }

    /**
     * Parse the BigDecimal value of the string given, in non-base-10.
     * @param value The string value of the digit to be parsed
     * @param base The base of the number
     * @return The value of the string
     */

    private static BigDecimal parseInt(String value, int base) {
        try {
            return BigDecimal.valueOf(Long.parseLong(value, base));
        } catch (NumberFormatException e) {
            int dot = value.indexOf('.');
            if (dot == -1) {
                return new BigDecimal(new BigInteger(value, base));
            } else {
                /*
                 * Because BigDecimal doesn't allow non-base-10 bases in
                 * their BigDecimal arguments...
                 *
                 * This takes the value of the decimal, assuming it contains a
                 * dot, removes the dot, parses it as an integer, and then
                 * divides by the right number to shift it to the right value.
                 */
                String noDecimalValue = value.replaceFirst("\\.", "");
                BigDecimal unShifted = new BigDecimal(new BigInteger(noDecimalValue, base));
                return unShifted.divide(new BigDecimal(base).pow(value.length() - dot - 1), PARSE_PRECISION);
            }
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
