package main.java.converter;

/**
 * A class for dealing with jump-labels.
 * <p>
 *     Each label is compared by object-identity, so different labels are
 *     different objects. They also have methods for setting the actual
 *     byte-value of the label, this *can* be done multiple times, but care
 *     should be taken to ensure that the final output is correct.
 * </p>
 */
public final class Label {
    private int value;

    public Label() {
        this.value = -1;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
