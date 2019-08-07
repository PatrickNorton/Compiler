// TODO! Rename getName() to not conflict with standard name
// TODO: Reduce/remove nulls
// TODO: Casting
// TODO? Replace StatementBodyNode with BaseNode[] (and equivalents)


import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

public class Parser {
    private LinkedList<Token> tokens;
    private Token lookahead;

    public TopNode parse(LinkedList<Token> tokens) {  // Should this be a static method?
        this.tokens = new LinkedList<>(tokens);
        this.lookahead = tokens.getFirst();
        TopNode topNode = new TopNode();
        while (!lookahead.is(Token.EPSILON)) {
            topNode.add(statement());
        }
        return topNode;
    }

    private boolean contains(boolean braces, int... question) {
        if (braces) {
            return braceContains(question);
        } else {
            return lineContains(question);
        }
    }

    private boolean contains(boolean braces, String... question) {
        if (braces) {
            return braceContains(question);
        } else {
            return lineContains(question);
        }
    }

    private boolean lineContains(int... question) {
        int netBraces = 0;
        for (Token token : tokens) {
            if (token.is(Token.OPEN_BRACE)) {
                netBraces++;
            } else if (token.is(Token.CLOSE_BRACE)) {
                netBraces--;
            }
            if (netBraces == 0 && token.is(Token.NEWLINE)) {
                return false;
            }
            if (netBraces == 0 && token.is(question)) {
                return true;
            }
        }
        return false;
    }

    private boolean lineContains(String... question) {
        for (Token token : tokens) {
            if (token.is(Token.NEWLINE)) {
                return false;
            }
            if (token.is(question)) {
                return true;
            }
        }
        return false;
    }

    private LinkedList<Token> firstLevel() {
        LinkedList<Token> tokens = new LinkedList<>();
        int netBraces = lookahead.is(Token.OPEN_BRACE) ? 0 : 1;
        for (Token token : this.tokens) {
            if (token.is(Token.OPEN_BRACE)) {
                netBraces++;
            }
            if (token.is(Token.CLOSE_BRACE)) {
                netBraces--;
            }
            if (netBraces == 0) {
                return tokens;
            }
            if (token.is(Token.EPSILON)) {
                throw new ParserException("Unmatched brace");
            }
            if (netBraces == 1) {
                tokens.add(token);
            }
        }
        throw new RuntimeException("You shouldn't have ended up here");
    }

    private boolean braceContains(int... question) {
        for (Token token : firstLevel()) {
            if (token.is(question)) {
                return true;
            }
        }
        return false;
    }

    private boolean braceContains(String... question) {
        for (Token token : firstLevel()) {
            if (token.is(question)) {
                return true;
            }
        }
        return false;
    }

    private int sizeOfVariable() {
        return sizeOfVariable(0);
    }

    private int sizeOfVariable(int offset) {
        assert lookahead.is(Token.VARIABLE);
        if (tokens.get(offset + 1).is("[")) {
            int netBraces = 1;
            int numTokens = offset + 2;
            while (netBraces != 0) {
                Token token = tokens.get(numTokens);
                if (token.is(Token.OPEN_BRACE)) {
                    netBraces++;
                } else if (token.is(Token.CLOSE_BRACE)) {
                    netBraces--;
                } else if (token.is(Token.EPSILON)) {
                    throw new ParserException("Unexpected EOF while parsing");
                }
                numTokens++;
            }
            return numTokens - offset;
        } else {
            return 1;
        }
    }

    private int sizeOfBrace(int offset) {
        int netBraces = 0;
        int size = offset;
        for (Token token : tokens) {
            if (token.is(Token.OPEN_BRACE)) {
                netBraces++;
            } else if (token.is(Token.CLOSE_BRACE)) {
                netBraces--;
            }
            size++;
            if (netBraces == 0) {
                break;
            }
        }
        return size;
    }

    private void NextToken(Boolean ignore_newline) {
        tokens.pop();
        if (ignore_newline) {
            passNewlines();
        }
        if (tokens.isEmpty()) {
            lookahead = new Token(Token.EPSILON, "");
        } else {
            lookahead = tokens.getFirst();
        }
    }

    private void NextToken() {
        NextToken(false);
    }

    private void Newline() {
        if (!lookahead.is(Token.NEWLINE)) {
            throw new ParserException("Expected newline, got "+lookahead.sequence);
        }
        NextToken();
    }

    private void passNewlines() {
        while (!tokens.isEmpty() && tokens.getFirst().is(Token.NEWLINE)) {
            NextToken(false);
        }
    }

    private BaseNode statement() {
        passNewlines();
        switch (lookahead.token) {
            case Token.KEYWORD:
                return keyword();
            case Token.DESCRIPTOR:
                return descriptor();
            case Token.OPEN_BRACE:
                return openBrace();
            case Token.CLOSE_BRACE:
                throw new ParserException("Unmatched close brace");
            case Token.SELF_CLS:
            case Token.VARIABLE:
                return left_variable();
            case Token.COMMA:
                throw new ParserException("Unexpected comma");
            case Token.AUG_ASSIGN:
                throw new ParserException("Unexpected operator");
            case Token.OPERATOR:
                return left_operator(false);
            case Token.ASSIGN:
                throw new ParserException("Unexpected assignment");
            case Token.STRING:
                return string();
            case Token.BOOL_OP:
                return left_bool_op(false);
            case Token.INTEGER:
                return integer();
            case Token.OPERATOR_SP:
                return operator_sp();
            case Token.OP_FUNC:
                return op_func();
            case Token.COLON:
                throw new ParserException("Unexpected colon");
            case Token.ELLIPSIS:
                return ellipsis();
            default:
                throw new RuntimeException("Nonexistent token found");
        }
    }

    private BaseNode keyword() {
        switch (lookahead.sequence) {
            case "class":
                if (tokens.get(1).is(Token.DESCRIPTOR, Token.KEYWORD)) {
                    return descriptor();
                }
                return class_def();
            case "func":
                return func_def();
            case "if":
                return if_stmt();
            case "for":
                return for_stmt();
            case "else":
                throw new ParserException("else must have a preceding if");
            case "do":
                return do_stmt();
            case "dotimes":
                return dotimes_stmt();
            case "method":
                return method_def();
            case "while":
                return while_stmt();
            case "in":
                throw new ParserException("in must have a preceding token");
            case "from":
                return left_from();
            case "import":
                return import_stmt();
            case "export":
                return export_stmt();
            case "typeget":
                return typeget_stmt();
            case "break":
                return break_stmt();
            case "continue":
                return continue_stmt();
            case "return":
                return return_stmt();
            case "property":
                return property_stmt();
            case "get":
            case "set":
                throw new ParserException("get and set must be in a property block");
            case "lambda":
                return test();
            case "context":
                return context_def();
            case "enter":
            case "exit":
                throw new ParserException("enter and exit must be in a context block");
            case "try":
                return try_stmt();
            case "except":
            case "finally":
                throw new ParserException("except and finally must come after a try");
            case "with":
                return with_stmt();
            case "as":
                throw new ParserException("as must come in a with statement");
            case "assert":
                return assert_stmt();
            case "del":
                return del_stmt();
            case "yield":
                return yield_stmt();
            case "raise":
                return raise_stmt();
            case "typedef":
                return typedef_stmt();
            case "some":
                return some_op();
            case "interface":
                return interface_def();
            default:
                throw new RuntimeException("Keyword mismatch");
        }
    }

