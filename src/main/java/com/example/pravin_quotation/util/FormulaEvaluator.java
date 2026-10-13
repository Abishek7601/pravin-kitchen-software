package com.example.pravin_quotation.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;

/**
 * Safe, deterministic mathematical expression parser and evaluator for dimension formulas.
 * Supports:
 * - Variables: L (Length), W (Width), H (Height) entered in inches.
 * - Operators: +, -, *, /, ×, x
 * - Parentheses: (, )
 * - Numbers: Integers and decimals
 *
 * Never uses JavaScript engine or arbitrary code execution.
 */
public final class FormulaEvaluator {

    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);
    public static final BigDecimal SQFT_DIVISOR = new BigDecimal("144");
    public static final BigDecimal LINEAR_FOOT_DIVISOR = new BigDecimal("12");

    public record EvaluationResult(
            BigDecimal rawValue,
            BigDecimal finalMeasurement,
            String measurementUnit,
            String pricingUnit,
            String unitType,
            String formulaUsed
    ) {}

    public record ValidationResult(
            boolean valid,
            String errorMessage,
            Set<String> variablesUsed,
            String inferredUnitType,
            BigDecimal sampleResult
    ) {}

    private FormulaEvaluator() {
    }

    /**
     * Validates an expression for syntax and safety.
     */
    public static ValidationResult validateExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return new ValidationResult(false, "Formula expression cannot be empty.", Collections.emptySet(), "AREA", null);
        }

        String cleaned = cleanExpression(expression);

        // Check for disallowed characters
        for (char c : cleaned.toCharArray()) {
            if (!Character.isDigit(c) && c != '.' && c != '+' && c != '-' && c != '*' && c != '/'
                    && c != '(' && c != ')' && c != ' ' && c != 'L' && c != 'W' && c != 'H') {
                return new ValidationResult(false, "Invalid character in formula: '" + c + "'. Only L, W, H, numbers and +, -, *, /, () are allowed.", Collections.emptySet(), "AREA", null);
            }
        }

        Set<String> vars = detectVariables(cleaned);

        try {
            // Test evaluation with sample dimensions L=24, W=12, H=10
            BigDecimal testL = new BigDecimal("24");
            BigDecimal testW = new BigDecimal("12");
            BigDecimal testH = new BigDecimal("10");

            BigDecimal sampleVal = evaluateRaw(cleaned, testL, testW, testH);

            if (sampleVal == null || sampleVal.compareTo(BigDecimal.ZERO) < 0) {
                return new ValidationResult(false, "Formula produced a negative value with sample dimensions.", vars, "AREA", null);
            }

            // Infer degree / unit type: test scaling by factor 2
            BigDecimal scaledVal = evaluateRaw(cleaned, testL.multiply(new BigDecimal("2")), testW.multiply(new BigDecimal("2")), testH.multiply(new BigDecimal("2")));
            String inferredType = "AREA";
            if (sampleVal.compareTo(BigDecimal.ZERO) > 0 && scaledVal != null) {
                double ratio = scaledVal.divide(sampleVal, 2, RoundingMode.HALF_UP).doubleValue();
                if (Math.abs(ratio - 2.0) < 0.5) {
                    inferredType = "LINEAR";
                }
            }

            return new ValidationResult(true, null, vars, inferredType, sampleVal);

        } catch (Exception e) {
            return new ValidationResult(false, "Formula syntax error: " + e.getMessage(), vars, "AREA", null);
        }
    }

    /**
     * Evaluates a formula and returns the measurement according to calculationType ("SQFT" or "INCHES").
     */
    public static EvaluationResult evaluate(
            String expression,
            String calculationType,
            String unitType,
            BigDecimal length,
            BigDecimal width,
            BigDecimal height
    ) {
        if (expression == null || expression.isBlank()) {
            expression = "(L + W) * H";
        }

        String cleaned = cleanExpression(expression);
        BigDecimal l = length != null ? length : BigDecimal.ZERO;
        BigDecimal w = width != null ? width : BigDecimal.ZERO;
        BigDecimal h = height != null ? height : BigDecimal.ZERO;

        BigDecimal rawVal = evaluateRaw(cleaned, l, w, h);
        if (rawVal == null) {
            rawVal = BigDecimal.ZERO;
        }

        boolean isSqft = calculationType == null || "SQFT".equalsIgnoreCase(calculationType);
        boolean isExplicitlyDividedBy144 = cleaned.contains("/144") || cleaned.contains("/ 144");

        // Determine unit type if not provided
        if (unitType == null || unitType.isBlank()) {
            unitType = inferUnitType(cleaned);
        }

        BigDecimal finalMeasurement;
        String measurementUnit;
        String pricingUnit;

        if (isSqft) {
            measurementUnit = "SQFT";
            pricingUnit = "₹ / SQFT";

            if (isExplicitlyDividedBy144) {
                finalMeasurement = rawVal;
            } else if ("LINEAR".equalsIgnoreCase(unitType)) {
                // Linear inches converted to linear feet (or sqft equivalent)
                finalMeasurement = rawVal.divide(LINEAR_FOOT_DIVISOR, 6, RoundingMode.HALF_UP);
            } else {
                // Area in square inches converted to square feet
                finalMeasurement = rawVal.divide(SQFT_DIVISOR, 6, RoundingMode.HALF_UP);
            }
        } else {
            // INCHES
            if ("LINEAR".equalsIgnoreCase(unitType)) {
                measurementUnit = "INCH";
                pricingUnit = "₹ / INCH";
                finalMeasurement = rawVal;
            } else {
                measurementUnit = "SQIN";
                pricingUnit = "₹ / SQIN";
                finalMeasurement = rawVal;
            }
        }

        finalMeasurement = finalMeasurement.setScale(2, RoundingMode.HALF_UP);

        return new EvaluationResult(
                rawVal.setScale(2, RoundingMode.HALF_UP),
                finalMeasurement,
                measurementUnit,
                pricingUnit,
                unitType,
                expression
        );
    }

    /**
     * Infer whether an expression represents AREA or LINEAR measurement.
     */
    public static String inferUnitType(String expression) {
        String cleaned = cleanExpression(expression);
        try {
            BigDecimal baseVal = evaluateRaw(cleaned, new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"));
            BigDecimal scaledVal = evaluateRaw(cleaned, new BigDecimal("20"), new BigDecimal("20"), new BigDecimal("20"));
            if (baseVal.compareTo(BigDecimal.ZERO) > 0 && scaledVal != null) {
                double ratio = scaledVal.divide(baseVal, 2, RoundingMode.HALF_UP).doubleValue();
                if (Math.abs(ratio - 2.0) < 0.5) {
                    return "LINEAR";
                }
            }
        } catch (Exception ignored) {
        }
        return "AREA";
    }

    /**
     * Safely evaluates raw mathematical value using recursive descent parser.
     */
    public static BigDecimal evaluateRaw(
            String cleanedExpr,
            BigDecimal length,
            BigDecimal width,
            BigDecimal height
    ) {
        List<Token> tokens = tokenize(cleanedExpr, length, width, height);
        Parser parser = new Parser(tokens);
        return parser.parse();
    }

    public static Set<String> detectVariables(String expr) {
        Set<String> vars = new LinkedHashSet<>();
        String upper = expr.toUpperCase();
        if (upper.contains("L")) vars.add("L");
        if (upper.contains("W")) vars.add("W");
        if (upper.contains("H")) vars.add("H");
        return vars;
    }

    public static String cleanExpression(String expr) {
        if (expr == null) return "";
        return expr.trim()
                .replace("×", "*")
                .replace("X", "*")
                .replace("x", "*")
                .replaceAll("(?i)\\bLENGTH\\b", "L")
                .replaceAll("(?i)\\bWIDTH\\b", "W")
                .replaceAll("(?i)\\bHEIGHT\\b", "H")
                .replaceAll("\\s+", "");
    }

    // =========================================================================
    // TOKENIZER & PARSER
    // =========================================================================

    private enum TokenType { NUMBER, PLUS, MINUS, MULTIPLY, DIVIDE, LPAREN, RPAREN }

    private record Token(TokenType type, BigDecimal value) {}

    private static List<Token> tokenize(String expr, BigDecimal l, BigDecimal w, BigDecimal h) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int len = expr.length();

        while (i < len) {
            char c = expr.charAt(i);

            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            if (c == '+') {
                tokens.add(new Token(TokenType.PLUS, null));
                i++;
            } else if (c == '-') {
                tokens.add(new Token(TokenType.MINUS, null));
                i++;
            } else if (c == '*') {
                tokens.add(new Token(TokenType.MULTIPLY, null));
                i++;
            } else if (c == '/') {
                tokens.add(new Token(TokenType.DIVIDE, null));
                i++;
            } else if (c == '(') {
                // If preceded by a number or RPAREN, insert implicit MULTIPLY
                if (!tokens.isEmpty() && (tokens.get(tokens.size() - 1).type == TokenType.NUMBER || tokens.get(tokens.size() - 1).type == TokenType.RPAREN)) {
                    tokens.add(new Token(TokenType.MULTIPLY, null));
                }
                tokens.add(new Token(TokenType.LPAREN, null));
                i++;
            } else if (c == ')') {
                tokens.add(new Token(TokenType.RPAREN, null));
                i++;
            } else if (c == 'L' || c == 'l') {
                if (!tokens.isEmpty() && (tokens.get(tokens.size() - 1).type == TokenType.NUMBER || tokens.get(tokens.size() - 1).type == TokenType.RPAREN)) {
                    tokens.add(new Token(TokenType.MULTIPLY, null));
                }
                tokens.add(new Token(TokenType.NUMBER, l != null ? l : BigDecimal.ZERO));
                i++;
            } else if (c == 'W' || c == 'w') {
                if (!tokens.isEmpty() && (tokens.get(tokens.size() - 1).type == TokenType.NUMBER || tokens.get(tokens.size() - 1).type == TokenType.RPAREN)) {
                    tokens.add(new Token(TokenType.MULTIPLY, null));
                }
                tokens.add(new Token(TokenType.NUMBER, w != null ? w : BigDecimal.ZERO));
                i++;
            } else if (c == 'H' || c == 'h') {
                if (!tokens.isEmpty() && (tokens.get(tokens.size() - 1).type == TokenType.NUMBER || tokens.get(tokens.size() - 1).type == TokenType.RPAREN)) {
                    tokens.add(new Token(TokenType.MULTIPLY, null));
                }
                tokens.add(new Token(TokenType.NUMBER, h != null ? h : BigDecimal.ZERO));
                i++;
            } else if (Character.isDigit(c) || c == '.') {
                if (!tokens.isEmpty() && tokens.get(tokens.size() - 1).type == TokenType.RPAREN) {
                    tokens.add(new Token(TokenType.MULTIPLY, null));
                }
                int start = i;
                while (i < len && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
                    i++;
                }
                String numStr = expr.substring(start, i);
                tokens.add(new Token(TokenType.NUMBER, new BigDecimal(numStr)));
            } else {
                throw new IllegalArgumentException("Unexpected character: '" + c + "'");
            }
        }
        return tokens;
    }

    private static class Parser {
        private final List<Token> tokens;
        private int pos = 0;

        Parser(List<Token> tokens) {
            this.tokens = tokens;
        }

        BigDecimal parse() {
            if (tokens.isEmpty()) {
                return BigDecimal.ZERO;
            }
            BigDecimal result = expression();
            if (pos < tokens.size()) {
                throw new IllegalArgumentException("Unexpected token after expression: " + tokens.get(pos).type);
            }
            return result;
        }

        // expression = term (('+' | '-') term)*
        private BigDecimal expression() {
            BigDecimal left = term();
            while (pos < tokens.size()) {
                Token token = tokens.get(pos);
                if (token.type == TokenType.PLUS) {
                    pos++;
                    left = left.add(term(), MC);
                } else if (token.type == TokenType.MINUS) {
                    pos++;
                    left = left.subtract(term(), MC);
                } else {
                    break;
                }
            }
            return left;
        }

        // term = factor (('*' | '/') factor)*
        private BigDecimal term() {
            BigDecimal left = factor();
            while (pos < tokens.size()) {
                Token token = tokens.get(pos);
                if (token.type == TokenType.MULTIPLY) {
                    pos++;
                    left = left.multiply(factor(), MC);
                } else if (token.type == TokenType.DIVIDE) {
                    pos++;
                    BigDecimal divisor = factor();
                    if (divisor.compareTo(BigDecimal.ZERO) == 0) {
                        throw new IllegalArgumentException("Division by zero in formula.");
                    }
                    left = left.divide(divisor, 10, RoundingMode.HALF_UP);
                } else {
                    break;
                }
            }
            return left;
        }

        // factor = ('+' | '-')? primary
        private BigDecimal factor() {
            if (pos >= tokens.size()) {
                throw new IllegalArgumentException("Unexpected end of formula.");
            }
            Token token = tokens.get(pos);
            if (token.type == TokenType.PLUS) {
                pos++;
                return factor();
            } else if (token.type == TokenType.MINUS) {
                pos++;
                return factor().negate();
            }
            return primary();
        }

        // primary = NUMBER | '(' expression ')'
        private BigDecimal primary() {
            if (pos >= tokens.size()) {
                throw new IllegalArgumentException("Unexpected end of formula.");
            }
            Token token = tokens.get(pos);
            if (token.type == TokenType.NUMBER) {
                pos++;
                return token.value;
            } else if (token.type == TokenType.LPAREN) {
                pos++;
                BigDecimal val = expression();
                if (pos >= tokens.size() || tokens.get(pos).type != TokenType.RPAREN) {
                    throw new IllegalArgumentException("Missing closing parenthesis ')'.");
                }
                pos++; // consume ')'
                return val;
            } else {
                throw new IllegalArgumentException("Unexpected token: " + token.type);
            }
        }
    }
}
