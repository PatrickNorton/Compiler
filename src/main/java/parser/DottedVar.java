package main.java.parser;

import main.java.converter.CompilerException;

import java.util.ArrayList;
import java.util.List;

public class DottedVar implements BaseNode {
    private LineInfo lineInfo;
    private NameNode postDot;
    private String dotPrefix;

    public DottedVar(LineInfo info, String dotType, NameNode postDot) {
        this.lineInfo = info;
        this.dotPrefix = dotType;
        this.postDot = postDot;
    }

    @Override
    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public String getDotPrefix() {
        return dotPrefix;
    }

    public NameNode getPostDot() {
        return postDot;
    }

    public static DottedVar parse(TokenList tokens, boolean namesOnly, boolean ignoreNewlines) {
        assert tokens.tokenIs(TokenType.DOT);
        LineInfo info = tokens.lineInfo();
        String dotType = tokens.tokenSequence().substring(0, tokens.tokenSequence().length() - 1);
        tokens.nextToken(ignoreNewlines);
        NameNode postDot;
        if (tokens.tokenIs(TokenType.OPERATOR_SP)) {
            if (namesOnly) {
                throw tokens.defaultError();
            }
            postDot = SpecialOpNameNode.parse(tokens);
        } else if (tokens.tokenIs(TokenType.NUMBER)) {
            var number = NumberNode.parse(tokens);
            var value = number.getValue();
            try {
                postDot = new VariableNode(number.getLineInfo(), value.toBigIntegerExact().toString());
            } catch (ArithmeticException e) {
                throw CompilerException.of("Numbers as a post-dot must be whole integers", number);
            }
        } else {
            postDot = VariableNode.parse(tokens);
        }
        if (!namesOnly) {
            postDot = NameNode.parsePostBraces(tokens, postDot);
        }
        if (ignoreNewlines) {
            tokens.passNewlines();
        }
        return new DottedVar(info, dotType, postDot);
    }

    public static DottedVar[] parseAll(TokenList tokens, boolean ignoreNewlines) {
        List<DottedVar> vars = new ArrayList<>();
        while (tokens.tokenIs(TokenType.DOT)) {
            vars.add(parse(tokens, false, ignoreNewlines));
        }
        return vars.toArray(new DottedVar[0]);
    }

    @Override
    public String toString() {
        return dotPrefix + "." + postDot;
    }
}