    private BaseNode descriptor() {
        ArrayList<DescriptorNode> descriptors = new ArrayList<>();
        if (lookahead.is("class")) {
            descriptors.add(new DescriptorNode("class"));
            NextToken();
        }
        while (lookahead.is(Token.DESCRIPTOR)) {
            descriptors.add(new DescriptorNode(lookahead.sequence));
            NextToken();
        }
        switch (lookahead.token) {
            case Token.KEYWORD:
                return descriptor_keyword(descriptors);
            case Token.OPERATOR:
                return descriptor_op_sp(descriptors);
            case Token.VARIABLE:
                return descriptor_var(descriptors);
            default:
                throw new ParserException("Invalid descriptor placement");
        }
    }

    private BaseNode openBrace() {
        // Types of brace statement: comprehension, literal, grouping paren
        if (lookahead.is("(")) {
            if (braceContains("for")) {
                return comprehension();
            } else if (braceContains(",")) {
                return literal();
            } else {
                NextToken(true);
                TestNode contained = test(true);
                if (!lookahead.is(")")) {
                    throw new ParserException("Unmatched brace");
                }
                NextToken();
                return contained;
            }
        } else if (lookahead.is("[")) {
            if (braceContains("for")) {
                return comprehension();
            } else {
                return literal();
            }
        } else if (lookahead.is("{")) {
            if (braceContains("for")) {
                return comprehension();
            } else if (braceContains(":")) {
                return dict_comprehension();
            } else {
                return literal();
            }
        } else {
            throw new RuntimeException("Some unknown brace was found");
        }
    }

    private SubTestNode subtest_openBrace() {
        BaseNode node = openBrace();
        if (node instanceof SubTestNode) {
            return (SubTestNode) node;
        }
        throw new ParserException("Unexpected " + lookahead.sequence);
    }

    private VariableNode variable(boolean allow_dotted) {
        if (!lookahead.is(Token.VARIABLE) && !lookahead.is(Token.SELF_CLS)) {
            throw new ParserException("Expected variable, got " + lookahead.sequence);
        }
        if (!allow_dotted && lookahead.sequence.contains(".")) {
            throw new ParserException("Dotted variables are not allowed here");
        }
        String[] tokens = lookahead.sequence.split("\\.");
        NextToken();
        return new VariableNode(tokens);
    }

    private VariableNode variable() {
        return variable(true);
    }

    private BaseNode left_variable() {
        // Things starting with a variable token:
        // Function call
        // Lone variable, just sitting there
        // Declaration
        // Declared assignment
        // Assignment
        // Lone expression
        Token after_var = tokens.get(sizeOfVariable());
        if (lineContains(Token.ASSIGN)) {
            return assignment();
        } else if (lineContains(Token.AUG_ASSIGN)) {
            VariableNode var = var_or_index();
            if (!lookahead.is(Token.AUG_ASSIGN)) {
                throw new ParserException("Expected augmented assignment, got " + lookahead.sequence);
            }
            OperatorNode op = new OperatorNode(lookahead.sequence.replaceAll("=$", ""));
            NextToken();
            TestNode assignment = test();
            return new AugmentedAssignmentNode(op, var, assignment);
        } else if (lineContains("(")) {
            return function_call();
        } else if (after_var.is("++", "--")) {
            return inc_or_dec();
        } else if (lineContains(Token.BOOL_OP, Token.OPERATOR)) {
            return test();
        } else if (after_var.is(Token.VARIABLE, Token.SELF_CLS)) {
            return declaration();
        } else {
            VariableNode var = var_or_index();
            Newline();
            return var;
        }
    }

