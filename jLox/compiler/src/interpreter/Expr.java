package interpreter;

import java.util.List;

abstract class Expr {
    abstract <R> R accept(Visitor<R> visitor);

    public interface Visitor<R> {
        R visitThisExpr(This Expr);
        R visitSuperExpr(Super Expr);
        R visitUnaryExpr(Unary Expr);
        R visitBinaryExpr(Binary Expr);
        R visitGroupingExpr(Grouping Expr);
        R visitLiteralExpr(Literal Expr);
        R visitLogicalExpr(Logical Expr);
        R visitVariableExpr(Variable Expr);
        R visitAssignExpr(Assign Expr);
        R visitCallExpr(Call Expr);
        R visitGetExpr(Get Expr);
        R visitSetExpr(Set Expr);
    }

    static class This extends Expr {
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitThisExpr(this);
        }

        This(Token keyword) {
            this.keyword = keyword;
        }

        final Token keyword;
    }

    static class Super extends Expr {
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitSuperExpr(this);
        }

        Super(Token keyword, Token method) {
            this.keyword = keyword;
            this.method = method;
        }

        final Token keyword;
        final Token method;
    }

    static class Unary extends Expr {
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitUnaryExpr(this);
        }

        Unary(Token operator, Expr right) {
            this.operator = operator;
            this.right = right;
        }

        final Token operator;
        final Expr right;
    }

    static class Binary extends Expr {
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitBinaryExpr(this);
        }

        Binary(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        final Expr left;
        final Token operator;
        final Expr right;
    }

    static class Grouping extends Expr {
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitGroupingExpr(this);
        }

        Grouping(Expr expression) {
            this.expression = expression;
        }

        final Expr expression;
    }

    static class Literal extends Expr {
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitLiteralExpr(this);
        }

        Literal(Object value) {
            this.value = value;
        }

        final Object value;
    }

    static class Logical extends Expr {
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitLogicalExpr(this);
        }

        Logical(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        final Expr left;
        final Token operator;
        final Expr right;
    }

    static class Variable extends Expr {
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitVariableExpr(this);
        }

        Variable(Token name) {
            this.name = name;
        }

        final Token name;
    }

    static class Assign extends Expr {
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitAssignExpr(this);
        }

        Assign(Token name, Expr value) {
            this.name = name;
            this.value = value;
        }

        final Token name;
        final Expr value;
    }

    static class Call extends Expr {
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitCallExpr(this);
        }

        Call(Expr callee, Token paren, List<Expr> arguments) {
            this.callee = callee;
            this.paren = paren;
            this.arguments = arguments;
        }

        final Expr callee;
        final Token paren;
        final List<Expr> arguments;
    }

    static class Get extends Expr {
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitGetExpr(this);
        }

        Get(Expr object, Token name) {
            this.object = object;
            this.name = name;
        }

        final Expr object;
        final Token name;
    }

    static class Set extends Expr {
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitSetExpr(this);
        }

        Set(Expr object, Token name, Expr value) {
            this.object = object;
            this.name = name;
            this.value = value;
        }

        final Expr object;
        final Token name;
        final Expr value;
    }

}
