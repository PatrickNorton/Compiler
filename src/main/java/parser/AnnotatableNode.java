package main.java.parser;

import java.util.LinkedList;

/**
 * The interface representing an node which can be preceded by an annotation.
 *
 * @author Patrick Norton
 */
public interface AnnotatableNode extends IndependentNode {
    void addAnnotations(NameNode... annotations);
    NameNode[] getAnnotations();

    /**
     * Parse annotations and the annotated node from a list of tokens.
     * <p>
     *     The syntax for an annotation is <code>"$" {@link NameNode}</code>.
     * </p>
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed AnnotatableNode, with annotations added
     */

    static AnnotatableNode parseLeftAnnotation(TokenList tokens) {
        assert tokens.tokenIs(TokenType.DOLLAR);
        LinkedList<NameNode> annotations = new LinkedList<>();
        while (tokens.tokenIs(TokenType.DOLLAR)) {
            tokens.nextToken();
            annotations.add(NameNode.parse(tokens));
            tokens.passNewlines();
        }
        AnnotatableNode statement = AnnotatableNode.parse(tokens);
        if (statement.getAnnotations().length > 0) {
            throw tokens.error("Attempted to annotate twice");
        }
        statement.addAnnotations(annotations.toArray(new NameNode[0]));
        return statement;
    }

    /**
     * Parse an annotatable node from a list of tokens.
     * @param tokens The list of tokens to be destructively parsed
     * @return The freshly parsed AnnotatableNode
     */

    static AnnotatableNode parse(TokenList tokens) {
        IndependentNode stmt = IndependentNode.parse(tokens);
        if (stmt instanceof AnnotatableNode) {
            return (AnnotatableNode) stmt;
        } else {
            throw ParserException.of("Attempted to annotate un-annotatable statement", stmt);
        }
    }
}
