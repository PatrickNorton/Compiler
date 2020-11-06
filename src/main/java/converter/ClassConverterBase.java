package main.java.converter;

import main.java.converter.classbody.ConverterHolder;
import main.java.converter.classbody.Method;
import main.java.parser.BaseClassNode;
import main.java.parser.DeclarationNode;
import main.java.parser.DeclaredAssignmentNode;
import main.java.parser.IndependentNode;
import main.java.parser.MethodDefinitionNode;
import main.java.parser.OpSpTypeNode;
import main.java.parser.OperatorDefinitionNode;
import main.java.parser.PropertyDefinitionNode;
import main.java.util.Pair;
import main.java.util.Zipper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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
    protected final <U> Map<U, List<Byte>> convert(UserType<?> type, @NotNull Map<U, Method> functions) {
        Map<U, List<Byte>> result = new HashMap<>();
        for (var pair : functions.entrySet()) {
            var methodInfo = pair.getValue();
            var isConstMethod = !methodInfo.isMut();
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
                    if (arg.isVararg()) {
                        info.addVariable(arg.getName(), Builtins.ITERABLE.generify(arg, arg.getType()), methodInfo);
                    } else {
                        info.addVariable(arg.getName(), arg.getType(), methodInfo);
                    }
                }
                var retInfo = info.getFnReturns();
                retInfo.addFunctionReturns(fnInfo.isGenerator(), fnReturns(fnInfo));
                if (pair.getKey() == OpSpTypeNode.NEW) {
                    info.accessHandler().enterConstructor(type);
                }
                var bytes = BaseConverter.bytes(0, methodInfo.getBody(), info);
                retInfo.popFnReturns();
                result.put(pair.getKey(), bytes);
                info.removeStackFrame();
            } finally {
                handler.removePrivateAccess(type);
                handler.removeCls();
                recursivelyRemoveProtectedAccess(handler, type);
                if (pair.getKey() == OpSpTypeNode.NEW) {
                    info.accessHandler().exitConstructor();
                }
            }
        }
        return result;
    }

    private TypeObject[] fnReturns(@NotNull FunctionInfo fnInfo) {
        if (!fnInfo.isGenerator()) {
            return fnInfo.getReturns();
        }
        var returns = fnInfo.getReturns();
        assert returns.length == 1;
        var currentReturn = returns[0];
        return Builtins.deIterable(currentReturn);
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

    protected final void putInInfo(UserType<?> type, String defType, List<String> variants,
                                   List<Short> superConstants, @NotNull ConverterHolder converter) {
        var name = node.getName().strName();
        info.checkDefinition(name, node);
        var constant = new ClassConstant(name, info.classIndex(type), type);
        info.addVariable(name, Builtins.TYPE.generify(type), constant, node);
        var cls = createClass(type, variants, superConstants, converter);
        info.setClass(cls);
        if (Builtins.FORBIDDEN_NAMES.contains(name)) {
            throw CompilerException.format("Illegal name for %s '%s'", node.getName(), defType, name);
        }
    }

    protected final void addToInfo(UserType<?> type, String defType, List<String> variants,
                                   List<Short> superConstants, @NotNull ConverterHolder converter) {
        var name = node.getName().strName();
        info.checkDefinition(name, node);
        info.reserveConstVar(name, Builtins.TYPE.generify(type), node);
        ClassInfo cls;
        try {
            info.accessHandler().addCls(type);
            info.addLocalTypes(type.getGenericInfo().getParamMap());
            cls = createClass(type, variants, superConstants, converter);
        } finally {
            info.accessHandler().removeCls();
            info.removeLocalTypes();
        }
        int classIndex = info.addClass(cls);
        if (Builtins.FORBIDDEN_NAMES.contains(name)) {
            throw CompilerException.format("Illegal name for %s '%s'", node.getName(), defType, name);
        }
        info.setReservedVar(name, new ClassConstant(name, classIndex, type));
    }

    protected final void putInInfo(UserType<?> type, String defType,
                                   List<Short> superConstants, @NotNull ConverterHolder converter) {
        putInInfo(type, defType, null, superConstants, converter);
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
            throw CompilerInternalError.format("Unknown class statement %s", stmt, stmt.getClass());
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

    protected List<Short> getSuperConstants(UserType<?> type) {
        List<Short> superConstants = new ArrayList<>(node.getSuperclasses().length);
        for (var sup : Zipper.of(node.getSuperclasses(), type.getSupers())) {
            superConstants.add(info.constIndex(info.typeConstant(sup.getKey(), sup.getValue())));
        }
        return superConstants;
    }

    protected void checkContract(UserType<?> type, @NotNull List<TypeObject> supers) {
        for (var sup : supers) {
            if (!(sup instanceof UserType<?>)) continue;

            var contract = ((UserType<?>) sup).contract();
            for (var attr : contract.getKey()) {
                if (type.attrType(attr, AccessLevel.PUBLIC).isEmpty()) {
                    throw CompilerException.format(
                            "Missing impl for method '%s' (defined by interface %s)",
                            node, attr, sup.name()
                    );
                }
            }
            for (var op : contract.getValue()) {
                if (type.operatorInfo(op, AccessLevel.PUBLIC).isEmpty()) {
                    throw CompilerException.format(
                            "Missing impl for %s (defined by interface %s)",
                            node, op, sup.name()
                    );
                }
            }
        }
    }
}
