import java.math.BigDecimal;

public class NumberNode implements AtomicNode {
    private BigDecimal integer;

    public NumberNode(BigDecimal integer) {
        this.integer = integer;
    }

    public BigDecimal getInteger() {
        return integer;
    }
}
