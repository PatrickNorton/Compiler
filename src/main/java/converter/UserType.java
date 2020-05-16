package main.java.converter;

import main.java.parser.OpSpTypeNode;
import main.java.util.Pair;

import java.util.List;
import java.util.Set;

public abstract class UserType extends NameableType {
    public abstract boolean isFinal();
    public abstract List<TypeObject> getSupers();
    public abstract GenericInfo getGenericInfo();
    public abstract Pair<Set<String>, Set<OpSpTypeNode>> contract();
}
