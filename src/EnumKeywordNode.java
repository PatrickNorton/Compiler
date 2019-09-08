import org.jetbrains.annotations.NotNull;

public interface EnumKeywordNode extends NameNode {
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
