import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * The interface for assignment, whether it be declared or otherwise.
 * <p>
 * This also contains the parser creator method for both of its subclasses, as
 * they are difficult to tell apart early in parsing, but they are very
 * different in compilation.
 * </p>
 *
 * @author Patrick Norton
 * @see AssignmentNode
 * @see DeclaredAssignmentNode
 */
public interface AssignStatementNode extends SimpleStatementNode {
    /**
     * AssignStatementNodes must have something that is assigned to them
     * @return list of assigned names
     */
    NameNode[] getName();

    /**
     * Parse an assignment statement from a list of tokens.
     * <p>
     *     This uses analysis of the code as it parses in order to determine
     *     whether or not the assignment was a declaration and initialization,
     *     or if it was simply assignment. This is difficult to tell at parse
     *     time, simply because there can be only a comma difference between a
     *     syntactically valid assignment and declared assignment.
     * </p>
     * @param tokens The tokens to be parsed. Removes tokens which are turned
     *               into the node.
     * @return The statement that was parsed out
     */
    @NotNull
    @Contract("_ -> new")
    static AssignStatementNode parse(@NotNull TokenList tokens) {
        ArrayList<TypeNode> var_type = new ArrayList<>();
        ArrayList<NameNode> vars = new ArrayList<>();
        while (!tokens.tokenIs(TokenType.ASSIGN)) {
            if (!tokens.getToken(tokens.sizeOfVariable()).is(TokenType.ASSIGN, TokenType.COMMA)) {
                var_type.add(TypeNode.parse(tokens));
                vars.add(DottedVariableNode.parseName(tokens));
                if (tokens.tokenIs(TokenType.ASSIGN)) {
                    break;
                }
                if (!tokens.tokenIs(TokenType.COMMA)) {
                    throw new ParserException("Expected comma, got " + tokens.getFirst());
                }
                tokens.nextToken();
            } else {
                var_type.add(new TypeNode(new DottedVariableNode()));
                vars.add(DottedVariableNode.parseName(tokens));
                if (tokens.tokenIs(TokenType.ASSIGN)) {
                    break;
                }
                if (!tokens.tokenIs(TokenType.COMMA)) {
                    throw new ParserException("Expected comma, got " + tokens.getFirst());
                }
                tokens.nextToken();
            }
        }
        boolean is_colon = tokens.tokenIs(":=");
        tokens.nextToken();
        TestNode[] assignments = TestNode.parseList(tokens, false);
        tokens.Newline();
        TypeNode[] type_array = var_type.toArray(new TypeNode[0]);
        NameNode[] vars_array = vars.toArray(new NameNode[0]);
        boolean is_declared = false;
        for (TypeNode type : var_type) {
            if (!type.getName().isEmpty()) {
                is_declared = true;
                break;
            }
        }
        if (is_declared) {
            return new DeclaredAssignmentNode(is_colon, type_array, vars_array, assignments);
        } else {
            return new AssignmentNode(is_colon, vars_array, assignments);
        }
    }
}
