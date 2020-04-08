package main.java.converter;

import main.java.parser.TypeLikeNode;
import main.java.parser.TypeNode;
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
        assert params.stream().filter(TemplateParam::isVararg).count() <= 1;
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
    public List<TypeObject> generify(@NotNull TypeObject... args) {
        List<TypeObject> result = new ArrayList<>();
        int i;
        for (i = 0; i < args.length && !params.get(i).isVararg(); i++) {
            assert params.get(i).getBound() instanceof ListTypeObject == args[i] instanceof ListTypeObject;
            result.add(args[i]);
        }
        if (i == args.length) return result;
        List<TypeObject> resultTypes = new ArrayList<>(args.length - i - 1);
        int j;
        for (j = args.length - 1; j >= i && !params.get(Math.abs(args.length - params.size()) + j).isVararg(); j--) {
            resultTypes.add(0, args[j]);
        }
        List<TypeObject> varargTypes = new ArrayList<>(j - i + 1);
        for (; j >= i; j--) {
            varargTypes.add(0, args[j]);
        }
        result.add(TypeObject.list(varargTypes.toArray(new TypeObject[0])));
        result.addAll(resultTypes);
        return result;
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
            TemplateParam param;
            if (generic.isVararg()) {
                param = new TemplateParam(generic.strName(), i, true);
            } else if (generic instanceof TypeNode && ((TypeNode) generic).getName().isEmpty()) {
                assert generic.getSubtypes().length == 1 && generic.getSubtypes()[0].isVararg();  // => [*T]
                param = new TemplateParam(generic.getSubtypes()[0].strName(), i, TypeObject.list());
            } else {
                var bound = generic.getSubtypes().length == 1 ? info.getType(generic.getSubtypes()[0]) : Builtins.OBJECT;
                param = new TemplateParam(generic.strName(), i, bound);
            }
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
