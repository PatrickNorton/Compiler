package main.java.converter.classbody;

import main.java.converter.AccessLevel;
import main.java.converter.FunctionInfo;
import main.java.parser.LineInfo;
import main.java.parser.Lined;
import main.java.parser.StatementBodyNode;

public final class Method implements Lined {
    private final AccessLevel accessLevel;
    private final boolean isMut;
    private final FunctionInfo info;
    private final StatementBodyNode body;
    private final LineInfo lineInfo;

    public Method(AccessLevel access, FunctionInfo info, StatementBodyNode body, LineInfo lineInfo) {
        this(access, false, info, body, lineInfo);
    }

    public Method(AccessLevel access, boolean isMut, FunctionInfo info, StatementBodyNode body, LineInfo lineInfo) {
        this.accessLevel = access;
        this.isMut = isMut;
        this.info = info;
        this.body = body;
        this.lineInfo = lineInfo;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public boolean isMut() {
        return isMut;
    }

    public FunctionInfo getInfo() {
        return info;
    }

    public StatementBodyNode getBody() {
        return body;
    }

    public LineInfo getLineInfo() {
        return lineInfo;
    }
}
