import java.math.BigDecimal;

public class NumberNode implements AtomicNode {
    private BigDecimal integer;

    public NumberNode(BigDecimal integer) {
        this.integer = integer;
    }

    public BigDecimal getInteger() {
        return integer;
    }

    static NumberNode parse(TokenList tokens) {
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
                return new NumberNode(new BigDecimal(value));
        }
        BigDecimal val = parseInt(value.substring(2), "0123465789abcdef".substring(0, digit_size));
        return new NumberNode(val);
    }

    private static BigDecimal parseInt(String value, String digits) {
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
                val = val.add(BigDecimal.ONE.divide(base.pow(i - exp_size)).multiply(digit_val));
            }
        }
        return val;
    }
}
