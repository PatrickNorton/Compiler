package main.java.converter;

import main.java.parser.BaseNode;
import main.java.parser.BreakStatementNode;
import main.java.parser.ContinueStatementNode;
import main.java.parser.DeclaredAssignmentNode;
import main.java.parser.FunctionDefinitionNode;
import main.java.parser.IfStatementNode;
import main.java.parser.ImportExportNode;
import main.java.parser.ReturnStatementNode;
import main.java.parser.TestNode;
import main.java.parser.WhileStatementNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface BaseConverter {
    List<Byte> convert(int start);

    static List<Byte> bytes(int start, BaseNode tokens, CompilerInfo info) {
        return toBytes(tokens, info).convert(start);
    }

    @NotNull
    private static BaseConverter toBytes(@NotNull BaseNode node, CompilerInfo info) {
        if (node instanceof TestNode) {
            return TestConverter.of(info,  (TestNode) node);
        } else if (node instanceof BreakStatementNode) {
            return new BreakConverter(info, (BreakStatementNode) node);
        } else if (node instanceof ContinueStatementNode) {
            return new ContinueConverter(info, (ContinueStatementNode) node);
        } else if (node instanceof DeclaredAssignmentNode) {
            return new DeclaredAssignmentConverter(info, (DeclaredAssignmentNode) node);
        } else if (node instanceof FunctionDefinitionNode) {
            return new FunctionDefinitionConverter(info, (FunctionDefinitionNode) node);
        } else if (node instanceof IfStatementNode) {
            return new IfConverter(info, (IfStatementNode) node);
        } else if (node instanceof ImportExportNode) {
            return new ImportExportConverter(info, (ImportExportNode) node);
        } else if (node instanceof ReturnStatementNode) {
            return new ReturnConverter(info, (ReturnStatementNode) node);
        } else if (node instanceof WhileStatementNode) {
            return new WhileConverter(info, (WhileStatementNode) node);
        } else {
            throw new UnsupportedOperationException("Unsupported node");
        }
    }
}
