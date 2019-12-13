package main.java.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TypeUnionNode implements TypeLikeNode {
    private LineInfo lineInfo;
    private TypeLikeNode[] subtypes;

    public TypeUnionNode(TypeLikeNode... types) {
        this(types[0].getLineInfo(), types);
    }

    @Contract(pure = true)
    public TypeUnionNode(LineInfo lineInfo, TypeLikeNode... types) {
        this.subtypes = types;
        this.lineInfo = lineInfo;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public TypeLikeNode[] getSubtypes() {
        return subtypes;
    }

    @Override
    public boolean isVararg() {
        return false;
    }

    @Override
    public boolean isOptional() {
        return false;
    }

    @Contract("_, _, _ -> new")
    @NotNull
    public static TypeUnionNode fromType(@NotNull TokenList tokens, TypeLikeNode first, boolean ignoreNewlines) {
        assert tokens.tokenIs("|");
        List<TypeLikeNode> joined = new ArrayList<>();
        joined.add(first);
        if (ignoreNewlines) {
            tokens.passNewlines();
        }
        while (tokens.tokenIs("|")) {
            tokens.nextToken(ignoreNewlines);
            joined.add(TypeLikeNode.parse(tokens));
        }
        return new TypeUnionNode(joined.toArray(new TypeLikeNode[0]));
    }

    public String toString() {
        return Arrays.stream(subtypes).map(Object::toString).collect(Collectors.joining("|"));
    }
}
