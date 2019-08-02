import java.math.BigInteger;

public class IntegerNode implements AtomicNode {
    private BigInteger integer;

    public IntegerNode(BigInteger integer) {
        this.integer = integer;
    }

    public BigInteger getInteger() {
        return integer;
    }
}
