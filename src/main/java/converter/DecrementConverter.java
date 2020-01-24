package main.java.converter;

import main.java.parser.DecrementNode;

public final class DecrementConverter extends IncrementDecrementConverter {

    public DecrementConverter(CompilerInfo info, DecrementNode node) {
        super(true, info, node);
    }
}
