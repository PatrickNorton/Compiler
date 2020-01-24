package main.java.converter;

import main.java.parser.IncrementNode;

public final class IncrementConverter extends IncrementDecrementConverter {

    public IncrementConverter(CompilerInfo info, IncrementNode node) {
        super(false, info, node);
    }
}
