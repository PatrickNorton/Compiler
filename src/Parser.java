// TODO! Rename getName() to not conflict with standard name
// TODO: Reduce/remove nulls
// TODO? Replace StatementBodyNode with BaseNode[] (and equivalents)
// TODO: Keyword-only arguments
// FIXME: Turn var_list(false...) into name_list
// FIXME: Allow self/cls declarations


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
    private LinkedList<Token> tokens;
    private Token lookahead;

    public Parser(LinkedList<Token> tokens) {
        this.tokens = tokens;
        this.lookahead = tokens.getFirst();
    }

    public static TopNode parse(LinkedList<Token> tokens) {
        Parser parser = new Parser(tokens);
        TopNode topNode = new TopNode();
        while (!parser.lookahead.is(TokenType.EPSILON)) {
            topNode.add(parser.statement());
            parser.passNewlines();
        }
        return topNode;
    }

    private boolean contains(boolean braces, TokenType... question) {
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

    private boolean lineContains(TokenType... question) {
        int netBraces = 0;
        for (Token token : tokens) {
            if (token.is(TokenType.OPEN_BRACE)) {
                netBraces++;
            } else if (token.is(TokenType.CLOSE_BRACE)) {
                netBraces--;
            }
            if (netBraces == 0 && token.is(TokenType.NEWLINE)) {
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
            if (token.is(TokenType.NEWLINE)) {
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
        int netBraces = lookahead.is(TokenType.OPEN_BRACE) ? 0 : 1;
        for (Token token : this.tokens) {
            switch (token.token) {
                case OPEN_BRACE:
                    netBraces++;
                    break;
                case CLOSE_BRACE:
                    netBraces--;
                    break;
                case EPSILON:
                    throw new ParserException("Unmatched brace");
            }
            if (netBraces == 0) {
                return tokens;
            }
            if (netBraces == 1) {
                tokens.add(token);
            }
        }
        throw new RuntimeException("You shouldn't have ended up here");
    }

    private boolean braceContains(TokenType... question) {
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
        assert lookahead.is(TokenType.NAME);
        int netBraces = 0;
        boolean wasVar = false;
        for (int size = offset;; size++) {
            Token token = tokens.get(size);
            switch (token.token) {
                case OPEN_BRACE:
                    netBraces++;
                    break;
                case CLOSE_BRACE:
                    if (netBraces == 0) {
                        return size;
                    }
                    netBraces--;
                    break;
                case NAME:
                    if (wasVar && netBraces == 0) {
                        return size;
                    }
                    wasVar = true;
                    break;
                case DOT:
                    wasVar = false;
                    break;
                case EPSILON:
                    if (netBraces > 0) {
                        throw new ParserException("Unmatched brace");
                    }
                default:
                    if (netBraces == 0) {
                        return size;
                    }
            }
        }
    }

    private int sizeOfBrace(int offset) {
        int netBraces = 0;
        int size = offset;
        for (Token token : tokens) {
            if (token.is(TokenType.OPEN_BRACE)) {
                netBraces++;
            } else if (token.is(TokenType.CLOSE_BRACE)) {
                netBraces--;
            }
            size++;
            if (netBraces == 0) {
                break;
            }
        }
        return size;
    }

    private void NextToken(boolean ignore_newline) {
        tokens.pop();
        if (ignore_newline) {
            passNewlines();
        }
        if (tokens.isEmpty()) {
            lookahead = new Token(TokenType.EPSILON, "");
        } else {
            lookahead = tokens.getFirst();
        }
    }

    private void NextToken() {
        NextToken(false);
    }

    private void Newline() {
        if (!lookahead.is(TokenType.NEWLINE)) {
            throw new ParserException("Expected newline, got "+lookahead);
        }
        NextToken();
    }

    private void passNewlines() {
        while (!tokens.isEmpty() && tokens.getFirst().is(TokenType.NEWLINE)) {
            NextToken(false);
        }
    }

    private void mustToken(String message, boolean parser, String... tokens) {
        mustToken(message, parser, true, false, tokens);
    }

    private void mustToken(String message, boolean parser, TokenType... tokens) {
        mustToken(message, parser, true, false, tokens);
    }

    private void mustToken(String message, boolean parser, boolean nextToken, boolean ignore_newline, String... tokens) {
        if (!lookahead.is(tokens)) {
            throw (parser ? new ParserException(message) : new RuntimeException(message));
        }
        if (nextToken) {
            NextToken(ignore_newline);
        }
    }

    private void mustToken(String message, boolean parser, boolean nextToken, boolean ignore_newline, TokenType... tokens) {
        if (!lookahead.is(tokens)) {
            throw (parser ? new ParserException(message) : new RuntimeException(message));
        }
        if (nextToken) {
            NextToken(ignore_newline);
        }
    }

    private BaseNode statement() {
        passNewlines();
        switch (lookahead.token) {
            case KEYWORD:
                return keyword();
            case DESCRIPTOR:
                return descriptor();
            case OPEN_BRACE:
                return openBrace();
            case CLOSE_BRACE:
                throw new ParserException("Unmatched close brace");
            case NAME:
                return left_variable();
            case COMMA:
                throw new ParserException("Unexpected comma");
            case AUG_ASSIGN:
                throw new ParserException("Unexpected operator");
            case OPERATOR:
                return left_operator(false);
            case ASSIGN:
                throw new ParserException("Unexpected assignment");
            case STRING:
                return string();
            case BOOL_OP:
                return left_bool_op(false);
            case NUMBER:
                return number();
            case OPERATOR_SP:
                return operator_sp();
            case OP_FUNC:
                return op_func();
            case COLON:
                throw new ParserException("Unexpected colon");
            case ELLIPSIS:
                return ellipsis();
            case DOT:
                throw new ParserException("Unexpected dot");
            default:
                throw new RuntimeException("Nonexistent token found");
        }
    }

    private BaseNode keyword() {
        switch (lookahead.sequence) {
            case "class":
                if (tokens.get(1).is(TokenType.DESCRIPTOR, TokenType.KEYWORD)) {
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
            case "casted":
            case "in":
                throw new ParserException(lookahead+" must have a preceding token");
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
            case "cast":
                return cast_stmt();
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
        while (lookahead.is(TokenType.DESCRIPTOR)) {
            descriptors.add(new DescriptorNode(lookahead.sequence));
            NextToken();
        }
        switch (lookahead.token) {
            case KEYWORD:
                return descriptor_keyword(descriptors);
            case OPERATOR_SP:
                return descriptor_op_sp(descriptors);
            case NAME:
                return descriptor_var(descriptors);
            default:
                throw new ParserException("Invalid descriptor placement");
        }
    }

    private TestNode openBrace() {
        // Types of brace statement: comprehension, literal, grouping paren, casting
        TestNode stmt;
        if (lookahead.is("(")) {
            if (braceContains("for")) {
                stmt = comprehension();
            } else if (braceContains(",")) {
                stmt = literal();
            } else {
                NextToken(true);
                TestNode contained = test(true);
                mustToken("Unmatched brace", true, ")");
                stmt = contained;
            }
            if (lookahead.is("(")) {
                stmt = new FunctionCallNode(stmt, fn_call_args());
            }
        } else if (lookahead.is("[")) {
            if (braceContains("for")) {
                stmt = comprehension();
            } else {
                stmt = literal();
            }
        } else if (lookahead.is("{")) {
            if (braceContains("for")) {
                stmt = braceContains(":") ? dict_comprehension() : comprehension();
            } else if (braceContains(":")) {
                stmt = dict_literal();
            } else {
                stmt = literal();
            }
        } else {
            throw new RuntimeException("Some unknown brace was found");
        }
        if (lookahead.is(TokenType.DOT)) {
            return dotted_var(stmt);
        } else {
            return stmt;
        }
    }

    private SubTestNode subtest_openBrace() {
        BaseNode node = openBrace();
        if (node instanceof SubTestNode) {
            return (SubTestNode) node;
        }
        throw new ParserException("Unexpected " + lookahead);
    }

    private VariableNode name() {
        mustToken("Expected name, got " + lookahead, true, false,
                false, TokenType.NAME);
        String name = lookahead.sequence;
        NextToken();
        return new VariableNode(name);
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
        if (lineContains(TokenType.ASSIGN)) {
            return assignment();
        } else if (lineContains(TokenType.AUG_ASSIGN)) {
            DottedVariableNode var = left_name();
            mustToken("Expected augmented assignment, got " + lookahead, true,
                    false, false, TokenType.AUG_ASSIGN);
            OperatorNode op = new OperatorNode(lookahead.sequence.replaceAll("=$", ""));
            NextToken();
            TestNode assignment = test();
            return new AugmentedAssignmentNode(op, var, assignment);
        } else if (after_var.is("++", "--")) {
            return inc_or_dec();
        } else if (lineContains(TokenType.BOOL_OP, TokenType.OPERATOR)) {
            return test();
        } else if (after_var.is(TokenType.NAME)) {
            return declaration();
        } else {
            DottedVariableNode var = left_name();
            Newline();
            return var;
        }
    }

    private AssignStatementNode assignment() {
        ArrayList<TypeNode> var_type = new ArrayList<>();
        ArrayList<NameNode> vars = new ArrayList<>();
        while (!lookahead.is(TokenType.ASSIGN)) {
            if (!tokens.get(sizeOfVariable()).is(TokenType.ASSIGN, TokenType.COMMA)) {
                var_type.add(type());
                vars.add(name());
                if (lookahead.is(TokenType.ASSIGN)) {
                    break;
                }
                mustToken("Expected comma, got " + lookahead, true, TokenType.COMMA);
            } else {
                var_type.add(new TypeNode(new DottedVariableNode()));
                vars.add(left_name());
                if (lookahead.is(TokenType.ASSIGN)) {
                    break;
                }
                mustToken("Expected comma, got " + lookahead, true, TokenType.COMMA);
            }
        }
        boolean is_colon = lookahead.is(":=");
        NextToken();
        TestNode[] assignments = test_list(false);
        Newline();
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

    private DeclaredAssignmentNode decl_assignment() {
        AssignStatementNode assign = assignment();
        if (assign instanceof DeclaredAssignmentNode) {
            return (DeclaredAssignmentNode) assign;
        }
        throw new ParserException("Expected declared assignment, got normal assignment");
    }

    private SubTestNode left_operator(boolean ignore_newline) {
        assert lookahead.is(TokenType.OPERATOR);
        mustToken("- is the only unary operator", true, "-");
        TestNode next = test(ignore_newline);
        return new OperatorNode("-", next);
    }

    private AtomicNode string() {
        assert lookahead.is(TokenType.STRING);
        String contents = lookahead.sequence;
        NextToken();
        String inside = contents.replaceAll("(^[rfb]*)|(?<!\\\\)\"", "");
        Matcher regex = Pattern.compile("^[rfb]+").matcher(contents);
        if (regex.find()) {
            String prefixes = regex.group();
            if (prefixes.contains("f")) {
                LinkedList<String> strs = new LinkedList<>();
                LinkedList<TestNode> tests = new LinkedList<>();
                Matcher m = Pattern.compile("(?<!\\\\)(\\{([^{}]*)}?|})").matcher(inside);
                int index = 0;
                int start, end = 0;
                while (m.find()) {  // TODO: Find better way of handling this
                    start = m.start();
                    strs.add(inside.substring(index, start - 1));
                    StringBuilder to_test = new StringBuilder();
                    int netBraces = 0;
                    do {
                        String a = m.group();
                        to_test.append(a);
                        if (a.startsWith("{")) netBraces++;
                        if (a.endsWith("}")) netBraces--;
                        if (netBraces == 0) break;
                    } while (m.find());
                    if (netBraces > 0) {
                        throw new ParserException("Unmatched braces in "+lookahead.sequence);
                    }
                    end = m.end();
                    LinkedList<Token> oldTokens = tokens;
                    tokens = Tokenizer.parse(to_test.substring(1, to_test.length() - 1)).getTokens();
                    tokens.add(new Token(TokenType.EPSILON, ""));
                    lookahead = tokens.get(0);
                    tests.add(test());
                    mustToken("Unexpected " + lookahead, true,
                            false, false, TokenType.EPSILON);
                    tokens = oldTokens;
                    lookahead = tokens.get(0);
                    index = end + 1;
                }
                if (index <= inside.length()) {
                    strs.add(inside.substring(end));
                }
                return new FormattedStringNode(strs.toArray(new String[0]), tests.toArray(new TestNode[0]));
            }
            return new StringNode(inside, prefixes.toCharArray());
        }
        return new StringNode(contents);
    }

    private OperatorNode left_bool_op(boolean ignore_newline) {
        switch (lookahead.sequence) {
            case "not":
                NextToken(ignore_newline);
                return new OperatorNode("not", test());
            case "and":
            case "or":
            case "xor":
                throw new ParserException(lookahead+" must be in between statements");
            default:
                throw new RuntimeException("Unknown boolean operator");
        }
    }

    private OperatorDefinitionNode operator_sp() {
        String op_code = lookahead.sequence.replaceFirst("operator *", "");
        NextToken();
        if (lookahead.is(TokenType.ASSIGN)) {
            NextToken();
            if (lookahead.is(TokenType.OPERATOR_SP)) {
                OperatorTypeNode op = new OperatorTypeNode(lookahead.sequence);
                NextToken();
                Newline();
                return new OperatorDefinitionNode(op_code, new StatementBodyNode(op));
            } else if (lookahead.is(TokenType.NAME)) {
                NameNode var = left_name();
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

    private NumberNode number() {
        String value = lookahead.sequence;
        NextToken();
        if (value.length() < 2) {
            return new NumberNode(new BigDecimal(value));
        }
        int digit_size;
        switch (value.charAt(1)) {
            case 'x':
                digit_size = 16;
                break;
            case 'b':
                digit_size = 2;
                break;
            case 'o':
                digit_size = 8;
                break;
            default:
                return new NumberNode(new BigDecimal(value));
        }
        BigDecimal val = parse_int(value.substring(2), "0123465789abcdef".substring(0, digit_size));
        return new NumberNode(val);
    }

    private BigDecimal parse_int(String value, String digits) {
        int dot = value.indexOf('.');
        int exp_size = dot >= 0 ? dot - 1 : value.length() - 1;
        BigDecimal base = BigDecimal.valueOf(digits.length());
        value = value.replaceAll("\\.", "");
        BigDecimal val = new BigDecimal(0);
        for (int i = 0; i < value.length(); i++) {
            char digit = value.charAt(i);
            if (digits.indexOf(digit) == -1) {
                throw new ParserException(digit + " is not a valid digit");
            }
            BigDecimal digit_val = BigDecimal.valueOf(digits.indexOf(digit));
            if (exp_size - i >= 0) {
                val = val.add(base.pow(exp_size - i).multiply(digit_val));
            } else {
                val = val.add(BigDecimal.ONE.divide(base.pow(i - exp_size)).multiply(digit_val));
            }
        }
        return val;
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
        assert lookahead.is(TokenType.ELLIPSIS);
        NextToken();
        return new VariableNode("...");
    }

    private ClassDefinitionNode class_def() {
        assert lookahead.is("class");
        NextToken();
        if (!lookahead.is(TokenType.NAME) && !lookahead.is("from")) {
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
        VariableNode name = name();
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
        mustToken("Expected in, got "+lookahead, true, "in");
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
        mustToken("Do statements must have a corresponding while", true, "while");
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
        VariableNode name = name();
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
        if (lookahead.is(TokenType.NEWLINE)) {
            throw new ParserException("Empty import statements are illegal");
        }
        DottedVariableNode[] imports = var_list(true, false);
        Newline();
        return new ImportStatementNode(imports);
    }

    private ExportStatementNode export_stmt() {
        assert lookahead.is("export");
        NextToken();
        if (lookahead.is(TokenType.NEWLINE)) {
            throw new ParserException("Empty export statements are illegal");
        }
        DottedVariableNode[] exports = var_list(true,false);
        Newline();
        return new ExportStatementNode(exports);
    }

    private TypegetStatementNode typeget_stmt() {
        DottedVariableNode from = new DottedVariableNode();
        if (lookahead.is("from")) {
            NextToken();
            from = left_name();
        }
        assert lookahead.is("typeget");
        NextToken();
        if (lookahead.is(TokenType.NEWLINE)) {
            throw new ParserException("Empty typeget statements are illegal");
        }
        DottedVariableNode[] typegets = var_list(true, false);
        Newline();
        return new TypegetStatementNode(typegets, from);
    }

    private BreakStatementNode break_stmt() {
        assert lookahead.is("break");
        NextToken();
        int loops;
        if (lookahead.is(TokenType.NUMBER)) {
            loops = Integer.parseInt(lookahead.sequence);
            NextToken();
        } else if (lookahead.is(TokenType.NEWLINE) || lookahead.is("if")) {
            loops = 0;
        } else {
            throw new ParserException("Break statement must not be followed by anything");
        }
        TestNode cond = null;
        if (lookahead.is("if")) {
            NextToken();
            cond = test();
        }
        Newline();
        return new BreakStatementNode(loops, cond);
    }

    private ContinueStatementNode continue_stmt() {
        assert lookahead.is("continue");
        NextToken();
        TestNode cond = null;
        if (lookahead.is("if")) {
            NextToken();
            cond = test();
        }
        Newline();
        return new ContinueStatementNode(cond);
    }

    private ReturnStatementNode return_stmt() {
        assert lookahead.is("return");
        NextToken();
        boolean is_conditional = false;
        if (lookahead.is("(") && tokens.get(sizeOfBrace(0) + 1).is("if")
                && !lineContains("else")) {
            NextToken();
            is_conditional = true;
        }
        TestNode[] returned;
        if (!lookahead.is(TokenType.NEWLINE) && !lookahead.is("if")) {
            returned = test_list(false);
        } else {
            returned = new TestNode[0];
        }
        TestNode cond = null;
        if (is_conditional) {
            mustToken("Expected ), got " + lookahead, true, ")");
            cond = test();
        }
        Newline();
        return new ReturnStatementNode(returned, cond);
    }

    private BaseNode descriptor_keyword(ArrayList<DescriptorNode> descriptors) {
        assert lookahead.is(TokenType.KEYWORD);
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
                throw new ParserException("Expected descriptor-usable keyword, got "+lookahead);
            default:
                throw new RuntimeException("Keyword mismatch");
        }
        node.addDescriptor(descriptors.toArray(new DescriptorNode[0]));
        return node;
    }

    private BaseNode descriptor_op_sp(ArrayList<DescriptorNode> descriptors) {
        assert lookahead.is(TokenType.OPERATOR_SP);
        OperatorDefinitionNode op_sp = operator_sp();
        op_sp.addDescriptor(descriptors.toArray(new DescriptorNode[0]));
        return op_sp;
    }

    private ClassStatementNode descriptor_var(ArrayList<DescriptorNode> descriptors) {
        assert lookahead.is(TokenType.NAME);
        ClassStatementNode stmt;
        if (lineContains(TokenType.ASSIGN)) {
            stmt = decl_assignment();
        } else {
            stmt = declaration();
        }
        stmt.addDescriptor(descriptors.toArray(new DescriptorNode[0]));
        return stmt;
    }

    private TypedArgumentListNode fn_args() {
        mustToken("Argument lists must start with an open-paren", true, "(");
        ArrayList<TypedArgumentNode> args = new ArrayList<>();
        while (!lookahead.is(")")) {
            args.add(typed_argument());
            if (lookahead.is(TokenType.COMMA)) {
                NextToken();
                if (lookahead.is(TokenType.NEWLINE)) {
                    NextToken();
                }
                continue;
            }
            if (lookahead.is(TokenType.NEWLINE)) {
                NextToken();
            }
            mustToken("Comma must separate arguments", true, false, false, ")");
        }
        NextToken();
        return new TypedArgumentListNode(args.toArray(new TypedArgumentNode[0]));
    }

    private StatementBodyNode fn_body() {
        mustToken("The body of a function must be enclosed in curly brackets", true,
                true, true, "{");
        ArrayList<BaseNode> statements = new ArrayList<>();
        while (!lookahead.is("}")) {
            statements.add(statement());
            passNewlines();
        }
        NextToken();
        return new StatementBodyNode(statements.toArray(new BaseNode[0]));
    }

    private ClassBodyNode class_body() {
        mustToken("The body of a class must be enclosed in curly brackets", true,
                true, true, "{");
        ArrayList<ClassStatementNode> statements = new ArrayList<>();
        while (!lookahead.is("}")) {
            statements.add(class_statement());
            passNewlines();
        }
        NextToken();
        Newline();
        return new ClassBodyNode(statements.toArray(new ClassStatementNode[0]));
    }

    private TypeNode[] fn_retval() {
        if (lookahead.is("{")) {
            return new TypeNode[0];
        }
        mustToken("Return value must use arrow operator", true, "->");
        LinkedList<TypeNode> types = new LinkedList<>();
        while (!lookahead.is("{") && !lookahead.is(TokenType.NEWLINE)) {
            types.add(type());
            if (lookahead.is(",")) {
                NextToken();
            }
        }
        return types.toArray(new TypeNode[0]);
    }

    private TestNode test(boolean ignore_newline) {
        SubTestNode if_true = test_no_ternary(ignore_newline);
        if (lookahead.is("if")) {
            NextToken(ignore_newline);
            SubTestNode statement = test_no_ternary(ignore_newline);
            mustToken("Ternary must have an else", true,
                    true, ignore_newline, "else");
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
            case NAME:
                return test_left_variable(ignore_newline);
            case OPEN_BRACE:
                return subtest_openBrace();
            case BOOL_OP:
                return left_bool_op(ignore_newline);
            case NUMBER:
                return number();
            case OP_FUNC:
                return op_func();
            case ELLIPSIS:
                return ellipsis();
            case OPERATOR:
                return left_operator(ignore_newline);
            case KEYWORD:
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
        if (lineContains(TokenType.ASSIGN)) {
            throw new ParserException("Illegal assignment");
        } else if (contains(ignore_newline, TokenType.AUG_ASSIGN)) {
            throw new ParserException("Illegal augmented assignment");
        } else {
            LinkedList<SubTestNode> nodes = new LinkedList<>();
            while_loop:
            while (!lookahead.is(TokenType.NEWLINE)) {
                switch (lookahead.token) {
                    case OPEN_BRACE:
                        SubTestNode last_node = nodes.peekLast();
                        if (last_node instanceof OperatorNode) {
                            nodes.add(subtest_openBrace());
                            break;
                        }
                        break while_loop;
                    case NAME:
                        nodes.add(left_name());
                        break;
                    case NUMBER:
                        nodes.add(number());
                        break;
                    case BOOL_OP:
                        nodes.add(new OperatorNode(lookahead.sequence));
                        NextToken();
                        break;
                    case OPERATOR:
                        if (lookahead.is("->")) {
                            throw new ParserException("Unexpected ->");
                        }
                        nodes.add(new OperatorNode(lookahead.sequence));
                        NextToken();
                        break;
                    case OP_FUNC:
                        nodes.add(op_func());
                        break;
                    case EPSILON:
                    case COLON:
                    case COMMA:
                        break while_loop;
                    case KEYWORD:
                        if (lookahead.is("in", "casted")) {
                            nodes.add(new OperatorNode(lookahead.sequence));
                            NextToken();
                            break;
                        } else if (lookahead.is("if", "else")) {
                            break while_loop;
                        }  // Lack of breaks here is intentional
                    case CLOSE_BRACE:
                        if (ignore_newline) {
                            break while_loop;
                        }
                    default:
                        throw new ParserException("Unexpected "+lookahead);
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
            parseExpression(nodes, "casted");
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

    private TestNode[] var_index() {
        mustToken("Expected [, got " + lookahead, true,
                false, false, "[");
        TestNode[] vars;
        if (braceContains(":")) {
            vars = new TestNode[]{slice()};
        } else {
            vars = literal().getBuilders();
        }
        return vars;
    }

    private TypeNode type() {
        return type(false, false);
    }

    private TypeNode type(boolean allow_empty, boolean is_vararg) {
        DottedVariableNode main;
        if (!lookahead.is(TokenType.NAME)) {
            if (allow_empty && lookahead.is("[")) {
                main = new DottedVariableNode();
            } else {
                throw new ParserException("Expected type name, got " + lookahead);
            }
        } else {
            main = dotted_name();
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
            if (lookahead.is(TokenType.COMMA)) {
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
            if (!lookahead.is(TokenType.NAME)) {
                throw new ParserException("Expected variable, got " + lookahead);
            }
            vars.add(typed_variable());
            if (lookahead.is(TokenType.COMMA)) {
                NextToken();
            }
        }
        return vars.toArray(new TypedVariableNode[0]);
    }

    private TestNode[] for_iterables() {
        if (lookahead.is(TokenType.NEWLINE)) {
            return new TestNode[0];
        }
        LinkedList<TestNode> tests = new LinkedList<>();
        while (!lookahead.is("{")) {
            tests.add(test());
            if (lookahead.is(TokenType.COMMA)) {
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
            if (!tokens.get(brace_size).is(TokenType.COMMA)) {
                NextToken();
                TestNode[] vars = test_list(true);
                if (!lookahead.is(")")) {
                    throw new ParserException("Unmatched braces");
                }
                NextToken();
                return vars;
            }
        }
        if (!ignore_newlines && lookahead.is(TokenType.NEWLINE)) {
            return new TestNode[0];
        }
        LinkedList<TestNode> tests = new LinkedList<>();
        while (!lookahead.is(TokenType.NEWLINE)) {
            tests.add(test(ignore_newlines));
            if (lookahead.is(TokenType.COMMA)) {
                NextToken(ignore_newlines);
                continue;
            }
            if (!ignore_newlines && !lookahead.is(TokenType.NEWLINE)) {
                throw new ParserException("Comma must separate values");
            } else if (ignore_newlines) {
                break;
            }
        }
        if (!ignore_newlines && !lookahead.is(TokenType.NEWLINE)) {
            throw new ParserException("Expected newline, got "+lookahead);
        }
        return tests.toArray(new TestNode[0]);
    }

    private TypedArgumentNode typed_argument() {
        boolean is_vararg = lookahead.is("*", "**");
        String vararg_type;
        if (lookahead.is("*", "**")) {
            vararg_type = lookahead.sequence;
            NextToken();
        } else {
            vararg_type = "";
        }
        TypeNode type = type();
        VariableNode var = name();
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
        throw new ParserException(lookahead+" is not a valid class statement");
    }

    private DeclarationNode declaration() {
        TypeNode type = type();
        VariableNode var = name();
        return new DeclarationNode(type, var);
    }

    private PropertyDefinitionNode property_stmt() {
        VariableNode name = new VariableNode();
        if (!lookahead.is("{")) {
            name = name();
        }
        NextToken(true);
        StatementBodyNode get = new StatementBodyNode();
        StatementBodyNode set = new StatementBodyNode();
        TypedArgumentListNode set_args = new TypedArgumentListNode();
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
        if (!lookahead.is(TokenType.OPEN_BRACE)) {  // Comprehensions in function calls
            brace_type = "";
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
        if (!brace_type.isEmpty() && !lookahead.is(matching_brace(brace_type))) {
            throw new ParserException("Expected close brace");
        }
        NextToken();
        TestNode[] looped_array = looped.toArray(new TestNode[0]);
        return new ComprehensionNode(brace_type, variables, builder, looped_array);
    }

    private DictComprehensionNode dict_comprehension() {
        assert lookahead.is("{");
        NextToken(true);
        TestNode key = test(true);
        if (!lookahead.is(":")) {
            throw new ParserException("Expected :, got "+lookahead);
        }
        NextToken(true);
        TestNode val = test(true);
        if (!lookahead.is("for")) {
            throw new ParserException("Expected for, got "+lookahead);
        }
        NextToken();
        TypedVariableNode[] vars = comp_params();
        if (!lookahead.is("in")) {
            throw new ParserException("Expected in, got "+lookahead);
        }
        NextToken();
        TestNode[] looped = test_list(true);
        if (!lookahead.is("}")) {
            throw new ParserException("Expected }, got "+lookahead);
        }
        NextToken();
        return new DictComprehensionNode(key, val, vars, looped);
    }

    private String matching_brace(String brace) {
        switch (brace) {
            case "(":
                return ")";
            case "[":
                return "]";
            case "{":
                return "}";
            default:
                throw new RuntimeException("Unknown brace "+brace);
        }
    }

    private LiteralNode literal() {
        assert lookahead.is(TokenType.OPEN_BRACE);
        String brace_type = lookahead.sequence;
        NextToken(true);
        String balanced_brace = matching_brace(brace_type);
        LinkedList<TestNode> tokens = new LinkedList<>();
        LinkedList<Boolean> is_splat = new LinkedList<>();
        while (true) {
            if (lookahead.is(balanced_brace)) {
                break;
            } else if (lookahead.is(TokenType.CLOSE_BRACE)) {
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

    private DottedVariableNode[] var_list(boolean allow_dotted, boolean ignore_newlines) {
        LinkedList<DottedVariableNode> variables = new LinkedList<>();
        if (ignore_newlines) {
            passNewlines();
        }
        if (lookahead.is("(") && !braceContains("in") && !braceContains("for")) {
            NextToken();
            DottedVariableNode[] vars = var_list(allow_dotted, true);
            if (!lookahead.is(")")) {
                throw new ParserException("Unmatched braces");
            }
            return vars;
        }
        while (true) {
            if (!lookahead.is(TokenType.NAME)) {
                break;
            }
            if (lookahead.is(TokenType.CLOSE_BRACE)) {
                throw new ParserException("Unmatched braces");
            }
            variables.add(left_name());
            if (lookahead.is(",")) {
                NextToken(ignore_newlines);
            } else {
                break;
            }
        }
        return variables.toArray(new DottedVariableNode[0]);
    }

    private DictLiteralNode dict_literal() {
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
        return new DictLiteralNode(keys.toArray(new TestNode[0]), values.toArray(new TestNode[0]));
    }

    private ContextDefinitionNode context_def() {
        assert lookahead.is("context");
        NextToken();
        VariableNode name = new VariableNode();
        if (lookahead.is(TokenType.NAME)) {
            name = name();
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
        DottedVariableNode[] excepted = new DottedVariableNode[0];
        VariableNode as_var = new VariableNode();
        StatementBodyNode else_stmt = new StatementBodyNode();
        StatementBodyNode finally_stmt = new StatementBodyNode();
        if (lookahead.is("except")) {
            NextToken();
            excepted = var_list(true, false);
            if (lookahead.is("as")) {
                NextToken();
                as_var = name();
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
            if (lookahead.is(TokenType.COMMA)) {
                NextToken();
            } else if (!lookahead.is("as")) {
                throw new ParserException("Expected comma or as, got "+lookahead);
            }
        }
        DottedVariableNode[] vars = var_list(false, false);
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
        while (!lookahead.is(TokenType.NEWLINE)) {
            yields.add(test());
            if (lookahead.is(TokenType.COMMA)) {
                NextToken();
                continue;
            }
            if (!lookahead.is(TokenType.NEWLINE)) {
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
                var = name();
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
                throw new ParserException("Expected comma, got "+lookahead);
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
            throw new ParserException("Expected an in, got "+lookahead);
        }
        OperatorNode in_stmt = (OperatorNode) contained;
        if (!in_stmt.getOperator().equals("in")) {
            throw new ParserException("Expected an in, got "+lookahead);
        }
        TestNode[] operands = in_stmt.getOperands();
        return new SomeStatementNode(operands[0], operands[1]);
    }

    private TypedVariableNode typed_variable() {
        TypeNode type = type();
        VariableNode var = name();
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
        NameNode var = left_name();
        if (!lookahead.is("++")) {
            throw new RuntimeException("Expected ++, got "+lookahead);
        }
        NextToken();
        return new IncrementNode(var);
    }

    private DecrementNode decrement() {
        NameNode var = left_name();
        if (!lookahead.is("--")) {
            throw new RuntimeException("Expected --, got "+lookahead);
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
                throw new ParserException("Unexpected "+lookahead);
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
            throw new ParserException("Expected ], got "+lookahead);
        }
        NextToken();
        return new SliceNode(start, end, step);
    }

    private TestNode slice_test() {
        if (!lookahead.is(":")) {
            throw new ParserException("Expected :, got "+lookahead);
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
        if (lookahead.is(TokenType.OPERATOR_SP, TokenType.DESCRIPTOR) || lookahead.is("method")
              || (lookahead.is("class") && tokens.get(1).is(TokenType.DESCRIPTOR, TokenType.KEYWORD))) {
            LinkedList<DescriptorNode> descriptors = new LinkedList<>();
            if (lookahead.is("class")) {
                descriptors.add(new DescriptorNode("class"));
                NextToken();
            }
            while (lookahead.is(TokenType.DESCRIPTOR)) {
                descriptors.add(new DescriptorNode(lookahead.sequence));
                NextToken();
            }
            int line_size = 0;
            for (Token token : tokens) {
                if (token.is(TokenType.NEWLINE)) {
                    break;
                }
                line_size++;
            }
            if (!tokens.get(line_size - 1).is("{")) {
                boolean is_operator;
                VariableNode fn_name;
                String op_name;
                if (lookahead.is(TokenType.OPERATOR_SP)) {
                    is_operator = true;
                    op_name = lookahead.sequence;
                    fn_name = new VariableNode();
                    NextToken();
                } else {
                    NextToken();
                    is_operator = false;
                    fn_name = name();
                    op_name = "";
                }
                TypedArgumentListNode args = fn_args();
                TypeNode[] retvals = new TypeNode[0];
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

    // TODO: Remove this
    private CastStatementNode cast_stmt() {
        assert lookahead.is("cast");
        NextToken();
        VariableNode casted = name();
        if (!lookahead.is("to")) {
            throw new ParserException("Expected to, got "+lookahead);
        }
        NextToken();
        TypeNode type = type();
        VariableNode new_name;
        if (lookahead.is("as")) {
            NextToken();
            new_name = name();
        } else {
            new_name = new VariableNode();
        }
        Newline();
        return new CastStatementNode(casted, type, new_name);
    }

    private DottedVariableNode dotted_var(TestNode preDot) {
        assert lookahead.is(TokenType.DOT);
        NextToken();
        LinkedList<NameNode> postDot = new LinkedList<>();
        while (lookahead.is(TokenType.NAME)) {
            NameNode name = name();
            while (lookahead.is(TokenType.OPEN_BRACE)) {
                if (lookahead.is("(")) {
                    ArgumentNode[] args = fn_call_args();
                    name = new FunctionCallNode(name, args);
                } else if (lookahead.is("[")) {
                    name = new IndexNode(name, var_index());
                } else if (lookahead.is("{")) {
                    break;
                } else {
                    throw new ParserException("Unexpected " + lookahead);
                }
            }
            if (lookahead.is(TokenType.DOT)) {
                postDot.add(name);
                NextToken();
            }
        }
        return new DottedVariableNode(preDot, postDot.toArray(new NameNode[0]));
    }

    private DottedVariableNode left_name() {
        assert lookahead.is(TokenType.NAME);
        NameNode name = name();
        while_loop:
        while (lookahead.is(TokenType.OPEN_BRACE)) {
            switch (lookahead.sequence) {
                case "(":
                    name = new FunctionCallNode(name, fn_call_args());
                    break;
                case "[":
                    if (braceContains(":")) {
                        name = new IndexNode(name, slice());
                    } else {
                        name = new IndexNode(name, literal().getBuilders());
                    }
                    break;
                case "{":
                    break while_loop;
                default:
                    throw new RuntimeException("Unexpected brace");
            }
        }
        if (lookahead.is(TokenType.DOT)) {
            return dotted_var(name);
        } else {
            return new DottedVariableNode(name);
        }
    }

    private DottedVariableNode dotted_name() {
        LinkedList<VariableNode> names = new LinkedList<>();
        while (lookahead.is(TokenType.NAME)) {
            names.add(name());
            if (!lookahead.is(TokenType.DOT)) {
                break;
            }
            NextToken();
        }
        return new DottedVariableNode(names.removeFirst(), names.toArray(new VariableNode[0]));
    }
}