    private AssignStatementNode assignment() {
        ArrayList<TypeNode> var_type = new ArrayList<>();
        ArrayList<VariableNode> vars = new ArrayList<>();
        while (!lookahead.is(Token.ASSIGN)) {
            if (!tokens.get(sizeOfVariable()).is(Token.ASSIGN, Token.COMMA)) {
                var_type.add(type());
                vars.add(var_or_index());
                if (lookahead.is(Token.ASSIGN)) {
                    break;
                }
                if (!lookahead.is(Token.COMMA)) {
                    throw new ParserException("Expected comma, got " + lookahead.sequence);
                }
                NextToken();
            } else {
                var_type.add(new TypeNode(""));
                vars.add(var_or_index());
                if (lookahead.is(Token.ASSIGN)) {
                    break;
                }
                if (!lookahead.is(Token.COMMA)) {
                    throw new ParserException("Expected comma, got " + lookahead.sequence);
                }
                NextToken();
            }
        }
        boolean is_colon = lookahead.is(":=");
        NextToken();
        TestNode[] assignments = test_list(false);
        Newline();
        TypeNode[] type_array = var_type.toArray(new TypeNode[0]);
        VariableNode[] vars_array = vars.toArray(new VariableNode[0]);
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

    private DeclaredAssignmentNode decl_assignment() {
        AssignStatementNode assign = assignment();
        if (assign instanceof DeclaredAssignmentNode) {
            return (DeclaredAssignmentNode) assign;
        }
        throw new ParserException("Expected declared assignment, got normal assignment");
    }

    private SubTestNode left_operator(boolean ignore_newline) {
        assert lookahead.is(Token.OPERATOR);
        if (!lookahead.is("-")) {
            throw new ParserException("- is the only unary operator");
        }
        NextToken();
        TestNode next = test(ignore_newline);
        return new OperatorNode("-", next);
    }

    private StringNode string() {
        return new StringNode(lookahead.sequence.replaceFirst("\"", ""));
    }

    private OperatorNode left_bool_op(boolean ignore_newline) {
        switch (lookahead.sequence) {
            case "not":
                NextToken(ignore_newline);
                return new OperatorNode("not", test());
            case "and":
            case "or":
            case "xor":
                throw new ParserException(lookahead.sequence+" must be in between statements");
            default:
                throw new RuntimeException("Unknown boolean operator");
        }
    }

    private OperatorDefinitionNode operator_sp() {
        String op_code = lookahead.sequence.replaceFirst("operator +", "");
        NextToken();
        if (lookahead.is(Token.ASSIGN)) {
            NextToken();
            if (lookahead.is(Token.OPERATOR_SP)) {
                OperatorTypeNode op = new OperatorTypeNode(lookahead.sequence);
                NextToken();
                Newline();
                return new OperatorDefinitionNode(op_code, new StatementBodyNode(op));
            } else if (lookahead.is(Token.VARIABLE, Token.SELF_CLS)) {
                VariableNode var = var_or_index();
                Newline();
                return new OperatorDefinitionNode(op_code, new StatementBodyNode(var));
            } else {
                throw new ParserException("Operator equivalence must be done to another var or op");
            }
        }
        TypedArgumentListNode args;
        if (lookahead.is("(")) {
            args = fn_args();
        } else {
            args = new TypedArgumentListNode();
        }
        TypeNode[] retval;
        if (lookahead.is("->")) {
            retval = fn_retval();
        } else {
            retval = new TypeNode[0];
        }
        StatementBodyNode body = fn_body();
        return new OperatorDefinitionNode(op_code, retval, args, body);
    }

    private IntegerNode integer() {
        BigInteger val = new BigInteger(lookahead.sequence);
        NextToken();
        return new IntegerNode(val);
    }

    private SubTestNode op_func() {
        String op_code = lookahead.sequence.replaceFirst("\\\\", "");
        NextToken();
        if (lookahead.is("(")) {
            return new OperatorNode(op_code, fn_args());
        } else {
            return new OperatorTypeNode(op_code);
        }
    }

    private VariableNode ellipsis() {
        assert lookahead.is(Token.ELLIPSIS);
        NextToken();
        return new VariableNode("...");
    }

    private ClassDefinitionNode class_def() {
        assert lookahead.is("class");
        NextToken();
        if (!lookahead.is(Token.VARIABLE) && !lookahead.is("from")) {
            throw new ParserException("class keyword must be followed by class name");
        }
        TypeNode name = type();
        LinkedList<TypeNode> superclasses = new LinkedList<>();
        while (lookahead.is("from")) {
            NextToken();
            superclasses.add(type());
        }
        return new ClassDefinitionNode(name, superclasses.toArray(new TypeNode[0]), class_body());
    }

    private FunctionDefinitionNode func_def() {
        assert lookahead.is("func");
        NextToken();
        VariableNode name = fn_like_name();
        TypedArgumentListNode args = fn_args();
        TypeNode[] retval = fn_retval();
        StatementBodyNode body = fn_body();
        Newline();
        return new FunctionDefinitionNode(name, args, retval, body);
    }

    private IfStatementNode if_stmt() {
        assert lookahead.is("if");
        NextToken();
        TestNode test = test();
        StatementBodyNode body = fn_body();
        LinkedList<ElifStatementNode> elifs = new LinkedList<>();
        while (lookahead.is("elif")) {
            NextToken();
            TestNode elif_test = test();
            StatementBodyNode elif_body = fn_body();
            elifs.add(new ElifStatementNode(elif_test, elif_body));
        }
        StatementBodyNode else_stmt = new StatementBodyNode();
        if (lookahead.is("else")) {
            NextToken();
            else_stmt = fn_body();
        }
        Newline();
        return new IfStatementNode(test, body, elifs.toArray(new ElifStatementNode[0]), else_stmt);
    }

    private ForStatementNode for_stmt() {
        assert lookahead.is("for");
        NextToken();
        TypedVariableNode[] vars = for_vars();
        if (!lookahead.is("in")) {
            throw new ParserException("Expected in, got "+lookahead.sequence);
        }
        NextToken();
        TestNode[] iterables = for_iterables();
        StatementBodyNode body = fn_body();
        StatementBodyNode nobreak = new StatementBodyNode();
        if (lookahead.is("nobreak")) {
            NextToken();
            nobreak = new StatementBodyNode(fn_body());
        }
        Newline();
        return new ForStatementNode(vars, iterables, body, nobreak);
    }

    private DoStatementNode do_stmt() {
        assert lookahead.is("do");
        NextToken();
        StatementBodyNode body = fn_body();
        if (!lookahead.is("while")) {
            throw new ParserException("Do statements must have a corresponding while");
        }
        NextToken();
        TestNode conditional = test();
        Newline();
        return new DoStatementNode(body, conditional);
    }

    private DotimesStatementNode dotimes_stmt() {
        assert lookahead.is("dotimes");
        NextToken();
        TestNode iterations = test();
        StatementBodyNode body = fn_body();
        StatementBodyNode nobreak = new StatementBodyNode();
        if (lookahead.is("nobreak")) {
            NextToken();
            nobreak = new StatementBodyNode(fn_body());
        }
        Newline();
        return new DotimesStatementNode(iterations, body, nobreak);
    }

    private MethodDefinitionNode method_def() {
        assert lookahead.is("method");
        NextToken();
        VariableNode name = fn_like_name();
        TypedArgumentListNode args = fn_args();
        TypeNode[] retval = fn_retval();
        StatementBodyNode body = fn_body();
        Newline();
        return new MethodDefinitionNode(name, args, retval, body);
    }

    private WhileStatementNode while_stmt() {
        assert lookahead.is("while");
        NextToken();
        TestNode cond = test();
        StatementBodyNode body = fn_body();
        StatementBodyNode nobreak = new StatementBodyNode();
        if (lookahead.is("nobreak")) {
            NextToken();
            nobreak = new StatementBodyNode(fn_body());
        }
        Newline();
        return new WhileStatementNode(cond, body, nobreak);
    }

    private ImportStatementNode import_stmt() {
        assert lookahead.is("import");
        NextToken();
        if (lookahead.is(Token.NEWLINE)) {
            throw new ParserException("Empty import statements are illegal");
        }
        VariableNode[] imports = var_list(true, false);
        Newline();
        return new ImportStatementNode(imports);
    }

    private ExportStatementNode export_stmt() {
        assert lookahead.is("export");
        NextToken();
        if (lookahead.is(Token.NEWLINE)) {
            throw new ParserException("Empty export statements are illegal");
        }
        VariableNode[] exports = var_list(true,false);
        Newline();
        return new ExportStatementNode(exports);
    }

    private TypegetStatementNode typeget_stmt() {
        VariableNode from = new VariableNode();
        if (lookahead.is("from")) {
            NextToken();
            from = variable();
        }
        assert lookahead.is("typeget");
        NextToken();
        if (lookahead.is(Token.NEWLINE)) {
            throw new ParserException("Empty typeget statements are illegal");
        }
        VariableNode[] typegets = var_list(true, false);
        Newline();
        return new TypegetStatementNode(typegets, from);
    }

    private BreakStatementNode break_stmt() {
        assert lookahead.is("break");
        NextToken();
        int loops;
        if (lookahead.is(Token.INTEGER)) {
            loops = Integer.parseInt(lookahead.sequence);
        } else if (lookahead.is(Token.NEWLINE)) {
            loops = 0;
        } else {
            throw new ParserException("Break statement must not be followed by anything");
        }
        Newline();
        return new BreakStatementNode(loops);
    }

    private ContinueStatementNode continue_stmt() {
        assert lookahead.is("continue");
        NextToken();
        Newline();
        return new ContinueStatementNode();
    }

    private ReturnStatementNode return_stmt() {
        assert lookahead.is("return");
        NextToken();
        TestNode[] returned = test_list(false);
        Newline();
        return new ReturnStatementNode(returned);
    }

    private BaseNode descriptor_keyword(ArrayList<DescriptorNode> descriptors) {
        assert lookahead.is(Token.KEYWORD);
        ClassStatementNode node;
        switch (lookahead.sequence) {
            case "class":
                node = class_def();
                break;
            case "method":
                node = method_def();
                break;
            case "interface":
                node = interface_def();
                break;
            case "func":
            case "if":
            case "for":
            case "else":
            case "do":
            case "dotimes":
            case "while":
            case "in":
            case "from":
            case "import":
            case "export":
            case "typeget":
            case "break":
            case "continue":
            case "return":
                throw new ParserException("Expected descriptor-usable keyword, got "+lookahead.sequence);
            default:
                throw new RuntimeException("Keyword mismatch");
        }
        node.addDescriptor(descriptors.toArray(new DescriptorNode[0]));
        return node;
    }

    private BaseNode descriptor_op_sp(ArrayList<DescriptorNode> descriptors) {
        assert lookahead.is(Token.OPERATOR_SP);
        OperatorDefinitionNode op_sp = operator_sp();
        op_sp.addDescriptor(descriptors.toArray(new DescriptorNode[0]));
        return op_sp;
    }

    private ClassStatementNode descriptor_var(ArrayList<DescriptorNode> descriptors) {
        assert lookahead.is(Token.VARIABLE);
        ClassStatementNode stmt;
        if (lineContains(Token.ASSIGN)) {
            stmt = decl_assignment();
        } else {
            stmt = declaration();
        }
        stmt.addDescriptor(descriptors.toArray(new DescriptorNode[0]));
        return stmt;
    }

    private TypedArgumentListNode fn_args() {
        if (!lookahead.is("(")) {
            throw new ParserException("Argument lists must start with an open-paren");
        }
        NextToken();
        ArrayList<TypedArgumentNode> args = new ArrayList<>();
        while (!lookahead.is(")")) {
            args.add(typed_argument());
            if (lookahead.is(Token.COMMA)) {
                NextToken();
                if (lookahead.is(Token.NEWLINE)) {
                    NextToken();
                }
                continue;
            }
            if (lookahead.is(Token.NEWLINE)) {
                NextToken();
            }
            if (!lookahead.is(")")) {
                throw new ParserException("Comma must separate arguments");
            }
        }
        NextToken();
        return new TypedArgumentListNode(args.toArray(new TypedArgumentNode[0]));
    }

    private StatementBodyNode fn_body() {
        if (!lookahead.is("{")) {
            throw new ParserException("The body of a function must be enclosed in curly brackets");
        }
        NextToken(true);
        ArrayList<BaseNode> statements = new ArrayList<>();
        while (!lookahead.is("}")) {
            statements.add(statement());
            passNewlines();
        }
        NextToken();
        return new StatementBodyNode(statements.toArray(new BaseNode[0]));
    }

    private ClassBodyNode class_body() {
        if (!lookahead.is("{")) {
            throw new ParserException("The body of a class must be enclosed in curly brackets");
        }
        NextToken(false);
        ArrayList<ClassStatementNode> statements = new ArrayList<>();
        while (!lookahead.is("}")) {
            statements.add(class_statement());
            passNewlines();
        }
        NextToken();
        Newline();
        return new ClassBodyNode(statements.toArray(new ClassStatementNode[0]));
    }

    private VariableNode fn_like_name() {
        VariableNode name;
        if (lookahead.sequence.contains(".")) {
            throw new ParserException("Function-like name may not contain dots");
        } else {
            name = new VariableNode(lookahead.sequence);
            NextToken();
        }
        return name;
    }

    private TypeNode[] fn_retval() {
        if (lookahead.is("{")) {
            return new TypeNode[0];
        }
        if (!lookahead.is("->")) {
            throw new ParserException("Return value must use arrow operator");
        }
        NextToken();
        LinkedList<TypeNode> types = new LinkedList<>();
        while (!lookahead.is("{") && !lookahead.is(Token.NEWLINE)) {
            types.add(type());
            if (lookahead.is(",")) {
                NextToken();
            }
        }
        return types.toArray(new TypeNode[0]);
    }

    private TestNode test(Boolean ignore_newline) {
        SubTestNode if_true = test_no_ternary(ignore_newline);
        if (lookahead.is("if")) {
            NextToken(ignore_newline);
            SubTestNode statement = test_no_ternary(ignore_newline);
            if (!lookahead.is("else")) {
                throw new ParserException("Ternary must have an else");
            }
            NextToken(ignore_newline);
            TestNode if_false = test(ignore_newline);
            return new TernaryNode(if_true, statement, if_false);
        } else {
            return if_true;
        }
    }

    private TestNode test() {
        return test(false);
    }

    private SubTestNode test_no_ternary(boolean ignore_newline) {
        switch (lookahead.token) {
            case Token.SELF_CLS:
            case Token.VARIABLE:
                return test_left_variable(ignore_newline);
            case Token.OPEN_BRACE:
                return subtest_openBrace();
            case Token.BOOL_OP:
                return left_bool_op(ignore_newline);
            case Token.INTEGER:
                return integer();
            case Token.OP_FUNC:
                return op_func();
            case Token.ELLIPSIS:
                return ellipsis();
            case Token.OPERATOR:
                return left_operator(ignore_newline);
            case Token.KEYWORD:
                if (lookahead.is("lambda")) {
                    return lambda();
                } else if (lookahead.is("some")) {
                    return some_op();
                }
                // Lack of final return very much intentional here
            default:
                throw new ParserException(""); // TODO: Error messages
        }
    }

    private LambdaNode lambda() {
        assert lookahead.is("lambda");
        NextToken();
        TypedArgumentListNode args = fn_args();
        StatementBodyNode body = fn_body();
        return new LambdaNode(args, body);
    }

    private SubTestNode test_left_variable(boolean ignore_newline) {
        // Things starting with a variable token (besides ternary):
        // Function call
        // Lone variable, just sitting there
        // X Declaration
        // X Declared assignment
        // X Assignment
        // X Assignment to function call
        // Lone expression
        if (lineContains(Token.ASSIGN)) {
            throw new ParserException("Illegal assignment");
        } else if (contains(ignore_newline, Token.AUG_ASSIGN)) {
            throw new ParserException("Illegal augmented assignment");
        } else {
            LinkedList<SubTestNode> nodes = new LinkedList<>();
            while_loop:
            while (!lookahead.is(Token.NEWLINE)) {
                switch (lookahead.token) {
                    case Token.OPEN_BRACE:
                        SubTestNode last_node = nodes.peekLast();
                        if (last_node instanceof OperatorNode) {
                            nodes.add(subtest_openBrace());
                            break;
                        }
                        break while_loop;
                    case Token.SELF_CLS:
                    case Token.VARIABLE:
                        if (tokens.get(sizeOfVariable()).is("(")) {
                            nodes.add(function_call());
                        } else {
                            nodes.add(var_or_index());
                        }
                        break;
                    case Token.INTEGER:
                        nodes.add(integer());
                        break;
                    case Token.BOOL_OP:
                        nodes.add(new OperatorNode(lookahead.sequence));
                        NextToken();
                        break;
                    case Token.OPERATOR:
                        if (lookahead.is("->")) {
                            throw new ParserException("Unexpected ->");
                        }
                        nodes.add(new OperatorNode(lookahead.sequence));
                        NextToken();
                        break;
                    case Token.OP_FUNC:
                        nodes.add(op_func());
                        break;
                    case Token.COMMA:
                        break while_loop;
                    case Token.KEYWORD:
                        if (lookahead.is("in")) {
                            nodes.add(new OperatorNode("in"));
                            NextToken();
                            break;
                        } else if (lookahead.is("if", "else")) {
                            break while_loop;
                        }  // Lack of breaks here is intentional
                    case Token.CLOSE_BRACE:
                        if (ignore_newline) {
                            break while_loop;
                        }
                    default:
                        throw new ParserException("Unexpected "+lookahead.sequence);
                }
            }
            if (nodes.size() == 1) {
                return nodes.get(0);
            }
            parseExpression(nodes, "**");
            parseExpression(nodes, "~");
            parseExpression(nodes, "*", "/", "//", "%");
            parseExpression(nodes,"+", "-");
            parseExpression(nodes, "<<", ">>");
            parseExpression(nodes, "&");
            parseExpression(nodes, "^", "|");
            parseExpression(nodes, "<", "<=", ">", ">=", "!=", "==");
            parseExpression(nodes, "in");
            parseExpression(nodes, "not");
            parseExpression(nodes, "and");
            parseExpression(nodes, "or");
            if (nodes.size() > 1) {
                throw new ParserException("Too many tokens");
            }
            return nodes.get(0);
        }
    }

    private void parseExpression(LinkedList<SubTestNode> nodes, String... expr) {
        if (nodes.size() == 1) {
            return;
        }
        for (int nodeNumber = 0; nodeNumber < nodes.size(); nodeNumber++) {
            SubTestNode node = nodes.get(nodeNumber);
            if (node instanceof OperatorNode) {
                if (((OperatorNode) node).getOperands().length != 0) {
                    continue;
                }
                String operator = ((OperatorNode) node).getOperator();
                if (Arrays.asList(expr).contains(operator)) {
                    if (operator.matches("\\+\\+|--|not|~")) {
                        parseUnaryOp(nodes, nodeNumber);
                    } else {
                        parseOperator(nodes, nodeNumber);
                        nodeNumber--;  // Adjust pointer to match new object
                    }
                }
            }
        }
    }

    private void parseOperator(LinkedList<SubTestNode> nodes, int nodeNumber) {
        SubTestNode node = nodes.get(nodeNumber);
        SubTestNode previous = nodes.get(nodeNumber - 1);
        SubTestNode next = nodes.get(nodeNumber + 1);
        SubTestNode op;
        if (node instanceof OperatorNode) {
            op = new OperatorNode(((OperatorNode) node).getOperator(), previous, next);
        } else {
            throw new ParserException("Unexpected node for parseOperator");
        }
        nodes.set(nodeNumber, op);
        nodes.remove(nodeNumber + 1);
        nodes.remove(nodeNumber - 1);
    }

    private void parseUnaryOp(LinkedList<SubTestNode> nodes, int nodeNumber) {
        SubTestNode node = nodes.get(nodeNumber);
        SubTestNode next = nodes.get(nodeNumber + 1);
        SubTestNode op;
        if (node instanceof OperatorTypeNode) {
            op = new OperatorNode(((OperatorTypeNode) node), next);
        }  else {
            throw new ParserException("Unexpected node for parseOperator");
        }
        nodes.set(nodeNumber, op);
        nodes.remove(nodeNumber + 1);
    }

    private VariableNode var_index() {
        VariableNode var = variable();
        if (!lookahead.is("[")) {
            throw new ParserException("Expected [, got " + lookahead.sequence);
        }
        ArrayList<TestNode[]> vars = new ArrayList<>();
        while (lookahead.is("[")) {
            if (braceContains(":")) {
                vars.add(new TestNode[]{slice()});
            } else {
                vars.add(literal().getBuilders());
            }
        }
        return new VariableNode(var.getNames(), vars);
    }

    private VariableNode var_or_index() {
        if (tokens.get(1).is("[")) {
            return var_index();
        } else {
            return variable();
        }
    }

    private TypeNode type() {
        return type(false, false);
    }

    private TypeNode type(boolean allow_empty, boolean is_vararg) {
        String main;
        if (!lookahead.is(Token.VARIABLE, Token.SELF_CLS)) {
            if (allow_empty && lookahead.is("[")) {
                main = "";
            } else {
                throw new ParserException("Expected type name, got " + lookahead.sequence);
            }
        } else {
            main = lookahead.sequence;
            NextToken();
        }
        if (!lookahead.is("[")) {
            return new TypeNode(main);
        }
        NextToken(true);
        LinkedList<TypeNode> subtypes = new LinkedList<>();
        while (!lookahead.is("]")) {
            boolean subcls_vararg;
            if (lookahead.is("*", "**")) {
                subcls_vararg = true;
                NextToken(true);
            } else {
                subcls_vararg = false;
            }
            subtypes.add(type(true, subcls_vararg));
            if (lookahead.is(Token.COMMA)) {
                NextToken(true);
                continue;
            }
            passNewlines();
            if (!lookahead.is("]")) {
                throw new ParserException("Comma must separate subtypes");
            }
        }
        NextToken();
        return new TypeNode(main, subtypes.toArray(new TypeNode[0]), is_vararg);
    }

    private TypedVariableNode[] for_vars() {
        LinkedList<TypedVariableNode> vars = new LinkedList<>();
        while (!lookahead.is("in")) {
            if (!lookahead.is(Token.VARIABLE)) {
                throw new ParserException("Expected variable, got " + lookahead.sequence);
            }
            vars.add(typed_variable());
            if (lookahead.is(Token.COMMA)) {
                NextToken();
            }
        }
        return vars.toArray(new TypedVariableNode[0]);
    }

    private TestNode[] for_iterables() {
        if (lookahead.is(Token.NEWLINE)) {
            return new TestNode[0];
        }
        LinkedList<TestNode> tests = new LinkedList<>();
        while (!lookahead.is("{")) {
            tests.add(test());
            if (lookahead.is(Token.COMMA)) {
                NextToken();
                continue;
            }
            if (!lookahead.is("{")) {
                throw new ParserException("Comma must separate values");
            }
        }
        return tests.toArray(new TestNode[0]);
    }

    private TestNode[] test_list(boolean ignore_newlines) {
        if (lookahead.is("(") && !braceContains("in") && !braceContains("for")) {
            int brace_size = sizeOfBrace(0);
            if (!tokens.get(brace_size).is(Token.COMMA)) {
                NextToken();
                TestNode[] vars = test_list(true);
                if (!lookahead.is(")")) {
                    throw new ParserException("Unmatched braces");
                }
                NextToken();
                return vars;
            }
        }
        if (!ignore_newlines && lookahead.is(Token.NEWLINE)) {
            return new TestNode[0];
        }
        LinkedList<TestNode> tests = new LinkedList<>();
        while (!lookahead.is(Token.NEWLINE)) {
            tests.add(test(ignore_newlines));
            if (lookahead.is(Token.COMMA)) {
                NextToken(ignore_newlines);
                continue;
            }
            if (!ignore_newlines && !lookahead.is(Token.NEWLINE)) {
                throw new ParserException("Comma must separate values");
            } else if (ignore_newlines) {
                break;
            }
        }
        if (!ignore_newlines && !lookahead.is(Token.NEWLINE)) {
            throw new ParserException("Expected newline, got "+lookahead.token);
        }
        return tests.toArray(new TestNode[0]);
    }

    private TypedArgumentNode typed_argument() {
        Boolean is_vararg = lookahead.is("*", "**");
        String vararg_type;
        if (lookahead.is("*", "**")) {
            vararg_type = lookahead.sequence;
            NextToken();
        } else {
            vararg_type = "";
        }
        TypeNode type = type();
        VariableNode var = variable();
        TestNode default_value = null;
        if (lookahead.is("=")) {
            NextToken();
            default_value = test(true);
        }
        return new TypedArgumentNode(type, var, default_value, is_vararg, vararg_type);
    }

    private ClassStatementNode class_statement() {
        BaseNode stmt = statement();
        if (stmt instanceof ClassStatementNode) {
            return (ClassStatementNode) stmt;
        }
        throw new ParserException(lookahead.sequence+" is not a valid class statement");
    }

    private FunctionCallNode function_call() {
        VariableNode caller = var_or_index();
        if (!lookahead.is("(")) {
            throw new ParserException("Expected function call, got "+lookahead.sequence);
        }
        ArgumentNode[] args = fn_call_args();
        return new FunctionCallNode(caller, args);
    }

    private DeclarationNode declaration() {
        TypeNode type = type();
        VariableNode var = variable();
        return new DeclarationNode(type, var);
    }

    private PropertyDefinitionNode property_stmt() {
        VariableNode name = null;
        if (!lookahead.is("{")) {
            name = variable(false);
        }
        NextToken(true);
        StatementBodyNode get = null;
        StatementBodyNode set = null;
        TypedArgumentListNode set_args = null;
        if (lookahead.is("get")) {
            get = fn_body();
        }
        if (lookahead.is("set")) {
            set_args = fn_args();
            set = fn_body();
        }
        passNewlines();
        if (!lookahead.is("}")) {
            throw new ParserException("Only set and get are allowed in context statements");
        }
        NextToken();
        Newline();
        return new PropertyDefinitionNode(name, get, set_args, set);
    }

    private ComprehensionNode comprehension() {
        String brace_type;
        if (!lookahead.is(Token.OPEN_BRACE)) {  // For comprehensions in function calls
            brace_type = null;
        } else {
            brace_type = lookahead.sequence;
            NextToken(true);
        }
        TestNode builder = test(true);
        if (!lookahead.is("for")) {
            throw new ParserException("Invalid start to comprehension");
        }
        NextToken(true);
        TypedVariableNode[] variables = comp_params();
        if (!lookahead.is("in")) {
            throw new ParserException("Comprehension body must have in after variable list");
        }
        NextToken(true);
        LinkedList<TestNode> looped = new LinkedList<>();
        while (true) {
            if (lookahead.is(brace_type)) {
                break;
            }
            looped.add(test(true));
            if (lookahead.is(",")) {
                NextToken(true);
            } else {
                break;
            }
        }
        if (!lookahead.is(Token.CLOSE_BRACE)) {  // FIXME: Close braces must match open braces
            throw new ParserException("Expected close brace");
        }
        NextToken();
        TestNode[] looped_array = looped.toArray(new TestNode[0]);
        return new ComprehensionNode(brace_type, variables, builder, looped_array);
    }

    private LiteralNode literal() {
        assert lookahead.is(Token.OPEN_BRACE);
        String brace_type = lookahead.sequence;
        String balanced_brace;
        switch (brace_type) {
            case "(":
                balanced_brace = ")";
                break;
            case "[":
                balanced_brace = "]";
                break;
            case "{":
                balanced_brace = "}";
                break;
            default:
                throw new RuntimeException("Unknown brace "+brace_type);
        }
        NextToken(true);
        LinkedList<TestNode> tokens = new LinkedList<>();
        LinkedList<Boolean> is_splat = new LinkedList<>();
        while (true) {
            if (lookahead.is(balanced_brace)) {
                break;
            } else if (lookahead.is(Token.CLOSE_BRACE)) {
                throw new ParserException("Unmatched braces");
            }
            if (lookahead.is("*")) {
                is_splat.add(true);
                NextToken(true);
            } else {
                is_splat.add(false);
            }
            tokens.add(test(true));
            if (lookahead.is(",")) {
                NextToken(true);
            } else {
                break;
            }
        }
        if (lookahead.is(balanced_brace)) {
            NextToken();
        } else {
            throw new ParserException("Unmatched braces");
        }
        return new LiteralNode(brace_type, tokens.toArray(new TestNode[0]), is_splat.toArray(new Boolean[0]));
    }

    private VariableNode[] var_list(boolean allow_dotted, boolean ignore_newlines) {
        LinkedList<VariableNode> variables = new LinkedList<>();
        if (ignore_newlines) {
            passNewlines();
        }
        if (lookahead.is("(") && !braceContains("in") && !braceContains("for")) {
            NextToken();
            VariableNode[] vars = var_list(allow_dotted, true);
            if (!lookahead.is(")")) {
                throw new ParserException("Unmatched braces");
            }
            return vars;
        }
        while (true) {
            if (!lookahead.is(Token.VARIABLE)) {
                break;
            }
            if (lookahead.is(Token.CLOSE_BRACE)) {
                throw new ParserException("Unmatched braces");
            }
            variables.add(variable(allow_dotted));
            if (lookahead.is(",")) {
                NextToken(ignore_newlines);
            } else {
                break;
            }
        }
        return variables.toArray(new VariableNode[0]);
    }

    private DictComprehensionNode dict_comprehension() {  // FIXME: This is a literal, not a comprehension
        assert lookahead.is("{");
        NextToken(true);
        LinkedList<TestNode> keys = new LinkedList<>();
        LinkedList<TestNode> values = new LinkedList<>();
        while (true) {
            keys.add(test());
            if (!lookahead.is(":")) {
                throw new ParserException("Dict comprehension must have colon");
            }
            NextToken(true);
            values.add(test());
            if (!lookahead.is(",")) {
                break;
            }
            NextToken(true);
            if (lookahead.is("}")) {
                break;
            }
        }
        if (!lookahead.is("}")) {
            throw new ParserException("Unmatched brace");
        }
        NextToken();
        return new DictComprehensionNode(keys.toArray(new TestNode[0]), values.toArray(new TestNode[0]));
    }

    private ContextDefinitionNode context_def() {
        assert lookahead.is("context");
        NextToken();
        VariableNode name = new VariableNode();
        if (lookahead.is(Token.VARIABLE)) {
            name = variable(false);
        }
        if (!lookahead.is("{")) {
            throw new ParserException("Context managers must be followed by a curly brace");
        }
        NextToken(true);
        StatementBodyNode enter = new StatementBodyNode();
        StatementBodyNode exit = new StatementBodyNode();
        if (lookahead.is("enter")) {
            enter = fn_body();
        }
        if (lookahead.is("exit")) {
            exit = fn_body();
        }
        passNewlines();
        if (!lookahead.is("}")) {
            throw new ParserException("Context manager must end with close curly brace");
        }
        NextToken();
        Newline();
        if (enter.isEmpty()) {
            enter = new StatementBodyNode();
        }
        if (exit.isEmpty()) {
            exit = new StatementBodyNode();
        }
        Newline();
        return new ContextDefinitionNode(name, enter, exit);
    }

    private TryStatementNode try_stmt() {
        assert lookahead.is("try");
        NextToken();
        StatementBodyNode body = fn_body();
        StatementBodyNode except = new StatementBodyNode();
        VariableNode[] excepted = new VariableNode[0];
        VariableNode as_var = new VariableNode();
        StatementBodyNode else_stmt = new StatementBodyNode();
        StatementBodyNode finally_stmt = new StatementBodyNode();
        if (lookahead.is("except")) {
            NextToken();
            excepted = var_list(true, false);
            if (lookahead.is("as")) {
                NextToken();
                as_var = variable(false);
            }
            except = fn_body();
            if (lookahead.is("else")) {
                NextToken();
                else_stmt = fn_body();
            }
        }
        if (lookahead.is("finally")) {
            NextToken();
            finally_stmt = fn_body();
        }
        if (except.isEmpty() && finally_stmt.isEmpty()) {
            throw new ParserException("Try statement must either have an except or finally clause");
        }
        Newline();
        return new TryStatementNode(body, except, excepted, as_var, else_stmt, finally_stmt);
    }

    private WithStatementNode with_stmt() {
        assert lookahead.is("with");
        NextToken();
        LinkedList<TestNode> managed = new LinkedList<>();
        while (!lookahead.is("as")) {
            managed.add(test());
            if (lookahead.is(Token.COMMA)) {
                NextToken();
            } else if (!lookahead.is("as")) {
                throw new ParserException("Expected comma or as, got "+lookahead.sequence);
            }
        }
        VariableNode[] vars = var_list(false, false);
        StatementBodyNode body = fn_body();
        Newline();
        return new WithStatementNode(managed.toArray(new TestNode[0]), vars, body);
    }

    private AssertStatementNode assert_stmt() {
        assert lookahead.is("assert");
        NextToken();
        TestNode assertion = test();
        Newline();
        return new AssertStatementNode(assertion);
    }

    private DeleteStatementNode del_stmt() {
        assert lookahead.is("del");
        NextToken();
        TestNode deletion = test();
        Newline();
        return new DeleteStatementNode(deletion);
    }

    private YieldStatementNode yield_stmt() {
        assert lookahead.is("yield");
        NextToken();
        boolean is_from = lookahead.is("from");
        if (is_from) {
            NextToken();
        }
        LinkedList<TestNode> yields = new LinkedList<>();
        while (!lookahead.is(Token.NEWLINE)) {
            yields.add(test());
            if (lookahead.is(Token.COMMA)) {
                NextToken();
                continue;
            }
            if (!lookahead.is(Token.NEWLINE)) {
                throw new ParserException("Comma must separate yields");
            }
        }
        return new YieldStatementNode(is_from, yields.toArray(new TestNode[0]));
    }

    private RaiseStatementNode raise_stmt() {
        assert lookahead.is("raise");
        NextToken();
        TestNode raised = test();
        Newline();
        return new RaiseStatementNode(raised);
    }

    private ImportExportNode left_from() {
        assert lookahead.is("from");
        if (lineContains("import")) {
            return import_stmt();
        } else if (lineContains("export")) {
            return export_stmt();
        } else if (lineContains("typeget")) {
            return typeget_stmt();
        } else {
            throw new ParserException("from does not begin a statement");
        }
    }

    private TypedefStatementNode typedef_stmt() {
        assert lookahead.is("typedef");
        NextToken();
        TypeNode name = type();
        assert lookahead.is("as");
        NextToken();
        TypeNode type = type();
        Newline();
        return new TypedefStatementNode(name, type);
    }

    private ArgumentNode[] fn_call_args() {
        if (!lookahead.is("(")) {
            throw new ParserException("Function call must start with open-paren");
        }
        NextToken(true);
        if (lookahead.is(")")) {
            NextToken();
            return new ArgumentNode[0];
        }
        LinkedList<ArgumentNode> args = new LinkedList<>();
        while (true) {
            VariableNode var = new VariableNode();
            if (tokens.get(sizeOfVariable()).is("=")) {
                var = variable(false);
                NextToken(true);
            }
            String vararg;
            if (lookahead.is("*", "**")) {
                vararg = lookahead.sequence;
                NextToken(true);
            } else {
                vararg = "";
            }
            TestNode argument = test(true);
            args.add(new ArgumentNode(var, vararg, argument));
            if (lookahead.is(")")) {
                break;
            }
            if (!lookahead.is(",")) {
                throw new ParserException("Expected comma, got "+lookahead.sequence);
            }
            NextToken(true);
        }
        NextToken();
        return args.toArray(new ArgumentNode[0]);
    }

    private SomeStatementNode some_op() {
        assert lookahead.is("some");
        NextToken();
        TestNode contained = test();
        if (!(contained instanceof OperatorNode)) {
            throw new ParserException("Expected an in, got "+lookahead.sequence);
        }
        OperatorNode in_stmt = (OperatorNode) contained;
        if (!in_stmt.getOperator().equals("in")) {
            throw new ParserException("Expected an in, got "+lookahead.sequence);
        }
        TestNode[] operands = in_stmt.getOperands();
        return new SomeStatementNode(operands[0], operands[1]);
    }

    private TypedVariableNode typed_variable() {
        TypeNode type = type();
        VariableNode var = variable();
        return new TypedVariableNode(type, var);
    }

    private SimpleStatementNode inc_or_dec() {
        Token amount = tokens.get(sizeOfVariable());
        if (amount.is("++")) {
            return increment();
        } else if (amount.is("--")) {
            return decrement();
        } else {
            throw new RuntimeException("inc_or_dec must use ++ or --");
        }
    }

    private IncrementNode increment() {
        VariableNode var = var_or_index();
        if (!lookahead.is("++")) {
            throw new RuntimeException("Expected ++, got "+lookahead.sequence);
        }
        NextToken();
        return new IncrementNode(var);
    }

    private DecrementNode decrement() {
        VariableNode var = var_or_index();
        if (!lookahead.is("--")) {
            throw new RuntimeException("Expected --, got "+lookahead.sequence);
        }
        NextToken();
        return new DecrementNode(var);
    }

    private TypedVariableNode[] comp_params() {
        LinkedList<TypedVariableNode> vars = new LinkedList<>();
        while (true) {
            vars.add(typed_variable());
            if (lookahead.is("in")) {
                break;
            }
            if (!lookahead.is(",")) {
                throw new ParserException("Unexpected "+lookahead.sequence);
            }
            NextToken();
        }
        return vars.toArray(new TypedVariableNode[0]);
    }

    private SliceNode slice() {
        assert lookahead.is("[");
        NextToken(true);
        TestNode start;
        if (lookahead.is(":")) {
            start = null;
        } else {
            start = test(true);
        }
        if (lookahead.is("]")) {
            NextToken();
            return new SliceNode(start, null, null);
        }
        TestNode end = slice_test();
        if (lookahead.is("]")) {
            NextToken();
            return new SliceNode(start, end, null);
        }
        TestNode step = slice_test();
        if (!lookahead.is("]")) {
            throw new ParserException("Expected ], got "+lookahead.sequence);
        }
        NextToken();
        return new SliceNode(start, end, step);
    }

    private TestNode slice_test() {
        if (!lookahead.is(":")) {
            throw new ParserException("Expected :, got "+lookahead.sequence);
        }
        NextToken(true);
        if (lookahead.is(":", "]")) {
            return null;
        } else {
            return test(true);
        }
    }

    private InterfaceDefinitionNode interface_def() {
        assert lookahead.is("interface");
        NextToken();
        TypeNode name = type();
        LinkedList<TypeNode> superclasses = new LinkedList<>();
        while (lookahead.is("from")) {
            NextToken();
            superclasses.add(type());
        }
        return new InterfaceDefinitionNode(name, superclasses.toArray(new TypeNode[0]), interface_body());
    }

    private InterfaceBodyNode interface_body() {
        if (!lookahead.is("{")) {
            throw new ParserException("The body of a class must be enclosed in curly brackets");
        }
        NextToken(false);
        ArrayList<InterfaceStatementNode> statements = new ArrayList<>();
        while (!lookahead.is("}")) {
            statements.add(interface_stmt());
            passNewlines();
        }
        NextToken();
        Newline();
        return new InterfaceBodyNode(statements.toArray(new InterfaceStatementNode[0]));
    }

    private InterfaceStatementNode interface_stmt() {  // TODO: Clean up method
        if (lookahead.is(Token.OPERATOR_SP, Token.DESCRIPTOR) || lookahead.is("method")
              || (lookahead.is("class") && tokens.get(1).is(Token.DESCRIPTOR, Token.KEYWORD))) {
            LinkedList<DescriptorNode> descriptors = new LinkedList<>();
            if (lookahead.is("class")) {
                descriptors.add(new DescriptorNode("class"));
                NextToken();
            }
            while (lookahead.is(Token.DESCRIPTOR)) {
                descriptors.add(new DescriptorNode(lookahead.sequence));
                NextToken();
            }
            int line_size = 0;
            for (Token token : tokens) {
                if (token.is(Token.NEWLINE)) {
                    break;
                }
                line_size++;
            }
            if (!tokens.get(line_size - 1).is("{")) {
                boolean is_operator;
                VariableNode fn_name;
                String op_name;
                if (lookahead.is(Token.OPERATOR_SP)) {
                    is_operator = true;
                    op_name = lookahead.sequence;
                    fn_name = null;
                    NextToken();
                } else {
                    NextToken();
                    is_operator = false;
                    fn_name = variable();
                    op_name = null;
                }
                TypedArgumentListNode args = fn_args();
                TypeNode[] retvals = null;
                if (lookahead.is("->")) {
                    retvals = fn_retval();
                }
                if (is_operator) {
                    GenericOperatorNode op = new GenericOperatorNode(op_name, args, retvals);
                    if (!descriptors.isEmpty()) {
                        op.addDescriptor(descriptors.toArray(new DescriptorNode[0]));
                    }
                    return op;
                } else {
                    GenericFunctionNode fn = new GenericFunctionNode(fn_name, args, retvals);
                    if (!descriptors.isEmpty()) {
                        fn.addDescriptor(descriptors.toArray(new DescriptorNode[0]));
                    }
                    return fn;
                }
            }
        }
        BaseNode stmt = statement();
        if (stmt instanceof InterfaceStatementNode) {
            return (InterfaceStatementNode) stmt;
        } else {
            throw new ParserException("Illegal statement");
        }
    }
}
