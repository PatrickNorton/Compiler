package main.java.converter;

import main.java.parser.TypeLikeNode;
import main.java.parser.TypeNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GenericInfo {
    private final List<String> generics;
    private final List<TypeObject> bounds;

    private GenericInfo(@NotNull List<String> generics, @NotNull List<TypeObject> bounds) {
        assert generics.size() == bounds.size();
        this.generics = generics;
        this.bounds = bounds;
    }

    public List<String> getGenerics() {
        return generics;
    }

    public List<TypeObject> getBounds() {
        return bounds;
    }

    @NotNull
    @Contract(pure = true)
    public static GenericInfo parse(CompilerInfo info, @NotNull TypeLikeNode... generics) {
        List<String> genericNames = new ArrayList<>(generics.length);
        List<TypeObject> bounds = new ArrayList<>(generics.length);
        for (var generic : generics) {
            assert generic instanceof TypeNode;
            genericNames.add(generic.strName());
            var subTypes = generic.getSubtypes();
            assert subTypes.length == 0 || subTypes.length == 1;
            bounds.add(subTypes.length == 0 ? Builtins.OBJECT : info.getType(subTypes[0]));
        }
        return new GenericInfo(genericNames, bounds);
    }

    private static GenericInfo EMPTY = new GenericInfo(Collections.emptyList(), Collections.emptyList());

    @NotNull
    @Contract(" -> new")
    public static GenericInfo empty() {
        return EMPTY;
    }
}
