package NameResolver;

import Parser.BaseNode;
import Parser.BodyNode;
import Parser.ImportStatementNode;
import Parser.IndependentNode;
import Parser.NameNode;
import Parser.TopNode;

import java.util.LinkedList;

public class Resolver {
    private final ImportStatementNode[] imports;
    private final TopNode resolved;

    private Resolver(TopNode resolved) {
        this.resolved = resolved;
        final LinkedList<ImportStatementNode> median = new LinkedList<>();
        for (IndependentNode i : resolved) {
            if (i instanceof ImportStatementNode) {
                median.add((ImportStatementNode) i);
            }
        }
        this.imports = median.toArray(new ImportStatementNode[0]);
    }

    public static void resolve(TopNode t) {
        Resolver r = new Resolver(t);
        r.resolve();
    }

    public void resolve() {

    }

}
