package main.java.converter.classbody;

import main.java.converter.FunctionInfo;
import main.java.parser.DescriptorNode;
import main.java.parser.LineInfo;
import main.java.parser.Lined;
import main.java.parser.StatementBodyNode;

import java.util.Set;

public final class MethodInfo implements Lined {
    private final Set<DescriptorNode> descriptors;
        private final FunctionInfo info;
        private final StatementBodyNode body;
        private final LineInfo lineInfo;

        public MethodInfo(Set<DescriptorNode> descriptors, FunctionInfo info, StatementBodyNode body, LineInfo lineInfo) {
            this.descriptors = descriptors;
            this.info = info;
            this.body = body;
            this.lineInfo = lineInfo;
        }

        public Set<DescriptorNode> getDescriptors() {
            return descriptors;
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
