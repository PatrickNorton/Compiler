package main.java.converter;

import main.java.parser.AnnotatableNode;
import main.java.parser.AssertStatementNode;
import main.java.parser.AssignmentNode;
import main.java.parser.AugmentedAssignmentNode;
import main.java.parser.BaseNode;
import main.java.parser.BreakStatementNode;
import main.java.parser.ClassDefinitionNode;
import main.java.parser.ContinueStatementNode;
import main.java.parser.DeclarationNode;
import main.java.parser.DeclaredAssignmentNode;
import main.java.parser.DecrementNode;
import main.java.parser.DeleteStatementNode;
import main.java.parser.DoStatementNode;
import main.java.parser.DotimesStatementNode;
import main.java.parser.EnumDefinitionNode;
import main.java.parser.ForStatementNode;
import main.java.parser.FunctionDefinitionNode;
import main.java.parser.IfStatementNode;
import main.java.parser.ImportExportNode;
import main.java.parser.IncrementNode;
import main.java.parser.InterfaceDefinitionNode;
import main.java.parser.ReturnStatementNode;
import main.java.parser.StatementBodyNode;
import main.java.parser.TestNode;
import main.java.parser.TryStatementNode;
import main.java.parser.TypedefStatementNode;
import main.java.parser.UnionDefinitionNode;
import main.java.parser.WhileStatementNode;
import main.java.parser.WithStatementNode;
import main.java.parser.YieldStatementNode;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface BaseConverter {
    @NotNull
    List<Byte> convert(int start);

    @NotNull
    default BytecodeList convert() {
        return new BytecodeList();
    }

    @NotNull
    default Pair<List<Byte>, DivergingInfo> convertAndReturn(int start) {
        return Pair.of(convert(start), new DivergingInfo());
    }

    @NotNull
    default Pair<BytecodeList, DivergingInfo> convertAndReturn() {
        return Pair.of(convert(), new DivergingInfo());
    }

    @NotNull
    static List<Byte> bytes(int start, BaseNode tokens, CompilerInfo info) {
        return toBytes(tokens, info).convert(start);
    }

    @NotNull
    static BytecodeList bytes(BaseNode tokens, CompilerInfo info) {
        return toBytes(tokens, info).convert();
    }

    @NotNull
    static BytecodeList bytesWithoutAnnotations(AnnotatableNode tokens, CompilerInfo info) {
        return withoutAnnotations(tokens, info).convert();
    }

    static Pair<List<Byte>, DivergingInfo> bytesWithReturn(int start, BaseNode tokens, CompilerInfo info) {
        return toBytes(tokens, info).convertAndReturn(start);
    }

    @NotNull
    static Pair<BytecodeList, DivergingInfo> bytesWithReturn(BaseNode tokens, CompilerInfo info) {
        return toBytes(tokens, info).convertAndReturn();
    }

    @NotNull
    private static BaseConverter toBytes(@NotNull BaseNode node, CompilerInfo info) {
        if (node instanceof AnnotatableNode) {
            return new AnnotationConverter(info, (AnnotatableNode) node);
        } else if (node instanceof TestNode) {
            return TestConverter.of(info, (TestNode) node, 0);
        } else if (node instanceof AssertStatementNode) {
            return new AssertConverter(info, (AssertStatementNode) node);
        } else if (node instanceof AssignmentNode) {
            return new AssignmentConverter(info, (AssignmentNode) node);
        } else if (node instanceof AugmentedAssignmentNode) {
            return new AugAssignConverter(info, (AugmentedAssignmentNode) node);
        } else if (node instanceof BreakStatementNode) {
            return new BreakConverter(info, (BreakStatementNode) node);
        } else if (node instanceof ContinueStatementNode) {
            return new ContinueConverter(info, (ContinueStatementNode) node);
        } else if (node instanceof DecrementNode) {
            return new IncrementDecrementConverter(info, (DecrementNode) node);
        } else if (node instanceof DeleteStatementNode) {
            return new DeleteConverter(info, (DeleteStatementNode) node);
        } else if (node instanceof DerivedOperatorNode) {
            return new DerivedOperatorConverter(info, (DerivedOperatorNode) node);
        } else if (node instanceof DoStatementNode) {
            return new DoWhileConverter(info, (DoStatementNode) node);
        } else if (node instanceof DotimesStatementNode) {
            return new DotimesConverter(info, (DotimesStatementNode) node);
        } else if (node instanceof ForStatementNode) {
            return new ForConverter(info, (ForStatementNode) node);
        } else if (node instanceof IfStatementNode) {
            return new IfConverter(info, (IfStatementNode) node);
        } else if (node instanceof ImportExportNode) {
            return new ImportExportConverter(info, (ImportExportNode) node);
        } else if (node instanceof IncrementNode) {
            return new IncrementDecrementConverter(info, (IncrementNode) node);
        } else if (node instanceof ReturnStatementNode) {
            return new ReturnConverter(info, (ReturnStatementNode) node);
        } else if (node instanceof StatementBodyNode) {
            return new BodyConverter(info, (StatementBodyNode) node);
        } else if (node instanceof TryStatementNode) {
            return new TryConverter(info, (TryStatementNode) node);
        } else if (node instanceof TypedefStatementNode) {
            return new TypedefConverter(info, (TypedefStatementNode) node);
        } else if (node instanceof WhileStatementNode) {
            return new WhileConverter(info, (WhileStatementNode) node);
        } else if (node instanceof WithStatementNode) {
            return new WithConverter(info, (WithStatementNode) node);
        } else if (node instanceof YieldStatementNode) {
            return new YieldConverter(info, (YieldStatementNode) node);
        } else {
            throw CompilerTodoError.of("Unsupported node", node);
        }
    }

    private static BaseConverter withoutAnnotations(AnnotatableNode node, CompilerInfo info) {
        if (node instanceof TestNode) {
            return TestConverter.of(info, (TestNode) node, 0);
        } else if (node instanceof ClassDefinitionNode) {
            return new ClassConverter(info, (ClassDefinitionNode) node);
        } else if (node instanceof DeclarationNode) {
            return new DeclarationConverter(info, (DeclarationNode) node);
        } else if (node instanceof DeclaredAssignmentNode) {
            return new DeclaredAssignmentConverter(info, (DeclaredAssignmentNode) node);
        } else if (node instanceof EnumDefinitionNode) {
            return new EnumConverter(info, (EnumDefinitionNode) node);
        } else if (node instanceof FunctionDefinitionNode) {
            return new FunctionDefinitionConverter(info, (FunctionDefinitionNode) node);
        } else if (node instanceof InterfaceDefinitionNode) {
            return new InterfaceConverter(info, (InterfaceDefinitionNode) node);
        }  else if (node instanceof UnionDefinitionNode) {
            return new UnionConverter(info, (UnionDefinitionNode) node);
        } else {
            throw CompilerTodoError.of("Unsupported node", node);
        }
    }
}
