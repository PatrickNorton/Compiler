package main.java.converter;

import main.java.parser.TypeLikeNode;
import main.java.parser.TypeNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.RandomAccess;

public final class GenericInfo implements Iterable<TemplateParam>, RandomAccess {
    private final List<TemplateParam> params;
    private boolean isRegistered;

    private GenericInfo(List<TemplateParam> params, boolean isRegistered) {
        this.params = params;
        this.isRegistered = isRegistered;
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

    public void setParent(TypeObject parent) {
        for (var param : params) {
            param.setParent(parent);
        }
    }

    public Map<String, TypeObject> getParamMap() {
        Map<String, TypeObject> result = new HashMap<>(params.size());
        for (var param : params) {
            result.put(param.baseName(), param);
        }
        return result;
    }

    public Optional<List<TypeObject>> generify(TypeObject... args) {
        if (!hasVararg()) {
            return generifyNoVarargs(args);
        } else {
            return generifyVararg(args);
        }
    }

    private Optional<List<TypeObject>> generifyNoVarargs(TypeObject... args) {
        if (args.length != params.size()) {
            return Optional.empty();
        }
        List<TypeObject> result = new ArrayList<>(args.length);
        for (int i = 0; i < args.length; i++) {
            if (isInvalid(args[i], params.get(i))) {
                return Optional.empty();
            }
            result.add(args[i]);
        }
        return Optional.of(result);
    }

    private Optional<List<TypeObject>> generifyVararg(TypeObject... args) {
        if (args.length < params.size() - 1) {
            return Optional.empty();
        }
        List<TypeObject> result = new ArrayList<>(params.size());
        int i;
        for (i = 0; i < args.length && !params.get(i).isVararg(); i++) {
            if (isInvalid(args[i], params.get(i))) {
                return Optional.empty();
            }
            result.add(args[i]);
        }
        var remainingArgs = args.length - i;
        var remainingParams = params.size() - i;
        if (remainingArgs < remainingParams) {
            // Number of types resulted in empty vararg, proceed accordingly
            result.add(TypeObject.list());
            for (; i < args.length; i++) {
                if (isInvalid(args[i], params.get(i + 1))) {
                    return Optional.empty();
                }
                result.add(args[i]);
            }
        } else if (remainingArgs == remainingParams) {
            // Exactly one parameter goes into the vararg (refactored b/c easy)
            result.add(TypeObject.list(args[i++]));
            for (; i < args.length; i++) {
                if (isInvalid(args[i], params.get(i))) {
                    return Optional.empty();
                }
                result.add(args[i]);
            }
        } else {  // remainingArgs > remainingParams
            int diff = remainingArgs - remainingParams;
            List<TypeObject> listArgs = new ArrayList<>(diff);
            listArgs.addAll(Arrays.asList(args).subList(i, diff + i + 1));
            result.add(TypeObject.list(listArgs.toArray(new TypeObject[0])));
            i++;
            for (; i < params.size(); i++) {
                if (isInvalid(args[i + diff], params.get(i))) {
                    return Optional.empty();
                }
                result.add(args[i + diff]);
            }
        }
        return Optional.of(result);
    }

    private boolean isInvalid(TypeObject arg,TemplateParam param) {
        boolean isList = param.getBound() instanceof ListTypeObject;
        if (isList != arg instanceof ListTypeObject) {
            return true;
        } else if (!isList) {
            return param.getBound() != null && !param.getBound().isSuperclass(arg);
        } else {
            return false;
        }

    }

    private boolean hasVararg() {
        for (var param : params) {
            if (param.isVararg()) {
                return true;
            }
        }
        return false;
    }

    public void reParse(CompilerInfo info, TypeLikeNode... generics) {
        if (isEmpty()) return;
        assert !isRegistered;
        isRegistered = true;
        var genInfo = parse(info, generics);
        assert params.size() == genInfo.params.size();
        params.clear();
        params.addAll(genInfo.params);
    }

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

    public static GenericInfo parse(CompilerInfo info,TypeLikeNode... generics) {
        if (generics.length == 0) return empty();
        List<TemplateParam> params = new ArrayList<>(generics.length);
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
            params.add(param);
        }
        return new GenericInfo(Collections.unmodifiableList(params), true);
    }

    public static GenericInfo parseNoTypes(TypeLikeNode... generics) {
        if (generics.length == 0) return empty();
        List<TemplateParam> params = new ArrayList<>(generics.length);
        for (int i = 0; i < generics.length; i++) {
            var generic = generics[i];
            TemplateParam param;
            if (generic.isVararg()) {
                param = new TemplateParam(generic.strName(), i, true);
            } else if (generic instanceof TypeNode && ((TypeNode) generic).getName().isEmpty()) {
                assert generic.getSubtypes().length == 1 && generic.getSubtypes()[0].isVararg();  // => [*T]
                param = new TemplateParam(generic.getSubtypes()[0].strName(), i, TypeObject.list());
            } else if (generic.getSubtypes().length == 0) {
                param = new TemplateParam(generic.strName(), i, Builtins.OBJECT);
            } else {
                // FIXME: Default interfaces are done before types are registered, therefore this will cause a NPE
                param = new TemplateParam(generic.strName(), i, null);
            }
            params.add(param);
        }
        return new GenericInfo(params, false);
    }

    public static GenericInfo of(TemplateParam... args) {
        return new GenericInfo(Collections.unmodifiableList(List.of(args)), true);
    }

    private static final GenericInfo EMPTY = new GenericInfo(List.of(), true);

    public static GenericInfo empty() {
        return EMPTY;
    }
}
