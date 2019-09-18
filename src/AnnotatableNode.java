import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

public interface AnnotatableNode extends IndependentNode {
    void addAnnotations(NameNode... annotations);
    NameNode[] getAnnotations();

    @NotNull
    static AnnotatableNode parseLeftAnnotation(@NotNull TokenList tokens) {
        LinkedList<NameNode> annotations = new LinkedList<>();
        while (tokens.tokenIs(TokenType.DOLLAR)) {
            tokens.nextToken();
            annotations.add(NameNode.parse(tokens));
        }
        AnnotatableNode statement = AnnotatableNode.parse(tokens);
        if (statement.getAnnotations().length > 0) {
            throw new ParserException("Attempted to annotate twice");
        }
        statement.addAnnotations(annotations.toArray(new NameNode[0]));
        return statement;
    }

    @NotNull
    static AnnotatableNode parse(TokenList tokens) {
        IndependentNode stmt = IndependentNode.parse(tokens);
        if (stmt instanceof AnnotatableNode) {
            return (AnnotatableNode) stmt;
        } else {
            throw new ParserException("Un-annotatable statement");
        }
    }
}
