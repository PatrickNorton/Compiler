package main.java.parser;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public interface DescribableNode extends IndependentNode {

    /**
     * Add a descriptor to the node
     * @param nodes The descriptors to add
     */
    void addDescriptor(EnumSet<DescriptorNode> nodes);

    /**
     * Get the descriptors of the node
     * @return The descriptors
     */
    EnumSet<DescriptorNode> getDescriptors();

    /**
     * Get the valid descriptors for a node
     * @return The descriptors of a node
     */
    default Set<DescriptorNode> validDescriptors() {
        return EnumSet.allOf(DescriptorNode.class);
    }

    /**
     * Parse a describable node from a list of tokens.
     * @param tokens The list of tokens to be parsed
     * @return The freshly parsed DescribableNode
     */

    static DescribableNode parse(TokenList tokens) {
        EnumSet<DescriptorNode> descriptors;
        if (tokens.tokenIs(TokenType.DESCRIPTOR)) {
            descriptors = DescriptorNode.parseList(tokens);
        } else {
            descriptors = DescriptorNode.emptySet();
        }
        if (!Collections.disjoint(descriptors, DescriptorNode.MUT_NODES)) {
            return parseMutability(tokens, descriptors);
        } else {
            return finishParse(IndependentNode.parse(tokens), descriptors);
        }
    }

    private static DescribableNode parseMutability(TokenList tokens, EnumSet<DescriptorNode> descriptors) {
        assert !Collections.disjoint(descriptors, DescriptorNode.MUT_NODES);
        if (tokens.tokenIs(Keyword.VAR)) {
            return finishParse(IndependentNode.parseVar(tokens), descriptors);
        } else if (tokens.tokenIs(TokenType.KEYWORD)) {
            return finishParse(IndependentNode.parse(tokens), descriptors);
        } else if (tokens.tokenIs(TokenType.OPERATOR_SP)) {
            return finishParse(OperatorDefinitionNode.parse(tokens), descriptors);
        } else if (tokens.lineContains(TokenType.ASSIGN)) {
            return finishParse(AssignStatementNode.parse(tokens), descriptors);
        } else if (tokens.lineContains(TokenType.AUG_ASSIGN)) {
            throw tokens.error("mut cannot be used in augmented assignment");
        } else {
            return finishParse(DeclarationNode.parse(tokens), descriptors);
        }
    }

    private static DescribableNode finishParse(IndependentNode stmt, EnumSet<DescriptorNode> descriptors) {
        if (stmt instanceof DescribableNode) {
            DescribableNode statement = (DescribableNode) stmt;
            if (!statement.validDescriptors().containsAll(descriptors)) {
                throw ParserException.of(errorMessage(statement, descriptors), statement);
            }
            statement.addDescriptor(descriptors);
            return statement;
        } else {
            throw ParserException.of("Descriptor not allowed in statement", stmt);
        }
    }

    private static String errorMessage(DescribableNode stmt,EnumSet<DescriptorNode> descriptors) {
        Set<DescriptorNode> disjoint = EnumSet.copyOf(descriptors);
        disjoint.removeAll(stmt.validDescriptors());
        return "Invalid descriptor(s): " + TestNode.toString(disjoint) + " not allowed in statement";
    }
}
