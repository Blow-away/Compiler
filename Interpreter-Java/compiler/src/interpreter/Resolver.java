package interpreter;

import com.sun.corba.se.spi.ior.IdentifiableContainerBase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * 语义分析，绑定变量
 */
public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    private enum FunctionType {
        NONE,
        FUNCTION,
        INITIALIZER,
        METHOD
    }

    private enum ClassType {
        NONE,
        CLASS,
        SUBCLASS
    }

    private ClassType currentClass = ClassType.NONE;

    private final Interpreter interpreter;
    // 块的栈，等同于 interpreter中的 enviroment
    // <变量名,是否准备好（初始化）>
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();
    // 当前是否在函数中，防止非函数中的 return语句
    private FunctionType currentFunction = FunctionType.NONE;

    public Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    @Override
    public Void visitThisExpr(Expr.This expr) {
        if (currentClass == ClassType.NONE) {
            Main.error(expr.keyword,
                    "Can't use 'this' outside of a class.");
            return null;
        }
        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitSuperExpr(Expr.Super expr) {
        if (currentClass == ClassType.NONE) {
            Main.error(expr.keyword,
                    "Can't use 'super' outside of a class.");
        } else if (currentClass != ClassType.SUBCLASS) {
            Main.error(expr.keyword,
                    "Can't use 'super' in a class with no superclass.");
        }
        resolveLocal(expr,expr.keyword);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!scopes.isEmpty() &&
                scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
            Main.error(expr.name,
                    "Can't read local variable without initializer.");
        }
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);

        for (Expr argument : expr.arguments) {
            resolve(argument);
        }
        return null;
    }

    @Override
    public Void visitGetExpr(Expr.Get expr) {
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitSetExpr(Expr.Set expr) {
        resolve(expr.object);
        resolve(expr.value);
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Main.error(stmt.keyword,
                    "Can't return from top-level code.");
        }
        if (currentFunction == FunctionType.INITIALIZER) {
            Main.error(stmt.keyword,
                    "Can't return a value from an initializer.");
        }
        if (stmt.value != null) {
            resolve(stmt.value);
        }
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        ClassType enclosingClass = currentClass;
        currentClass = ClassType.CLASS;
        declare(stmt.name);
        define(stmt.name);

        if (stmt.superclass != null &&
                stmt.name.lexeme.equals(stmt.superclass.name.lexeme)) {
            Main.error(stmt.superclass.name,
                    "A class can't inherit from itself.");
        }

        if (stmt.superclass != null) {
            currentClass = ClassType.SUBCLASS;
            resolve(stmt.superclass);
        }

        if(stmt.superclass != null){
            beginScope();
            scopes.peek().put("super",true);
        }

        beginScope();
        scopes.peek().put("this", true);

        for (Stmt.Function method : stmt.methods) {
            FunctionType declaration = FunctionType.METHOD;
            if (method.name.lexeme.equals("init")) {
                declaration = FunctionType.INITIALIZER;
            }
            resolveFunction(method, declaration);
        }
        endScope();

        if(stmt.superclass != null){
            endScope();
        }
        currentClass = enclosingClass;
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) {
            resolve(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);

        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }


    void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    }

    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }

    /**
     * 当分析到局部变量的引用时，将其depth绑定
     *
     * @param expr
     * @param name
     */
    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; --i) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }

    private void resolveFunction(Stmt.Function function, FunctionType type) {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;
        beginScope();
        for (Token param : function.params) {
            declare(param);
            define(param);
        }
        resolve(function.body);
        endScope();
        currentFunction = enclosingFunction;
    }

    private void beginScope() {
        scopes.push(new HashMap<String, Boolean>());
    }

    private void endScope() {
        scopes.pop();
    }

    /**
     * 声明变量
     *
     * @param name Token
     */
    private void declare(Token name) {
        if (scopes.isEmpty()) {
            return;
        }
        Map<String, Boolean> scope = scopes.peek();
        if (scope.containsKey(name.lexeme)) {
            Main.error(name, "Already variable with this name in this scope.");
        }
        scope.put(name.lexeme, false);
    }

    /**
     * 定义变量
     *
     * @param name Token
     */
    private void define(Token name) {
        if (scopes.isEmpty()) {
            return;
        }
        scopes.peek().put(name.lexeme, true);
    }
}
