package language;

import scanner.Token;

public abstract class Statement {
    public static class ExpressionStatement extends Statement {
        public final Expression expression;

        public ExpressionStatement(Expression expression) {
            this.expression = expression;
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visitExpressionStatement(this);
        }
    }

    public static class PrintStatement extends Statement {
        public final Expression expression;

        public PrintStatement(Expression expression) {
            this.expression = expression;
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visitPrintStatement(this);
        }
    }

    public static class VariableDeclaration extends Statement {
        public final Token identifier;
        public final Expression expression;

        public VariableDeclaration(Token identifier, Expression expression) {
            this.identifier = identifier;
            this.expression = expression;
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visitVariableDeclarationStatement(this);
        }
    }

    public interface Visitor<T> {
        T visitExpressionStatement(ExpressionStatement expressionStatement);
        T visitPrintStatement(PrintStatement printStatement);
        T visitVariableDeclarationStatement(VariableDeclaration variableAssignmentOrDeclarationStatement);
    }

    public abstract<T> T accept(Visitor<T> visitor);
}
