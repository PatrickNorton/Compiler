// TODO! Rename getName() to not conflict with standard name
// TODO: Reduce/remove nulls
// TODO? Remove SubTestNode & replace with something more meaningful
// TODO: operator + = (something)
// FIXME: Allow self/cls declarations
// TODO: Annotations
// TODO: Refactor and split up


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
    // private LinkedList<Token> tokens;
    // private Token lookahead;

    private TokenList tokens;

    public Parser(LinkedList<Token> tokens) {
        this.tokens = new TokenList(tokens);
        // this.lookahead = tokens.getFirst();
    }

    public static TopNode parse(LinkedList<Token> tokens) {
        Parser parser = new Parser(tokens);
        TopNode topNode = new TopNode();
        while (!parser.tokens.tokenIs(TokenType.EPSILON)) {
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
        int netBraces = this.tokens.tokenIs(TokenType.OPEN_BRACE) ? 0 : 1;
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
        assert tokens.tokenIs(offset, TokenType.NAME);
        int netBraces = 0;
        boolean wasVar = false;
        for (int size = offset;; size++) {
            Token token = tokens.getToken(size);
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
        tokens.nextToken(ignore_newline);
    }

    private void NextToken() {
        NextToken(false);
    }

    private void Newline() {
        if (!tokens.tokenIs(TokenType.NEWLINE)) {
            throw new ParserException("Expected newline, got "+tokens.getFirst());
        }
        NextToken();
    }

    private void passNewlines() {
        while (!tokens.isEmpty() && tokens.tokenIs(TokenType.NEWLINE)) {
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
        if (!this.tokens.tokenIs(tokens)) {
            throw (parser ? new ParserException(message) : new RuntimeException(message));
        }
        if (nextToken) {
            NextToken(ignore_newline);
        }
    }

    private void mustToken(String message, boolean parser, boolean nextToken, boolean ignore_newline, TokenType... tokens) {
        if (!this.tokens.tokenIs(tokens)) {
            throw (parser ? new ParserException(message) : new RuntimeException(message));
        }
        if (nextToken) {
            NextToken(ignore_newline);
        }
    }

    private BaseNode statement() {
        passNewlines();
        switch (tokens.getFirst().token) {
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
            case ARROW:
                throw new ParserException("Unexpected ->");
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
        switch (tokens.getFirst().sequence) {
            case "class":
                if (tokens.tokenIs(1, TokenType.DESCRIPTOR, TokenType.KEYWORD)) {
                    return descriptor();
                }
                return class_def();
            case "func":
                return func_def();
            case "if":
                return if_stmt();
            case "for":
                return for_stmt();
            case "elif":
            case "else":
                throw new ParserException(tokens.getFirst()+" must have a preceding if");
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
                throw new ParserException(tokens.getFirst()+" must have a preceding token");
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
        if (tokens.tokenIs("class")) {
            descriptors.add(new DescriptorNode("class"));
            NextToken();
        }
        while (tokens.tokenIs(TokenType.DESCRIPTOR)) {
            descriptors.add(new DescriptorNode(tokens.getFirst().sequence));
            NextToken();
        }
        switch (tokens.getFirst().token) {
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
        if (tokens.tokenIs("(")) {
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
            if (tokens.tokenIs("(")) {
                stmt = new FunctionCallNode(stmt, fn_call_args());
            }
        } else if (tokens.tokenIs("[")) {
            if (braceContains("for")) {
                stmt = comprehension();
            } else {
                stmt = literal();
            }
        } else if (tokens.tokenIs("{")) {
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
        if (tokens.tokenIs(TokenType.DOT)) {
            return dotted_var(stmt);
        } else {
            return stmt;
        }
    }

    private VariableNode name() {
        mustToken("Expected name, got " + tokens.getFirst(), true, false,
                false, TokenType.NAME);
        String name = tokens.getFirst().sequence;
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
        Token after_var = tokens.getToken(sizeOfVariable());
        if (lineContains(TokenType.ASSIGN)) {
            return assignment();
        } else if (lineContains(TokenType.AUG_ASSIGN)) {
            DottedVariableNode var = left_name();
            mustToken("Expected augmented assignment, got " + tokens.getFirst(), true,
                    false, false, TokenType.AUG_ASSIGN);
            OperatorNode op = new OperatorNode(tokens.getFirst().sequence.replaceAll("=$", ""));
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
        while (!tokens.tokenIs(TokenType.ASSIGN)) {
            if (!tokens.getToken(sizeOfVariable()).is(TokenType.ASSIGN, TokenType.COMMA)) {
                var_type.add(type());
                vars.add(name());
                if (tokens.tokenIs(TokenType.ASSIGN)) {
                    break;
                }
                mustToken("Expected comma, got " + tokens.getFirst(), true, TokenType.COMMA);
            } else {
                var_type.add(new TypeNode(new DottedVariableNode()));
                vars.add(left_name());
                if (tokens.tokenIs(TokenType.ASSIGN)) {
                    break;
                }
                mustToken("Expected comma, got " + tokens.getFirst(), true, TokenType.COMMA);
            }
        }
        boolean is_colon = tokens.tokenIs(":=");
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
        assert tokens.tokenIs(TokenType.OPERATOR);
        mustToken("- is the only unary operator", true, "-");
        TestNode next = test(ignore_newline);
        return new OperatorNode("-", next);
    }

    private AtomicNode string() {
        assert tokens.tokenIs(TokenType.STRING);
        String contents = tokens.getFirst().sequence;
        NextToken();
        String inside = contents.replaceAll("(^[rfb]*\")|(?<!\\\\)\"", "");
        Matcher regex = Pattern.compile("^[rfb]+").matcher(contents);
        if (regex.find()) {
            String prefixes = regex.group();
            if (!prefixes.contains("r")) {
                inside = process_escapes(inside);
            }
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
                        throw new ParserException("Unmatched braces in "+tokens.getFirst().sequence);
                    }
                    end = m.end();
                    TokenList oldTokens = tokens;
                    LinkedList<Token> tokenList = Tokenizer.parse(to_test.substring(1, to_test.length() - 1)).getTokens();
                    tokenList.add(new Token(TokenType.EPSILON, ""));
                    tokens = new TokenList(tokenList);
                    tests.add(test());
                    mustToken("Unexpected " + tokens.getFirst(), true,
                            false, false, TokenType.EPSILON);
                    tokens = oldTokens;
                    index = end + 1;
                }
                if (index <= inside.length()) {
                    strs.add(inside.substring(end));
                }
                return new FormattedStringNode(strs.toArray(new String[0]), tests.toArray(new TestNode[0]));
            }
            return new StringNode(inside, prefixes.toCharArray());
        }
        inside = process_escapes(inside);
        return new StringNode(inside);
    }

    private String process_escapes(String str) {
        StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            char chr = str.charAt(i);
            if (chr != '\\') {
                sb.append(chr);
                continue;
            }
            char chr2 = str.charAt(i+1);
            switch (chr2) {
                case '\\':
                    sb.append('\\');
                    break;
                case '"':
                    sb.append('"');
                    break;
                case '\'':
                    sb.append('\'');
                    break;
                case 'a':
                    sb.append('\7');
                    break;
                case 'b':
                    sb.append('\b');
                    break;
                case 'f':
                    sb.append('\f');
                    break;
                case 'n':
                    sb.append('\n');
                    break;
                case 'r':
                    sb.append('\r');
                    break;
                case 't':
                    sb.append('\t');
                    break;
                case 'v':
                    sb.append('\013');
                    break;
                case 'o':
                    sb.append(Integer.parseInt(str.substring(i + 2, i + 4), 8));
                    i += 3;
                    break;
                case 'x':
                    sb.append(Integer.parseInt(str.substring(i + 2, i + 3), 16));
                    i += 2;
                    break;
                case 'u':
                    sb.append(Integer.parseInt(str.substring(i + 2, i + 5), 16));
                    i += 4;
                    break;
                case 'U':
                    sb.append(Integer.parseInt(str.substring(i + 2, i + 9), 16));
                    i += 8;
                    break;
                default:
                    throw new ParserException("Unknown escape sequence " + str.substring(i, i+1));
            }
            i++;
        }
        return sb.toString();
    }

    private OperatorNode left_bool_op(boolean ignore_newline) {
        switch (tokens.getFirst().sequence) {
            case "not":
                NextToken(ignore_newline);
                return new OperatorNode("not", test());
            case "and":
            case "or":
            case "xor":
                throw new ParserException(tokens.getFirst()+" must be in between statements");
            default:
                throw new RuntimeException("Unknown boolean operator");
        }
    }

    private OperatorDefinitionNode operator_sp() {
        String op_code = tokens.getFirst().sequence.replaceFirst("operator *", "");
        NextToken();
        if (tokens.tokenIs(TokenType.ASSIGN)) {
            NextToken();
            if (tokens.tokenIs(TokenType.OPERATOR_SP)) {
                OperatorTypeNode op = new OperatorTypeNode(tokens.getFirst().sequence);
                NextToken();
                Newline();
                return new OperatorDefinitionNode(op_code, new StatementBodyNode(op));
            } else if (tokens.tokenIs(TokenType.NAME)) {
                NameNode var = left_name();
                Newline();
                return new OperatorDefinitionNode(op_code, new StatementBodyNode(var));
            } else {
                throw new ParserException("Operator equivalence must be done to another var or op");
            }
        }
        TypedArgumentListNode args;
        if (tokens.tokenIs("(")) {
            args = fn_args();
        } else {
            args = new TypedArgumentListNode();
        }
        TypeNode[] retval;
        if (tokens.tokenIs(TokenType.ARROW)) {
            retval = fn_retval();
        } else {
            retval = new TypeNode[0];
        }
        StatementBodyNode body = fn_body();
        return new OperatorDefinitionNode(op_code, retval, args, body);
    }

    private NumberNode number() {
        String value = tokens.getFirst().sequence;
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
        String op_code = tokens.getFirst().sequence.replaceFirst("\\\\", "");
        NextToken();
        if (tokens.tokenIs("(")) {
            return new OperatorNode(op_code, fn_args());
        } else {
            return new OperatorTypeNode(op_code);
        }
    }

    private VariableNode ellipsis() {
        assert tokens.tokenIs(TokenType.ELLIPSIS);
        NextToken();
        return new VariableNode("...");
    }

    private ClassDefinitionNode class_def() {
        assert tokens.tokenIs("class");
        NextToken();
        if (!tokens.tokenIs(TokenType.NAME) && !tokens.tokenIs("from")) {
            throw new ParserException("class keyword must be followed by class name");
        }
        TypeNode name = type();
        LinkedList<TypeNode> superclasses = new LinkedList<>();
        while (tokens.tokenIs("from")) {
            NextToken();
            superclasses.add(type());
        }
        return new ClassDefinitionNode(name, superclasses.toArray(new TypeNode[0]), class_body());
    }

    private FunctionDefinitionNode func_def() {
        assert tokens.tokenIs("func");
        NextToken();
        VariableNode name = name();
        TypedArgumentListNode args = fn_args();
        TypeNode[] retval = fn_retval();
        StatementBodyNode body = fn_body();
        Newline();
        return new FunctionDefinitionNode(name, args, retval, body);
    }

    private IfStatementNode if_stmt() {
        assert tokens.tokenIs("if");
        NextToken();
        TestNode test = test();
        StatementBodyNode body = fn_body();
        LinkedList<ElifStatementNode> elifs = new LinkedList<>();
        while (tokens.tokenIs("elif")) {
            NextToken();
            TestNode elif_test = test();
            StatementBodyNode elif_body = fn_body();
            elifs.add(new ElifStatementNode(elif_test, elif_body));
        }
        StatementBodyNode else_stmt = new StatementBodyNode();
        if (tokens.tokenIs("else")) {
            NextToken();
            else_stmt = fn_body();
        }
        Newline();
        return new IfStatementNode(test, body, elifs.toArray(new ElifStatementNode[0]), else_stmt);
    }

    private ForStatementNode for_stmt() {
        assert tokens.tokenIs("for");
        NextToken();
        TypedVariableNode[] vars = for_vars();
        mustToken("Expected in, got "+tokens.getFirst(), true, "in");
        TestNode[] iterables = for_iterables();
        StatementBodyNode body = fn_body();
        StatementBodyNode nobreak = new StatementBodyNode();
        if (tokens.tokenIs("nobreak")) {
            NextToken();
            nobreak = new StatementBodyNode(fn_body());
        }
        Newline();
        return new ForStatementNode(vars, iterables, body, nobreak);
    }

    private DoStatementNode do_stmt() {
        assert tokens.tokenIs("do");
        NextToken();
        StatementBodyNode body = fn_body();
        mustToken("Do statements must have a corresponding while", true, "while");
        TestNode conditional = test();
        Newline();
        return new DoStatementNode(body, conditional);
    }

    private DotimesStatementNode dotimes_stmt() {
        assert tokens.tokenIs("dotimes");
        NextToken();
        TestNode iterations = test();
        StatementBodyNode body = fn_body();
        StatementBodyNode nobreak = new StatementBodyNode();
        if (tokens.tokenIs("nobreak")) {
            NextToken();
            nobreak = new StatementBodyNode(fn_body());
        }
        Newline();
        return new DotimesStatementNode(iterations, body, nobreak);
    }

    private MethodDefinitionNode method_def() {
        assert tokens.tokenIs("method");
        NextToken();
        VariableNode name = name();
        TypedArgumentListNode args = fn_args();
        TypeNode[] retval = fn_retval();
        StatementBodyNode body = fn_body();
        Newline();
        return new MethodDefinitionNode(name, args, retval, body);
    }

    private WhileStatementNode while_stmt() {
        assert tokens.tokenIs("while");
        NextToken();
        TestNode cond = test();
        StatementBodyNode body = fn_body();
        StatementBodyNode nobreak = new StatementBodyNode();
        if (tokens.tokenIs("nobreak")) {
            NextToken();
            nobreak = new StatementBodyNode(fn_body());
        }
        Newline();
        return new WhileStatementNode(cond, body, nobreak);
    }

    private ImportStatementNode import_stmt() {
        assert tokens.tokenIs("import");
        NextToken();
        if (tokens.tokenIs(TokenType.NEWLINE)) {
            throw new ParserException("Empty import statements are illegal");
        }
        DottedVariableNode[] imports = var_list(false);
        Newline();
        return new ImportStatementNode(imports);
    }

    private ExportStatementNode export_stmt() {
        assert tokens.tokenIs("export");
        NextToken();
        if (tokens.tokenIs(TokenType.NEWLINE)) {
            throw new ParserException("Empty export statements are illegal");
        }
        DottedVariableNode[] exports = var_list(false);
        Newline();
        return new ExportStatementNode(exports);
    }

    private TypegetStatementNode typeget_stmt() {
        DottedVariableNode from = new DottedVariableNode();
        if (tokens.tokenIs("from")) {
            NextToken();
            from = left_name();
        }
        assert tokens.tokenIs("typeget");
        NextToken();
        if (tokens.tokenIs(TokenType.NEWLINE)) {
            throw new ParserException("Empty typeget statements are illegal");
        }
        DottedVariableNode[] typegets = var_list(false);
        Newline();
        return new TypegetStatementNode(typegets, from);
    }

    private BreakStatementNode break_stmt() {
        assert tokens.tokenIs("break");
        NextToken();
        int loops;
        if (tokens.tokenIs(TokenType.NUMBER)) {
            loops = Integer.parseInt(tokens.getFirst().sequence);
            NextToken();
        } else if (tokens.tokenIs(TokenType.NEWLINE) || tokens.tokenIs("if")) {
            loops = 0;
        } else {
            throw new ParserException("Break statement must not be followed by anything");
        }
        TestNode cond = null;
        if (tokens.tokenIs("if")) {
            NextToken();
            cond = test();
        }
        Newline();
        return new BreakStatementNode(loops, cond);
    }

    private ContinueStatementNode continue_stmt() {
        assert tokens.tokenIs("continue");
        NextToken();
        TestNode cond = null;
        if (tokens.tokenIs("if")) {
            NextToken();
            cond = test();
        }
        Newline();
        return new ContinueStatementNode(cond);
    }

    private ReturnStatementNode return_stmt() {
        assert tokens.tokenIs("return");
        NextToken();
        boolean is_conditional = false;
        if (tokens.tokenIs("(") && tokens.getToken(sizeOfBrace(0) + 1).is("if")
                && !lineContains("else")) {
            NextToken();
            is_conditional = true;
        }
        TestNode[] returned;
        if (!tokens.tokenIs(TokenType.NEWLINE) && !tokens.tokenIs("if")) {
            returned = test_list(false);
        } else {
            returned = new TestNode[0];
        }
        TestNode cond = null;
        if (is_conditional) {
            mustToken("Expected ), got " + tokens.getFirst(), true, ")");
            cond = test();
        }
        Newline();
        return new ReturnStatementNode(returned, cond);
    }

    private BaseNode descriptor_keyword(ArrayList<DescriptorNode> descriptors) {
        assert tokens.tokenIs(TokenType.KEYWORD);
        ClassStatementNode node;
        switch (tokens.getFirst().sequence) {
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
                throw new ParserException("Expected descriptor-usable keyword, got "+tokens.getFirst());
            default:
                throw new RuntimeException("Keyword mismatch");
        }
        node.addDescriptor(descriptors.toArray(new DescriptorNode[0]));
        return node;
    }

    private BaseNode descriptor_op_sp(ArrayList<DescriptorNode> descriptors) {
        assert tokens.tokenIs(TokenType.OPERATOR_SP);
        OperatorDefinitionNode op_sp = operator_sp();
        op_sp.addDescriptor(descriptors.toArray(new DescriptorNode[0]));
        return op_sp;
    }

    private ClassStatementNode descriptor_var(ArrayList<DescriptorNode> descriptors) {
        assert tokens.tokenIs(TokenType.NAME);
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
        assert tokens.tokenIs("(");
        boolean has_posArgs = braceContains("/");
        mustToken("Argument lists must start with an open-paren", true, "(");
        ArrayList<TypedArgumentNode> posArgs = new ArrayList<>();
        ArrayList<TypedArgumentNode> args = new ArrayList<>();
        ArrayList<TypedArgumentNode> kwArgs = new ArrayList<>();
        if (has_posArgs) {
            while (!tokens.tokenIs("/")) {
                posArgs.add(typed_argument());
                if (tokens.tokenIs(TokenType.COMMA)) {
                    NextToken(true);
                }
            }
            NextToken();
            if (tokens.tokenIs(TokenType.COMMA)) {
                NextToken(true);
            } else if (!tokens.tokenIs(TokenType.CLOSE_BRACE)) {
                throw new ParserException("Unexpected " + tokens.getFirst());
            }
        }
        ArrayList<TypedArgumentNode> which_args = args;
        while (!tokens.tokenIs(")")) {
            if (tokens.tokenIs("*") && tokens.getToken(1).is(",", ")")) {
                which_args = kwArgs;
                NextToken(true);
                NextToken(true);
                continue;
            }
            which_args.add(typed_argument());
            passNewlines();
            if (tokens.tokenIs(TokenType.COMMA)) {
                NextToken(true);
                continue;
            }
            mustToken("Comma must separate arguments", true, false, false, ")");
        }
        NextToken();
        return new TypedArgumentListNode(posArgs.toArray(new TypedArgumentNode[0]), args.toArray(new TypedArgumentNode[0]),
                kwArgs.toArray(new TypedArgumentNode[0]));
    }

    private StatementBodyNode fn_body() {
        mustToken("The body of a function must be enclosed in curly brackets", true,
                true, true, "{");
        ArrayList<BaseNode> statements = new ArrayList<>();
        while (!tokens.tokenIs("}")) {
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
        while (!tokens.tokenIs("}")) {
            statements.add(class_statement());
            passNewlines();
        }
        NextToken();
        Newline();
        return new ClassBodyNode(statements.toArray(new ClassStatementNode[0]));
    }

    private TypeNode[] fn_retval() {
        if (tokens.tokenIs("{")) {
            return new TypeNode[0];
        }
        mustToken("Return value must use arrow operator", true, TokenType.ARROW);
        LinkedList<TypeNode> types = new LinkedList<>();
        while (!tokens.tokenIs("{") && !tokens.tokenIs(TokenType.NEWLINE)) {
            types.add(type());
            if (tokens.tokenIs(",")) {
                NextToken();
            }
        }
        return types.toArray(new TypeNode[0]);
    }

    private TestNode test(boolean ignore_newline) {
        TestNode if_true = test_no_ternary(ignore_newline);
        if (tokens.tokenIs("if")) {
            NextToken(ignore_newline);
            TestNode statement = test_no_ternary(ignore_newline);
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

    private TestNode test_no_ternary(boolean ignore_newline) {
        switch (tokens.getFirst().token) {
            case NAME:
                return test_left_variable(ignore_newline);
            case OPEN_BRACE:
                return openBrace();
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
            case STRING:
                return string();
            case KEYWORD:
                if (tokens.tokenIs("lambda")) {
                    return lambda();
                } else if (tokens.tokenIs("some")) {
                    return some_op();
                }
                // Lack of final return very much intentional here
            default:
                throw new ParserException(""); // TODO: Error messages
        }
    }

    private LambdaNode lambda() {
        assert tokens.tokenIs("lambda");
        NextToken();
        TypedArgumentListNode args = fn_args();
        StatementBodyNode body = fn_body();
        return new LambdaNode(args, body);
    }

    private TestNode test_left_variable(boolean ignore_newline) {
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
            LinkedList<TestNode> nodes = new LinkedList<>();
            while_loop:
            while (!tokens.tokenIs(TokenType.NEWLINE)) {
                switch (tokens.getFirst().token) {
                    case OPEN_BRACE:
                        TestNode last_node = nodes.peekLast();
                        if (last_node instanceof OperatorNode) {
                            nodes.add(openBrace());
                            break;
                        }
                        break while_loop;
                    case NAME:
                        nodes.add(left_name());
                        break;
                    case NUMBER:
                        nodes.add(number());
                        break;
                    case ARROW:
                        throw new ParserException("Unexpected " + tokens.getFirst());
                    case BOOL_OP:
                    case OPERATOR:
                        nodes.add(new OperatorNode(tokens.getFirst().sequence));
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
                        if (tokens.tokenIs("in", "casted")) {
                            nodes.add(new OperatorNode(tokens.getFirst().sequence));
                            NextToken();
                            break;
                        } else if (tokens.tokenIs("if", "else")) {
                            break while_loop;
                        }  // Lack of breaks here is intentional
                    case CLOSE_BRACE:
                        if (ignore_newline) {
                            break while_loop;
                        }  // Lack of breaks here intentional too
                    default:
                        throw new ParserException("Unexpected "+tokens.getFirst());
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

    private void parseExpression(LinkedList<TestNode> nodes, String... expr) {
        if (nodes.size() == 1) {
            return;
        }
        for (int nodeNumber = 0; nodeNumber < nodes.size(); nodeNumber++) {
            TestNode node = nodes.get(nodeNumber);
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

    private void parseOperator(LinkedList<TestNode> nodes, int nodeNumber) {
        TestNode node = nodes.get(nodeNumber);
        TestNode previous = nodes.get(nodeNumber - 1);
        TestNode next = nodes.get(nodeNumber + 1);
        TestNode op;
        if (node instanceof OperatorNode) {
            op = new OperatorNode(((OperatorNode) node).getOperator(), previous, next);
        } else {
            throw new ParserException("Unexpected node for parseOperator");
        }
        nodes.set(nodeNumber, op);
        nodes.remove(nodeNumber + 1);
        nodes.remove(nodeNumber - 1);
    }

    private void parseUnaryOp(LinkedList<TestNode> nodes, int nodeNumber) {
        TestNode node = nodes.get(nodeNumber);
        TestNode next = nodes.get(nodeNumber + 1);
        TestNode op;
        if (node instanceof OperatorTypeNode) {
            op = new OperatorNode(((OperatorTypeNode) node), next);
        }  else {
            throw new ParserException("Unexpected node for parseOperator");
        }
        nodes.set(nodeNumber, op);
        nodes.remove(nodeNumber + 1);
    }

    private TypeNode type() {
        return type(false, false);
    }

    private TypeNode type(boolean allow_empty, boolean is_vararg) {
        DottedVariableNode main;
        if (!tokens.tokenIs(TokenType.NAME)) {
            if (allow_empty && tokens.tokenIs("[")) {
                main = new DottedVariableNode();
            } else {
                throw new ParserException("Expected type name, got " + tokens.getFirst());
            }
        } else {
            main = dotted_name();
        }
        if (!tokens.tokenIs("[")) {
            return new TypeNode(main);
        }
        NextToken(true);
        LinkedList<TypeNode> subtypes = new LinkedList<>();
        while (!tokens.tokenIs("]")) {
            boolean subcls_vararg;
            if (tokens.tokenIs("*", "**")) {
                subcls_vararg = true;
                NextToken(true);
            } else {
                subcls_vararg = false;
            }
            subtypes.add(type(true, subcls_vararg));
            if (tokens.tokenIs(TokenType.COMMA)) {
                NextToken(true);
                continue;
            }
            passNewlines();
            if (!tokens.tokenIs("]")) {
                throw new ParserException("Comma must separate subtypes");
            }
        }
        NextToken();
        return new TypeNode(main, subtypes.toArray(new TypeNode[0]), is_vararg);
    }

    private TypedVariableNode[] for_vars() {
        LinkedList<TypedVariableNode> vars = new LinkedList<>();
        while (!tokens.tokenIs("in")) {
            if (!tokens.tokenIs(TokenType.NAME)) {
                throw new ParserException("Expected variable, got " + tokens.getFirst());
            }
            vars.add(typed_variable());
            if (tokens.tokenIs(TokenType.COMMA)) {
                NextToken();
            }
        }
        return vars.toArray(new TypedVariableNode[0]);
    }

    private TestNode[] for_iterables() {
        if (tokens.tokenIs(TokenType.NEWLINE)) {
            return new TestNode[0];
        }
        LinkedList<TestNode> tests = new LinkedList<>();
        while (!tokens.tokenIs("{")) {
            tests.add(test());
            if (tokens.tokenIs(TokenType.COMMA)) {
                NextToken();
                continue;
            }
            if (!tokens.tokenIs("{")) {
                throw new ParserException("Comma must separate values");
            }
        }
        return tests.toArray(new TestNode[0]);
    }

    private TestNode[] test_list(boolean ignore_newlines) {
        if (tokens.tokenIs("(") && !braceContains("in") && !braceContains("for")) {
            int brace_size = sizeOfBrace(0);
            if (!tokens.getToken(brace_size).is(TokenType.COMMA)) {
                NextToken();
                TestNode[] vars = test_list(true);
                if (!tokens.tokenIs(")")) {
                    throw new ParserException("Unmatched braces");
                }
                NextToken();
                return vars;
            }
        }
        if (!ignore_newlines && tokens.tokenIs(TokenType.NEWLINE)) {
            return new TestNode[0];
        }
        LinkedList<TestNode> tests = new LinkedList<>();
        while (!tokens.tokenIs(TokenType.NEWLINE)) {
            tests.add(test(ignore_newlines));
            if (tokens.tokenIs(TokenType.COMMA)) {
                NextToken(ignore_newlines);
                continue;
            }
            if (!ignore_newlines && !tokens.tokenIs(TokenType.NEWLINE)) {
                throw new ParserException("Comma must separate values");
            } else if (ignore_newlines) {
                break;
            }
        }
        if (!ignore_newlines && !tokens.tokenIs(TokenType.NEWLINE)) {
            throw new ParserException("Expected newline, got "+tokens.getFirst());
        }
        return tests.toArray(new TestNode[0]);
    }

    private TypedArgumentNode typed_argument() {
        boolean is_vararg = tokens.tokenIs("*", "**");
        String vararg_type;
        if (tokens.tokenIs("*", "**")) {
            vararg_type = tokens.getFirst().sequence;
            NextToken();
        } else {
            vararg_type = "";
        }
        TypeNode type = type();
        VariableNode var = name();
        TestNode default_value = null;
        if (tokens.tokenIs("=")) {
            NextToken();
            default_value = test(true);
        }
        return new TypedArgumentNode(type, var, default_value, is_vararg, vararg_type);
    }

    private ClassStatementNode class_statement() {
        if (tokens.tokenIs("static") && tokens.getToken(1).is("{")) {
            NextToken();
            return new StaticBlockNode(fn_body());
        }
        BaseNode stmt = statement();
        if (stmt instanceof ClassStatementNode) {
            return (ClassStatementNode) stmt;
        }
        throw new ParserException(tokens.getFirst()+" is not a valid class statement");
    }

    private DeclarationNode declaration() {
        TypeNode type = type();
        VariableNode var = name();
        return new DeclarationNode(type, var);
    }

    private PropertyDefinitionNode property_stmt() {
        VariableNode name = new VariableNode();
        if (!tokens.tokenIs("{")) {
            name = name();
        }
        NextToken(true);
        StatementBodyNode get = new StatementBodyNode();
        StatementBodyNode set = new StatementBodyNode();
        TypedArgumentListNode set_args = new TypedArgumentListNode();
        if (tokens.tokenIs("get")) {
            get = fn_body();
        }
        if (tokens.tokenIs("set")) {
            set_args = fn_args();
            set = fn_body();
        }
        passNewlines();
        if (!tokens.tokenIs("}")) {
            throw new ParserException("Only set and get are allowed in context statements");
        }
        NextToken();
        Newline();
        return new PropertyDefinitionNode(name, get, set_args, set);
    }

    private ComprehensionNode comprehension() {
        String brace_type;
        if (!tokens.tokenIs(TokenType.OPEN_BRACE)) {  // Comprehensions in function calls
            brace_type = "";
        } else {
            brace_type = tokens.getFirst().sequence;
            NextToken(true);
        }
        TestNode builder = test(true);
        if (!tokens.tokenIs("for")) {
            throw new ParserException("Invalid start to comprehension");
        }
        NextToken(true);
        TypedVariableNode[] variables = comp_params();
        if (!tokens.tokenIs("in")) {
            throw new ParserException("Comprehension body must have in after variable list");
        }
        NextToken(true);
        LinkedList<TestNode> looped = new LinkedList<>();
        while (true) {
            if (tokens.tokenIs(brace_type)) {
                break;
            }
            looped.add(test(true));
            if (tokens.tokenIs(",")) {
                NextToken(true);
            } else {
                break;
            }
        }
        if (!brace_type.isEmpty() && !tokens.tokenIs(matching_brace(brace_type))) {
            throw new ParserException("Expected close brace");
        }
        NextToken();
        TestNode[] looped_array = looped.toArray(new TestNode[0]);
        return new ComprehensionNode(brace_type, variables, builder, looped_array);
    }

    private DictComprehensionNode dict_comprehension() {
        assert tokens.tokenIs("{");
        NextToken(true);
        TestNode key = test(true);
        if (!tokens.tokenIs(":")) {
            throw new ParserException("Expected :, got "+tokens.getFirst());
        }
        NextToken(true);
        TestNode val = test(true);
        if (!tokens.tokenIs("for")) {
            throw new ParserException("Expected for, got "+tokens.getFirst());
        }
        NextToken();
        TypedVariableNode[] vars = comp_params();
        if (!tokens.tokenIs("in")) {
            throw new ParserException("Expected in, got "+tokens.getFirst());
        }
        NextToken();
        TestNode[] looped = test_list(true);
        if (!tokens.tokenIs("}")) {
            throw new ParserException("Expected }, got "+tokens.getFirst());
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
        assert tokens.tokenIs(TokenType.OPEN_BRACE);
        String brace_type = tokens.getFirst().sequence;
        NextToken(true);
        String balanced_brace = matching_brace(brace_type);
        LinkedList<TestNode> tokens = new LinkedList<>();
        LinkedList<Boolean> is_splat = new LinkedList<>();
        while (true) {
            if (this.tokens.tokenIs(balanced_brace)) {
                break;
            } else if (this.tokens.tokenIs(TokenType.CLOSE_BRACE)) {
                throw new ParserException("Unmatched braces");
            }
            if (this.tokens.tokenIs("*")) {
                is_splat.add(true);
                NextToken(true);
            } else {
                is_splat.add(false);
            }
            tokens.add(test(true));
            if (this.tokens.tokenIs(",")) {
                NextToken(true);
            } else {
                break;
            }
        }
        if (this.tokens.tokenIs(balanced_brace)) {
            NextToken();
        } else {
            throw new ParserException("Unmatched braces");
        }
        return new LiteralNode(brace_type, tokens.toArray(new TestNode[0]), is_splat.toArray(new Boolean[0]));
    }

    private DottedVariableNode[] var_list(boolean ignore_newlines) {
        LinkedList<DottedVariableNode> variables = new LinkedList<>();
        if (ignore_newlines) {
            passNewlines();
        }
        if (tokens.tokenIs("(") && !braceContains("in") && !braceContains("for")) {
            NextToken();
            DottedVariableNode[] vars = var_list(true);
            if (!tokens.tokenIs(")")) {
                throw new ParserException("Unmatched braces");
            }
            return vars;
        }
        while (true) {
            if (!tokens.tokenIs(TokenType.NAME)) {
                break;
            }
            if (tokens.tokenIs(TokenType.CLOSE_BRACE)) {
                throw new ParserException("Unmatched braces");
            }
            variables.add(left_name());
            if (tokens.tokenIs(",")) {
                NextToken(ignore_newlines);
            } else {
                break;
            }
        }
        return variables.toArray(new DottedVariableNode[0]);
    }

    private VariableNode[] name_list(boolean ignore_newlines) {
        LinkedList<VariableNode> variables = new LinkedList<>();
        if (ignore_newlines) {
            passNewlines();
        }
        if (tokens.tokenIs("(") && !braceContains("in") && !braceContains("for")) {
            NextToken();
            VariableNode[] vars = name_list(true);
            if (!tokens.tokenIs(")")) {
                throw new ParserException("Unmatched braces");
            }
            return vars;
        }
        while (true) {
            if (!tokens.tokenIs(TokenType.NAME)) {
                break;
            }
            if (tokens.tokenIs(TokenType.CLOSE_BRACE)) {
                throw new ParserException("Unmatched braces");
            }
            variables.add(name());
            if (tokens.tokenIs(",")) {
                NextToken(ignore_newlines);
            } else {
                break;
            }
        }
        return variables.toArray(new VariableNode[0]);
    }

    private DictLiteralNode dict_literal() {
        assert tokens.tokenIs("{");
        NextToken(true);
        LinkedList<TestNode> keys = new LinkedList<>();
        LinkedList<TestNode> values = new LinkedList<>();
        while (true) {
            keys.add(test());
            if (!tokens.tokenIs(":")) {
                throw new ParserException("Dict comprehension must have colon");
            }
            NextToken(true);
            values.add(test());
            if (!tokens.tokenIs(",")) {
                break;
            }
            NextToken(true);
            if (tokens.tokenIs("}")) {
                break;
            }
        }
        if (!tokens.tokenIs("}")) {
            throw new ParserException("Unmatched brace");
        }
        NextToken();
        return new DictLiteralNode(keys.toArray(new TestNode[0]), values.toArray(new TestNode[0]));
    }

    private ContextDefinitionNode context_def() {
        assert tokens.tokenIs("context");
        NextToken();
        VariableNode name = new VariableNode();
        if (tokens.tokenIs(TokenType.NAME)) {
            name = name();
        }
        if (!tokens.tokenIs("{")) {
            throw new ParserException("Context managers must be followed by a curly brace");
        }
        NextToken(true);
        StatementBodyNode enter = new StatementBodyNode();
        StatementBodyNode exit = new StatementBodyNode();
        if (tokens.tokenIs("enter")) {
            enter = fn_body();
        }
        if (tokens.tokenIs("exit")) {
            exit = fn_body();
        }
        passNewlines();
        if (!tokens.tokenIs("}")) {
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
        assert tokens.tokenIs("try");
        NextToken();
        StatementBodyNode body = fn_body();
        StatementBodyNode except = new StatementBodyNode();
        VariableNode[] excepted = new VariableNode[0];
        VariableNode as_var = new VariableNode();
        StatementBodyNode else_stmt = new StatementBodyNode();
        StatementBodyNode finally_stmt = new StatementBodyNode();
        if (tokens.tokenIs("except")) {
            NextToken();
            excepted = name_list( false);
            if (tokens.tokenIs("as")) {
                NextToken();
                as_var = name();
            }
            except = fn_body();
            if (tokens.tokenIs("else")) {
                NextToken();
                else_stmt = fn_body();
            }
        }
        if (tokens.tokenIs("finally")) {
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
        assert tokens.tokenIs("with");
        NextToken();
        LinkedList<TestNode> managed = new LinkedList<>();
        while (!tokens.tokenIs("as")) {
            managed.add(test());
            if (tokens.tokenIs(TokenType.COMMA)) {
                NextToken();
            } else if (!tokens.tokenIs("as")) {
                throw new ParserException("Expected comma or as, got "+tokens.getFirst());
            }
        }
        VariableNode[] vars = name_list( false);
        StatementBodyNode body = fn_body();
        Newline();
        return new WithStatementNode(managed.toArray(new TestNode[0]), vars, body);
    }

    private AssertStatementNode assert_stmt() {
        assert tokens.tokenIs("assert");
        NextToken();
        TestNode assertion = test();
        Newline();
        return new AssertStatementNode(assertion);
    }

    private DeleteStatementNode del_stmt() {
        assert tokens.tokenIs("del");
        NextToken();
        TestNode deletion = test();
        Newline();
        return new DeleteStatementNode(deletion);
    }

    private YieldStatementNode yield_stmt() {
        assert tokens.tokenIs("yield");
        NextToken();
        boolean is_from = tokens.tokenIs("from");
        if (is_from) {
            NextToken();
        }
        LinkedList<TestNode> yields = new LinkedList<>();
        while (!tokens.tokenIs(TokenType.NEWLINE)) {
            yields.add(test());
            if (tokens.tokenIs(TokenType.COMMA)) {
                NextToken();
                continue;
            }
            if (!tokens.tokenIs(TokenType.NEWLINE)) {
                throw new ParserException("Comma must separate yields");
            }
        }
        return new YieldStatementNode(is_from, yields.toArray(new TestNode[0]));
    }

    private RaiseStatementNode raise_stmt() {
        assert tokens.tokenIs("raise");
        NextToken();
        TestNode raised = test();
        Newline();
        return new RaiseStatementNode(raised);
    }

    private ImportExportNode left_from() {
        assert tokens.tokenIs("from");
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
        assert tokens.tokenIs("typedef");
        NextToken();
        TypeNode name = type();
        assert tokens.tokenIs("as");
        NextToken();
        TypeNode type = type();
        Newline();
        return new TypedefStatementNode(name, type);
    }

    private ArgumentNode[] fn_call_args() {
        if (!tokens.tokenIs("(")) {
            throw new ParserException("Function call must start with open-paren");
        }
        NextToken(true);
        if (tokens.tokenIs(")")) {
            NextToken();
            return new ArgumentNode[0];
        }
        LinkedList<ArgumentNode> args = new LinkedList<>();
        while (true) {
            VariableNode var = new VariableNode();
            int offset = tokens.tokenIs("*", "**") ? 1 : 0;
            if (tokens.getToken(offset).is(TokenType.NAME)
                    && tokens.getToken(sizeOfVariable(offset)).is("=")) {
                var = name();
                NextToken(true);
            }
            String vararg;
            if (tokens.tokenIs("*", "**")) {
                vararg = tokens.getFirst().sequence;
                NextToken(true);
            } else {
                vararg = "";
            }
            TestNode argument = test(true);
            args.add(new ArgumentNode(var, vararg, argument));
            if (tokens.tokenIs(")")) {
                break;
            }
            if (!tokens.tokenIs(",")) {
                throw new ParserException("Expected comma, got "+tokens.getFirst());
            }
            NextToken(true);
        }
        NextToken();
        return args.toArray(new ArgumentNode[0]);
    }

    private SomeStatementNode some_op() {
        assert tokens.tokenIs("some");
        NextToken();
        TestNode contained = test();
        if (!(contained instanceof OperatorNode)) {
            throw new ParserException("Expected an in, got "+tokens.getFirst());
        }
        OperatorNode in_stmt = (OperatorNode) contained;
        if (!in_stmt.getOperator().equals("in")) {
            throw new ParserException("Expected an in, got "+tokens.getFirst());
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
        Token amount = tokens.getToken(sizeOfVariable());
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
        if (!tokens.tokenIs("++")) {
            throw new RuntimeException("Expected ++, got "+tokens.getFirst());
        }
        NextToken();
        return new IncrementNode(var);
    }

    private DecrementNode decrement() {
        NameNode var = left_name();
        if (!tokens.tokenIs("--")) {
            throw new RuntimeException("Expected --, got "+tokens.getFirst());
        }
        NextToken();
        return new DecrementNode(var);
    }

    private TypedVariableNode[] comp_params() {
        LinkedList<TypedVariableNode> vars = new LinkedList<>();
        while (true) {
            vars.add(typed_variable());
            if (tokens.tokenIs("in")) {
                break;
            }
            if (!tokens.tokenIs(",")) {
                throw new ParserException("Unexpected "+tokens.getFirst());
            }
            NextToken();
        }
        return vars.toArray(new TypedVariableNode[0]);
    }

    private SliceNode slice() {
        assert tokens.tokenIs("[");
        NextToken(true);
        TestNode start;
        if (tokens.tokenIs(":")) {
            start = null;
        } else {
            start = test(true);
        }
        if (tokens.tokenIs("]")) {
            NextToken();
            return new SliceNode(start, null, null);
        }
        TestNode end = slice_test();
        if (tokens.tokenIs("]")) {
            NextToken();
            return new SliceNode(start, end, null);
        }
        TestNode step = slice_test();
        if (!tokens.tokenIs("]")) {
            throw new ParserException("Expected ], got "+tokens.getFirst());
        }
        NextToken();
        return new SliceNode(start, end, step);
    }

    private TestNode slice_test() {
        if (!tokens.tokenIs(":")) {
            throw new ParserException("Expected :, got "+tokens.getFirst());
        }
        NextToken(true);
        if (tokens.tokenIs(":", "]")) {
            return null;
        } else {
            return test(true);
        }
    }

    private InterfaceDefinitionNode interface_def() {
        assert tokens.tokenIs("interface");
        NextToken();
        TypeNode name = type();
        LinkedList<TypeNode> superclasses = new LinkedList<>();
        while (tokens.tokenIs("from")) {
            NextToken();
            superclasses.add(type());
        }
        return new InterfaceDefinitionNode(name, superclasses.toArray(new TypeNode[0]), interface_body());
    }

    private InterfaceBodyNode interface_body() {
        if (!tokens.tokenIs("{")) {
            throw new ParserException("The body of a class must be enclosed in curly brackets");
        }
        NextToken(false);
        ArrayList<InterfaceStatementNode> statements = new ArrayList<>();
        while (!tokens.tokenIs("}")) {
            statements.add(interface_stmt());
            passNewlines();
        }
        NextToken();
        Newline();
        return new InterfaceBodyNode(statements.toArray(new InterfaceStatementNode[0]));
    }

    private InterfaceStatementNode interface_stmt() {  // TODO: Clean up method
        if (tokens.tokenIs(TokenType.OPERATOR_SP, TokenType.DESCRIPTOR) || tokens.tokenIs("method")
              || (tokens.tokenIs("class") && tokens.getToken(1).is(TokenType.DESCRIPTOR, TokenType.KEYWORD))) {
            LinkedList<DescriptorNode> descriptors = new LinkedList<>();
            if (tokens.tokenIs("class")) {
                descriptors.add(new DescriptorNode("class"));
                NextToken();
            }
            while (tokens.tokenIs(TokenType.DESCRIPTOR)) {
                descriptors.add(new DescriptorNode(tokens.getFirst().sequence));
                NextToken();
            }
            int line_size = 0;
            for (Token token : tokens) {
                if (token.is(TokenType.NEWLINE)) {
                    break;
                }
                line_size++;
            }
            if (!tokens.getToken(line_size - 1).is("{")) {
                boolean is_operator;
                VariableNode fn_name;
                String op_name;
                if (tokens.tokenIs(TokenType.OPERATOR_SP)) {
                    is_operator = true;
                    op_name = tokens.getFirst().sequence;
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
                if (tokens.tokenIs(TokenType.ARROW)) {
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

    private DottedVariableNode dotted_var(TestNode preDot) {
        assert tokens.tokenIs(TokenType.DOT);
        NextToken();
        LinkedList<NameNode> postDot = new LinkedList<>();
        while (tokens.tokenIs(TokenType.NAME, TokenType.OPERATOR_SP)) {
            if (tokens.tokenIs(TokenType.OPERATOR_SP)) {
                String op_type = tokens.getFirst().sequence.replaceFirst("operator *",  "");
                NextToken();
                postDot.add(new SpecialOpNameNode(new OperatorTypeNode(op_type)));
                break;
            }
            postDot.add(var_braced());
            if (!tokens.tokenIs(TokenType.DOT)) {
                break;
            }
            NextToken();
        }
        return new DottedVariableNode(preDot, postDot.toArray(new NameNode[0]));
    }

    private DottedVariableNode left_name() {
        assert tokens.tokenIs(TokenType.NAME);
        NameNode name = var_braced();
        if (tokens.tokenIs(TokenType.DOT)) {
            return dotted_var(name);
        } else {
            return new DottedVariableNode(name);
        }
    }

    private NameNode var_braced() {
        assert tokens.tokenIs(TokenType.NAME);
        NameNode name = name();
        while_brace:
        while (tokens.tokenIs(TokenType.OPEN_BRACE)) {
            switch (tokens.getFirst().sequence) {
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
                    break while_brace;
                default:
                    throw new RuntimeException("Unexpected brace");
            }
        }
        return name;
    }

    private DottedVariableNode dotted_name() {
        LinkedList<VariableNode> names = new LinkedList<>();
        while (tokens.tokenIs(TokenType.NAME)) {
            names.add(name());
            if (!tokens.tokenIs(TokenType.DOT)) {
                break;
            }
            NextToken();
        }
        return new DottedVariableNode(names.removeFirst(), names.toArray(new VariableNode[0]));
    }
}
