package parser;

import language.Expression;
import language.Statement;
import scanner.Token;
import scanner.TokenType;

import java.util.ArrayList;
import java.util.List;

import util.Message;

public class Parser {
    private final List<Token> tokens;
    private int current = 0;
    private boolean hadError = false;

    private static class ParserError extends RuntimeException {}

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<Statement> parse() {
        List<Statement> statements = new ArrayList<>();
        while (!isAtEnd()) {
            try {
                statements.add(declaration());
            } catch (ParserError e) {
                synchronize();
                hadError = true;
            }
        }

        return statements;
    }

    public boolean isHadError() {
        return this.hadError;
    }

    private Statement declaration() throws ParserError {
        if (match(TokenType.LET)) {
            return variableDeclaration();
        }

        return statement();
    }

    private Statement variableDeclaration() throws ParserError {
        Boolean mutable = match(TokenType.MUT);
        Token variableIdentifier = consume(TokenType.IDENTIFIER, "Expected variable name.");
        if (match(TokenType.EQUAL)) {
            Expression expression = expression();
            consume(TokenType.SEMICOLON, "Expected `;` after variable declaration.");
            return new Statement.VariableDeclaration(variableIdentifier, expression, mutable);
        } else {
            Message.error(variableIdentifier.line, "Expected '=' after variable name.");
            throw new ParserError();
        }
    }

    private Statement statement() throws ParserError {
        if (match(TokenType.PRINT)) return printStatement();
        if (match(TokenType.LEFT_CURLY)) return block();
        return expressionStatement();
    }

    private Statement block() throws ParserError {
        List<Statement> statements = new ArrayList<>();
        while (!check(TokenType.RIGHT_CURLY) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(TokenType.RIGHT_CURLY, "Expected '}' after block.");
        return new Statement.Block(statements);
    }

    private Statement printStatement() throws ParserError {
        Expression value = expression();
        consume(TokenType.SEMICOLON, "Expected `;` after print statement.");
        return new Statement.PrintStatement(value);
    }

    private Statement expressionStatement() throws ParserError {
        Expression value = expression();
        consume(TokenType.SEMICOLON, "Expected `;` after expression.");
        return new Statement.ExpressionStatement(value);
    }

    private Expression expression() throws ParserError {
        return assignment();
    }

    private Expression assignment() throws ParserError {
        Expression left = term();

        if (match(TokenType.EQUAL)) {
            Token equals = previous();
            Expression right = assignment();

            if (left instanceof Expression.Variable) {
                Token identifier = ((Expression.Variable) left).identifier;
                return new Expression.Assignment(identifier, right);
            }

            Message.error(equals.line, "Invalid assignment target.");
            throw new ParserError();
        }

        return left;
    }

    private Expression term() throws ParserError {
        Expression expression = factor();

        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token operator = previous();
            Expression right = factor();
            expression = new Expression.Binary(expression, operator, right);
        }

        return expression;
    }

    private Expression factor() throws ParserError {
        Expression expression = pow();

        while (match(TokenType.STAR, TokenType.SLASH)) {
            Token operator = previous();
            Expression right = pow();
            expression = new Expression.Binary(expression, operator, right);
        }

        return expression;
    }

    private Expression pow() throws ParserError {
        Expression expression = unary();

        if (match(TokenType.DOUBLE_STAR)) {
            Token operator = previous();
            Expression right = pow();
            expression = new Expression.Binary(expression, operator, right);
        }

        return expression;
    }

    private Expression unary() throws ParserError {
        if (match(TokenType.MINUS)) {
            Token operator = previous();
            Expression right = unary();
            return new Expression.Unary(operator, right);
        }

        return primary();
    }

    private Expression primary() throws ParserError {
        if (match(TokenType.NUMBER)) {
            return new Expression.Literal(previous().literal);
        }

        if (match(TokenType.LEFT_PAREN)) {
            Expression expression = expression();
            consume(TokenType.RIGHT_PAREN, "Expected ')' after expression.");
            return new Expression.Grouping(expression);
        }

        if (match(TokenType.IDENTIFIER)) {
            return new Expression.Variable(previous());
        }

        Message.error(peek().line, "Expected expression.");
        throw new ParserError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) return;

            switch (peek().type) {
                case PRINT -> {return;}
                default -> {}
            }
            advance();
        }
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token consume(TokenType type, String message) throws ParserError {
        if (check(type)) return advance();
        Message.error(peek().line, message);
        throw new ParserError();
    }
}
