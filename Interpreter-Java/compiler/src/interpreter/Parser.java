package interpreter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Parser {
    /**
     * 语法错误类
     * 语法分析采用递归调用，在分析过程中，每一次调用函数都会在调用栈上保存一层栈帧(call frame)
     * 异常可以清空调用栈。因此通过抛出异常，我们可以清空调用栈，重置分析状态
     */
    private static class ParseError extends RuntimeException {

    }

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * 对外接口
     * program        → statement^* EOF;
     */
    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    }

    /**
     * declaration    → funDecl | varDecl | statement | classDecl;
     */
    private Stmt declaration() {
        try {
            if (match(TokenType.CLASS)) {
                return classDeclaration();
            }
            if (match(TokenType.FUN)) {
                return funDeclaration();
            }
            if (match(TokenType.VAR)) {
                return varDeclaration();
            }
            return statement();
        } catch (ParseError Ps) {
            synchronize();
            return null;
        }
    }

    /**
     * classDecl      → "class" IDENTIFIER ( "<" IDENTIFIER )? "{" function* "}";
     */
    private Stmt classDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expect class name.");
        Expr.Variable superclass = null;
        if (match(TokenType.LESS)) {
            consume(TokenType.IDENTIFIER, "Expect supperclass name.");
            superclass = new Expr.Variable(previous());
        }

        consume(TokenType.LEFT_BRACE, "Expect '{' before class body.");

        List<Stmt.Function> methods = new ArrayList<>();
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            methods.add(function("method"));
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after class body.");

        return new Stmt.Class(name, superclass, methods);
    }

    /**
     * funDecl        → "fun" function ;
     */
    private Stmt funDeclaration() {
        return function("function");
    }

    /**
     * function       → IDENTIFIER "(" parameters? ")" block ;
     * parameters     → IDENTIFIER ( "," IDENTIFIER )* ;
     */
    private Stmt.Function function(String kind) {
        Token name = consume(TokenType.IDENTIFIER, "Expect " + kind + " name.");
        consume(TokenType.LEFT_PAREN, "Expect '(' after " + kind + " name.");
        List<Token> parameters = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }

                parameters.add(
                        consume(TokenType.IDENTIFIER, "Expect parameter name."));
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.");

        consume(TokenType.LEFT_BRACE, "Expect '{' before " + kind + " body.");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
    }

    /**
     * varDecl       → "var" IDENTIFIER ( "=" expression )? ";" ;
     */
    private Stmt varDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (match(TokenType.EQUAL)) {
            initializer = expression();
        }

        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    /**
     * statement      → exprStmt | printStmt | returnStmt | block |
     * ifStmt | whileStmt | forStmt ;
     */
    private Stmt statement() {
        if (match(TokenType.PRINT)) {
            return printStatement();
        }
        if (match(TokenType.RETURN)) {
            return returnStatement();
        }
        if (match(TokenType.LEFT_BRACE)) {
            return new Stmt.Block(block());
        }
        if (match(TokenType.IF)) {
            return ifStatement();
        }
        if (match(TokenType.WHILE)) {
            return whileStatement();
        }
        if (match(TokenType.FOR)) {
            return forStatement();
        }
        return expressionStatement();
    }

    /**
     * exprStmt       → expression ";"
     */
    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    /**
     * printStmt      → "print" expression ";"
     */
    private Stmt printStatement() {
        Expr value = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    /**
     * returnStmt     → "return" expression? ";" ;
     */
    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(TokenType.SEMICOLON)) {
            value = expression();
        }

        consume(TokenType.SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, value);
    }

    /**
     * block          → "{" declaration^* "}"
     */
    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    /**
     * ifStmt         → "if" "(" expression ")" statement
     * ( "else" statement )? ;
     */
    private Stmt ifStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.");
        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(TokenType.ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    /**
     * whileStmt      → "while" "(" expression ")" statement ;
     */
    private Stmt whileStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.");
        Stmt body = statement();
        return new Stmt.While(condition, body);
    }

    /**
     * for(initializer;condition;increment){}
     * forStmt        → "for" "(" ( varDecl | exprStmt | ";" )
     * expression? ";"
     * expression? ")" statement ;
     */
    private Stmt forStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.");
        Stmt initializer;
        if (match(TokenType.SEMICOLON)) {
            initializer = null;
        } else if (match(TokenType.VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(TokenType.SEMICOLON)) {
            condition = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after loop condition.");

        Expr increment = null;
        if (!check(TokenType.RIGHT_PAREN)) {
            increment = expression();
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.");

        Stmt body = statement();

        //将 increment 添加到block最后
        if (increment != null) {
            body = new Stmt.Block(
                    Arrays.asList(
                            body,
                            new Stmt.Expression(increment)));
        }
        //将 condition 添加到whileStatement
        if (condition == null) {
            condition = new Expr.Literal(true);
        }
        body = new Stmt.While(condition, body);

        //将 initializer 添加到block最前
        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    /**
     * expression     → assignment ;
     */
    private Expr expression() {
        return assignment();
    }

    /**
     * assignment     → ( call "." )? IDENTIFIER "=" assignment
     * | logic_or ;
     */
    private Expr assignment() {
        Expr expr = or();

        if (match(TokenType.EQUAL)) {
            Token equals = previous();
            Expr value = assignment();
            if (expr instanceof Expr.Variable) {
                Token name = ((((Expr.Variable) expr).name));
                return new Expr.Assign(name, value);
            } else if (expr instanceof Expr.Get) {
                Expr.Get get = (Expr.Get) expr;
                return new Expr.Set(get.object, get.name, value);
            }
            // 不 throw，继续分析，但报告错误
            error(equals, "Invalid assignment target.");
        }
        return expr;
    }

    /**
     * logic_or       → logic_and ( "or" logic_and )* ;
     */
    private Expr or() {
        Expr expr = and();

        while (match(TokenType.OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    /**
     * logic_and      → equality ( "and" equality )* ;
     */
    private Expr and() {
        Expr expr = equality();

        while (match(TokenType.AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    /**
     * equality       → (Comparison [==,!=] Equality) | Comparison;
     */
    private Expr equality() {
        Expr expr = comparison();
        if (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }//最先进行优先级最高的等号的判定

    /**
     * Comparison -> (Move [>,>=,<,<=] Comparison) | Move
     */
    private Expr comparison() {
        Expr expr = move();
        if (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /**
     * Move -> (Term [<<,>>] Move) | Term
     */
    private Expr move() {
        Expr expr = term();
        if (match(TokenType.SHIFT_LEFT, TokenType.SHIFT_RIGHT)) {
            Token operator = previous();
            Expr right = move();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /**
     * Term -> (Factor [-,+] Term) | Factor
     */
    private Expr term() {
        Expr expr = factor();
        if (match(TokenType.MINUS, TokenType.PLUS)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /**
     * Factor -> (Unary [/,*,%] Factor) | Unary
     */
    private Expr factor() {
        Expr expr = unary();
        if (match(TokenType.DIVIDE, TokenType.MULTIPLY, TokenType.MOD)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /**
     * Unary          → ([!,-,+,++,--] Unary) | call
     */
    private Expr unary() {
        if (match(TokenType.BANG, TokenType.MINUS, TokenType.PLUS,
                TokenType.PLUS_PLUS, TokenType.MINUS_MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return call();
    }

    /**
     * call           → primary ( "(" arguments? ")" | "." IDENTIFIER )*;
     * arguments      → expression ( "," expression )* ;
     */
    private Expr call() {
        Expr expr = primary();
        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(TokenType.DOT)) {
                Token name = consume(TokenType.IDENTIFIER,
                        "Expect property name after '.'.");
                expr = new Expr.Get(expr, name);
            } else {
                break;
            }
        }
        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            } while (match(TokenType.COMMA));
        }
        Token paren = consume(TokenType.RIGHT_PAREN,
                "Expect ')' after arguments.");
        return new Expr.Call(callee, paren, arguments);
    }

    /**
     * primary → NUMBER | STRING | "true" | "false" | "NULL"
     * | "(" expression ")"
     * | "super" "." IDENTIFIER ;
     */
    private Expr primary() {
        try {
            if (match(TokenType.FALSE)) {
                return new Expr.Literal(false);
            }
            if (match(TokenType.TRUE)) {
                return new Expr.Literal(true);
            }
            if (match(TokenType.NULL)) {
                return new Expr.Literal(null);
            }

            if (match(TokenType.NUMBER, TokenType.STRING, TokenType.CHAR)) {
                return new Expr.Literal(previous().literal);
            }

            if (match(TokenType.SUPER)) {
                Token keyword = previous();
                consume(TokenType.DOT, "Expect '.' after 'super'.");
                Token method = consume(TokenType.IDENTIFIER,
                        "Expect superclass method name.");
                return new Expr.Super(keyword, method);
            }

            if (match(TokenType.THIS)) {
                return new Expr.This(previous());
            }

            if (match(TokenType.IDENTIFIER)) {
                return new Expr.Variable(previous());
            }

            if (match(TokenType.LEFT_PAREN)) {
                Expr expr = expression();
                consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
                return new Expr.Grouping(expr);
            }

            throw error(peek(), "Expect expression.");
        } catch (ParseError Pe) {
            return null;
        }
    }

    /**
     * 返回当前token的type是否匹配types中的某一个,若匹配,隐式 advance()
     *
     * @param types
     * @return
     */
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    /**
     * 检查下一个令牌是否符合预期,符合就消耗,不符合就报告错误
     * 与match唯一不同是报告错误
     *
     * @param type
     * @param message
     * @return
     */
    private Token consume(TokenType type, String message) {
        try {
            if (check(type)) {
                return advance();
            }
            throw error(peek(), message);
        } catch (ParseError Pe) {
            return null;
        }
    }

    /**
     * match的辅助函数,判断当前token的type是否和某一type相同
     *
     * @param type
     * @return
     */
    private boolean check(TokenType type) {
        if (isAtEnd()) {
            return false;
        }
        return peek().type == type;
    }

    /**
     * 将检测令牌下标向前推进一个,返回上一个令牌
     *
     * @return
     */
    private Token advance() {
        if (!isAtEnd()) {
            ++current;
        }
        return previous();
    }

    /**
     * 是否用完所有令牌
     *
     * @return
     */
    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    /**
     * 返回当下还未检测的令牌
     *
     * @return
     */
    private Token peek() {
        return tokens.get(current);
    }

    /**
     * 返回上一个刚检测过的令牌
     *
     * @return
     */
    private Token previous() {
        return tokens.get(current - 1);
    }

    /**
     * 返回语法错误 而不是抛出,
     * 这样可以让程序决定是否进入panic mode(进行同步)
     * 或直接报告错误之后继续parser(不进行同步)
     * 比如:
     * 如果函数参数过多,应该不进入紧急模式,
     * 而继续分析之后的参数
     *
     * @param token
     * @param message
     * @return
     */
    private ParseError error(Token token, String message) {
        Main.error(token, message);
        return new ParseError();
    }

    /**
     * 忽略词法单元，减少之前错误的副作用
     * 直到遇到错误语句(statement)的结尾或下一个语句的开头，即同步词法单元。然后继续语法分析
     * 例如 ; } { while if for do
     */
    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            // 如果上一个是分号,同步完成,开始分析
            if (previous().type == TokenType.SEMICOLON) {
                return;
            }
            // 如果当下令牌type是一般而言语句开始的types,同步完成,直接return
//            switch (peek().type){
//                case TokenType.STRUCT:
//                case TokenType.FOR:
//                case TokenType.IF:
//                case TokenType.RETURN:
//                    return;
//            }
            advance();
        }
    }
}


