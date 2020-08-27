package main.java.converter;

import main.java.converter.classbody.ConverterHolder;
import main.java.converter.classbody.MethodInfo;
import main.java.parser.BaseClassNode;
import main.java.parser.DeclarationNode;
import main.java.parser.DeclaredAssignmentNode;
import main.java.parser.DescriptorNode;
import main.java.parser.IndependentNode;
import main.java.parser.MethodDefinitionNode;
import main.java.parser.OperatorDefinitionNode;
import main.java.parser.PropertyDefinitionNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ClassConverterBase<T extends BaseClassNode> {
    protected final CompilerInfo info;
    protected final T node;

    public ClassConverterBase(CompilerInfo info, T node) {
        this.info = info;
        this.node = node;
    }

    @NotNull
    protected final <U> Map<U, List<Byte>> convert(UserType<?> type, @NotNull Map<U, MethodInfo> functions) {
        Map<U, List<Byte>> result = new HashMap<>();
        for (var pair : functions.entrySet()) {
            var methodInfo = pair.getValue();
            var isConstMethod = !methodInfo.getDescriptors().contains(DescriptorNode.MUT);
            info.addStackFrame();
            info.addVariable("self", isConstMethod ? type.makeConst() : type.makeMut(), isConstMethod, node);
            info.addVariable("cls", Builtins.TYPE.generify(type), true, node);
            var handler = info.accessHandler();
            try {
                handler.allowPrivateAccess(type);
                handler.addCls(type);
                recursivelyAllowProtectedAccess(handler, type);
                var fnInfo = methodInfo.getInfo();
                for (var arg : fnInfo.getArgs()) {
                    info.addVariable(arg.getName(), arg.getType(), methodInfo);
                }
                var retInfo = info.getFnReturns();
                retInfo.addFunctionReturns(fnInfo.getReturns());
                var bytes = BaseConverter.bytes(0, methodInfo.getBody(), info);
                retInfo.popFnReturns();
                result.put(pair.getKey(), bytes);
                info.removeStackFrame();
            } finally {
                handler.removePrivateAccess(type);
                handler.removeCls();
                recursivelyRemoveProtectedAccess(handler, type);
            }
        }
        return result;
    }

    private void recursivelyAllowProtectedAccess(AccessHandler handler, @NotNull UserType<?> type) {
        for (var superCls : type.getSupers()) {
            handler.allowProtectedAccess(superCls);
            if (superCls instanceof StdTypeObject) {
                recursivelyAllowProtectedAccess(handler, (StdTypeObject) superCls);
            }
        }
    }

    private void recursivelyRemoveProtectedAccess(AccessHandler handler, @NotNull UserType<?> type) {
        for (var superCls : type.getSupers()) {
            handler.removeProtectedAccess(superCls);
            if (superCls instanceof StdTypeObject) {
                recursivelyRemoveProtectedAccess(handler, (StdTypeObject) superCls);
            }
        }
    }

    private ClassInfo createClass(UserType<?> type, List<String> variants,
                                  List<Short> superConstants, @NotNull ConverterHolder converter) {
        return new ClassInfo.Factory()
                .setType(type)
                .setSuperConstants(superConstants)
                .setVariables(converter.varsWithInts())
                .setStaticVariables(converter.staticVarsWithInts())
                .setOperatorDefs(convert(type, converter.getOperators()))
                .setStaticOperators(convert(type, converter.getStaticOperators()))
                .setMethodDefs(convert(type, converter.getMethods()))
                .setStaticMethods(convert(type, converter.getStaticMethods()))
                .setProperties(merge(convert(type, converter.allGetters()), convert(type, converter.getSetters())))
                .setStaticProperties(merge(convert(type, converter.staticGetters()),
                        convert(type, converter.staticSetters())))
                .setVariants(variants)
                .create();
    }

    protected final void addToInfo(UserType<?> type, String defType, List<String> variants,
                                   List<Short> superConstants, @NotNull ConverterHolder converter) {
        var name = node.getName().strName();
        info.checkDefinition(name, node);
        info.reserveConstVar(name, Builtins.TYPE.generify(type), node);
        var cls = createClass(type, variants, superConstants, converter);
        int classIndex = info.addClass(cls);
        if (Builtins.FORBIDDEN_NAMES.contains(name)) {
            throw CompilerException.format("Illegal name for %s '%s'", node.getName(), defType, name);
        }
        info.setReservedVar(name, new ClassConstant(name, classIndex, type));
    }

    protected final void addToInfo(UserType<?> type, String defType,
                                   List<Short> superConstants, @NotNull ConverterHolder converter) {
        addToInfo(type, defType, null, superConstants, converter);
    }

    protected final UserType<?>[] convertSupers(TypeObject[] supers) {
        try {
            return Arrays.copyOf(supers, supers.length, UserType[].class);
        } catch (ArrayStoreException e) {
            throw CompilerException.format(
                    "Class '%s' inherits from a non-standard type",
                    node, node.getName()
            );
        }
    }

    protected final void ensureProperInheritance(UserType<?> type, @NotNull UserType<?>... supers) {
        for (var superCls : supers) {
            if (superCls.isFinal()) {
                throw CompilerException.format(
                        "Class '%s' inherits from class '%s', which is not marked 'nonfinal'",
                        node, type.name(), superCls.name()
                );
            }
        }
    }

    protected final void parseStatements(ConverterHolder converter) {
        for (var stmt : node.getBody().getStatements()) {
            parseStatement(stmt, converter);
        }
    }

    protected void parseStatement(IndependentNode stmt, ConverterHolder converter) {
        if (stmt instanceof DeclarationNode) {
            converter.attributes().parse((DeclarationNode) stmt);
        } else if (stmt instanceof DeclaredAssignmentNode) {
            converter.attributes().parse((DeclaredAssignmentNode) stmt);
        } else if (stmt instanceof MethodDefinitionNode) {
            converter.methods().parse((MethodDefinitionNode) stmt);
        } else if (stmt instanceof OperatorDefinitionNode) {
            converter.operators().parse((OperatorDefinitionNode) stmt);
        } else if (stmt instanceof PropertyDefinitionNode) {
            converter.properties().parse((PropertyDefinitionNode) stmt);
        } else {
            throw new UnsupportedOperationException("Node not yet supported");
        }
    }

    @NotNull
    protected static <T, U> Map<T, Pair<U, U>> merge(@NotNull Map<T, U> first, @NotNull Map<T, U> second) {
        assert first.size() == second.size();
        Map<T, Pair<U, U>> result = new HashMap<>(first.size());
        for (var pair : first.entrySet()) {
            var key = pair.getKey();
            assert second.containsKey(key);
            result.put(key, Pair.of(pair.getValue(), second.get(key)));
        }
        return result;
    }
}
