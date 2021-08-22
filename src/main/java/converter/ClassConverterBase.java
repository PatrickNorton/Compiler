package main.java.converter;

import main.java.converter.classbody.ConverterHolder;
import main.java.converter.classbody.RawMethod;
import main.java.parser.AnnotatableNode;
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
    protected final <U> Map<U, Method> convert(UserType<?> type, @NotNull Map<U, RawMethod> functions) {
        Map<U, Method> result = new HashMap<>();
        for (var pair : functions.entrySet()) {
            var methodInfo = pair.getValue();
            var isConstMethod = !methodInfo.isMut();
            var genericInfo = methodInfo.getInfo().getGenerics();
            if (!genericInfo.isEmpty()) {
                info.addLocalTypes(type, genericInfo.getParamMap());
            }
            info.addStackFrame();
            info.addVariable("self", isConstMethod ? type.makeConst() : type.makeMut(), isConstMethod, node);
            if (type.isFinal() && type.getGenericInfo().isEmpty()) {
                // Classes can be used as a constant sometimes!
                var constant = new ClassConstant("cls", info.classIndex(type), type);
                info.addVariable("cls", Builtins.type().generify(type), constant, node);
                info.addVariable("", Builtins.type().generify(type), node);
            } else {
                info.addVariable("cls", Builtins.type().generify(type), true, node);
            }
            var handler = info.accessHandler();
            try {
                handler.allowPrivateAccess(type);
                handler.addCls(type);
                recursivelyAllowProtectedAccess(handler, type);
                var fnInfo = methodInfo.getInfo();
                for (var arg : fnInfo.getArgs()) {
                    if (arg.isVararg()) {
                        info.addVariable(arg.getName(), Builtins.iterable().generify(arg, arg.getType()), methodInfo);
                    } else {
                        info.addVariable(arg.getName(), arg.getType(), methodInfo);
                    }
                }
                var retInfo = info.getFnReturns();
                retInfo.addFunctionReturns(fnInfo.isGenerator(), fnReturns(fnInfo));
                if (pair.getKey() == OpSpTypeNode.NEW) {
                    info.accessHandler().enterConstructor(type);
                }
                var bodyPair = BaseConverter.bytesWithReturn(0, methodInfo.getBody(), info);
                var bytes = bodyPair.getKey();
                if (endsWithoutReturning(type, fnInfo, bodyPair.getValue().willReturn())) {
                    CompilerWarning.warn("Function ends without returning", WarningType.NO_TYPE, info, methodInfo);
                }
                retInfo.popFnReturns();
                var mInfo = new MethodInfo(methodInfo.getAccessLevel(), methodInfo.isMut(), methodInfo.getInfo());
                result.put(pair.getKey(), new Method(methodInfo, mInfo, bytes));
                info.removeStackFrame();
                if (!genericInfo.isEmpty()) {
                    info.removeLocalTypes();
                }
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

    private boolean endsWithoutReturning(UserType<?> type, FunctionInfo fnInfo, boolean returns) {
        if (fnInfo.isGenerator() || fnReturns(fnInfo).length == 0 || returns) {
            return false;
        }
        var contract = type.contract();
        if (contract.getKey().contains(fnInfo.getName())) {
            return false;
        } else {
            for (var key : contract.getValue()) {
                if (key.toString().equals(fnInfo.getName())) {
                    return false;
                }
            }
            return true;
        }
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
        info.addVariable(name, Builtins.type().generify(type), constant, node);
        var cls = create(type, variants, superConstants, converter);
        info.setClass(cls);
        if (Builtins.FORBIDDEN_NAMES.contains(name)) {
            throw CompilerException.format("Illegal name for %s '%s'", node.getName(), defType, name);
        }
    }

    protected final void addToInfo(UserType<?> type, String defType, List<String> variants,
                                   List<Short> superConstants, @NotNull ConverterHolder converter) {
        var name = node.getName().strName();
        info.checkDefinition(name, node);
        info.reserveConstVar(name, Builtins.type().generify(type), node);
        var cls = create(type, variants, superConstants, converter);
        int classIndex = info.addClass(cls);
        if (Builtins.FORBIDDEN_NAMES.contains(name)) {
            throw CompilerException.format("Illegal name for %s '%s'", node.getName(), defType, name);
        }
        info.setReservedVar(name, new ClassConstant(name, classIndex, type));
    }

    private ClassInfo create(
            UserType<?> type, List<String> variants, List<Short> superConstants, @NotNull ConverterHolder converter
    ) {
        try {
            info.accessHandler().addCls(type);
            info.addLocalTypes(type, type.getGenericInfo().getParamMap());
            return createClass(type, variants, superConstants, converter);
        } finally {
            info.accessHandler().removeCls();
            info.removeLocalTypes();
        }
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
        addDerivedOperators(converter);
        for (var stmt : node.getBody().getStatements()) {
            if (!(stmt instanceof AnnotatableNode) ||
                    AnnotationConverter.shouldCompile(stmt, info, ((AnnotatableNode) stmt).getAnnotations())) {
                parseStatement(stmt, converter);
            }
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
            var constant = TypeLoader.typeConstant(sup.getKey().getLineInfo(), sup.getValue(), info);
            superConstants.add(info.constIndex(constant.orElseThrow(
                            () -> CompilerException.of("Cannot yet serialize local types", sup.getKey())
                    )
            ));
        }
        return superConstants;
    }

    protected void checkContract(UserType<?> type, @NotNull List<TypeObject> supers) {
        for (var sup : supers) {
            if (sup.sameBaseType(type) || !(sup instanceof UserType<?>)) continue;

            var contract = ((UserType<?>) sup).contract();
            for (var attr : contract.getKey()) {
                if (type.makeMut().attrType(attr, AccessLevel.PUBLIC).isEmpty()) {
                    throw CompilerException.format(
                            "Missing impl for method '%s' (defined by interface %s)",
                            node, attr, sup.name()
                    );
                }
            }
            for (var op : contract.getValue()) {
                if (type.makeMut().operatorInfo(op, AccessLevel.PUBLIC).isEmpty()) {  // FIXME: Mutability
                    throw CompilerException.format(
                            "Missing impl for %s (defined by interface %s)",
                            node, op, sup.name()
                    );
                }
            }
        }
    }

    private void addDerivedOperators(ConverterHolder converter) {
        var opConverter = converter.operators();
        var operators = AnnotationConverter.deriveOperators(node.getAnnotations());
        for (var op : operators) {
            opConverter.parseDerived(op, node.getAnnotations()[0]);
        }
    }
}
