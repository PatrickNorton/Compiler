package main.java.converter;

import main.java.parser.TypeLikeNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;

public final class GenericInfo implements Iterable<TemplateParam>, RandomAccess {
    private final List<TemplateParam> params;

    private GenericInfo(@NotNull List<TemplateParam> params) {
        this.params = params;
    }

    public List<TemplateParam> getParams() {
        return params;
    }

    public boolean isEmpty() {
        return params.isEmpty();
    }

    public TemplateParam get(int i) {
        return params.get(i);
    }

    public int size() {
        return params.size();
    }

    @NotNull
    @Override
    public Iterator<TemplateParam> iterator() {
        return new InfoIterator();
    }

    private class InfoIterator implements Iterator<TemplateParam> {
        private int index = 0;

        @Override
        public boolean hasNext() {
            return index < size();
        }

        @Override
        public TemplateParam next() {
            return params.get(index++);
        }
    }

    @NotNull
    @Contract(pure = true)
    public static GenericInfo parse(CompilerInfo info, @NotNull TypeLikeNode... generics) {
        if (generics.length == 0) return empty();
        List<TemplateParam> params = new ArrayList<>();
        for (int i = 0; i < generics.length; i++) {
            var generic = generics[i];
            var bound = generic.getSubtypes().length == 1 ? info.getType(generic.getSubtypes()[0]) : Builtins.OBJECT;
            var param = new TemplateParam(generic.strName(), i, bound);
            info.addType(param);
            params.add(param);
        }
        return new GenericInfo(Collections.unmodifiableList(params));
    }

    @NotNull
    @Contract("_ -> new")
    public static GenericInfo of(TemplateParam... args) {
        return new GenericInfo(Collections.unmodifiableList(List.of(args)));
    }

    private static final GenericInfo EMPTY = new GenericInfo(Collections.emptyList());

    @NotNull
    @Contract(" -> new")
    public static GenericInfo empty() {
        return EMPTY;
    }
}
