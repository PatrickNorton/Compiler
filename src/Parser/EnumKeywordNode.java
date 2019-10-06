package Parser;

import org.jetbrains.annotations.NotNull;

public interface EnumKeywordNode extends NameNode {
    VariableNode getVariable();

    @NotNull
    static EnumKeywordNode parse(TokenList tokens) {
        NameNode t = NameNode.parse(tokens);
        if (t instanceof EnumKeywordNode) {
            return (EnumKeywordNode) t;
        } else {
            throw new ParserException("Unexpected keyword");
        }
    }
}
