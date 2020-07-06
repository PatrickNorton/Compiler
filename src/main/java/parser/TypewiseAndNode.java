package main.java.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class TypewiseAndNode implements TypeLikeNode {
    private LineInfo lineInfo;
    private TypeLikeNode[] subtypes;

    @Contract(pure = true)
    public TypewiseAndNode(TypeLikeNode... subtypes) {
        this(subtypes[0].getLineInfo(), subtypes);
    }

    @Contract(pure = true)
    public TypewiseAndNode(LineInfo lineInfo, TypeLikeNode... subtypes) {
        this.lineInfo = lineInfo;
        this.subtypes = subtypes;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    @Override
    public TypeLikeNode[] getSubtypes() {
        return subtypes;
    }

    @Override
    public boolean isOptional() {
        return false;
    }

    @Override
    public boolean isVararg() {
        return false;
    }

    @Override
    public String strName() {
        StringJoiner sj = new StringJoiner("&");
        for (TypeLikeNode node : subtypes) {
            sj.add(node.strName());
        }
        return sj.toString();
    }

    @Override
    public void setMutability(DescriptorNode node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DescriptorNode> getMutability() {
        return Optional.empty();
    }

    @NotNull
    @Contract("_, _, _ -> new")
    public static TypewiseAndNode fromType(@NotNull TokenList tokens, TypeLikeNode first, boolean ignoreNewlines) {
        assert tokens.tokenIs("&");
        List<TypeLikeNode> joined = new ArrayList<>();
        joined.add(first);
        if (ignoreNewlines) {
            tokens.passNewlines();
        }
        while (tokens.tokenIs("&")) {
            tokens.nextToken(ignoreNewlines);
            joined.add(TypeLikeNode.parse(tokens));
        }
        return new TypewiseAndNode(joined.toArray(new TypeLikeNode[0]));
    }

    public String toString() {
        return Arrays.stream(subtypes).map(Object::toString).collect(Collectors.joining("&"));
    }
}
