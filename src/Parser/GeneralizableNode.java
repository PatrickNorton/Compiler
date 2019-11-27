package Parser;

import org.jetbrains.annotations.NotNull;

public interface GeneralizableNode extends BaseNode, DescribableNode {
    TypeLikeNode[] getGenerics();
    void addGenerics(TypeLikeNode... types);

    @NotNull
    static GeneralizableNode parse(@NotNull TokenList tokens) {
        assert tokens.tokenIs(Keyword.GENERIC);
        tokens.nextToken();
        TypeLikeNode[] types = TypeLikeNode.parseList(tokens);
        tokens.passNewlines();
        switch (tokens.tokenType()) {
            case DESCRIPTOR:
                throw tokens.error("Generic statement may not come before descriptors");
            case DOLLAR:
                throw tokens.error("Generic statement may not precede annotations");
            case AT:
                throw tokens.error("Generic statement may not precede decorations");
        }
        IndependentNode generalized = IndependentNode.parse(tokens);
        if (generalized instanceof GeneralizableNode) {
            GeneralizableNode node = (GeneralizableNode) generalized;
            node.addGenerics(types);
            return node;
        } else {
            throw ParserException.of("Attempted to generalize non-generalizable node", generalized.getLineInfo());
        }
    }

    @NotNull
    static String toString(@NotNull TypeLikeNode... generics) {
        if (generics.length == 0) {
            return "";
        } else {
            return "generic " + TestNode.toString(generics) + " ";
        }
    }
}
