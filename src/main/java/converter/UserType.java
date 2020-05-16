package main.java.converter;

import java.util.List;

public abstract class UserType extends NameableType {
    public abstract boolean isFinal();
    public abstract List<TypeObject> getSupers();
    public abstract GenericInfo getGenericInfo();
}
