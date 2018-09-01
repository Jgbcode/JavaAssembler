package de.ecconia.assembler.preprocessor;

import java.util.HashMap;

public class ExpressionParser {
	
	// Hashmap of operators and there order of operations priority, higher priority gets executed first
	private static final HashMap<Character, Integer> order_of_ops;
	static {
		order_of_ops = new HashMap<Character, Integer>();
		order_of_ops.put('\u2228', 0);		// Logical OR
		order_of_ops.put('\u2227', 1);		// Logical AND
		order_of_ops.put('|', 2);
		order_of_ops.put('^', 3);
		order_of_ops.put('&', 4);
		order_of_ops.put('\uFF1D', 5);		// Equals
		order_of_ops.put('\u2260', 5);		// Not equals
		order_of_ops.put('<', 6);
		order_of_ops.put('>', 6);
		order_of_ops.put('\u2264', 6);		// Less than or equal
		order_of_ops.put('\u2265', 6);		// Greater than or equal
		order_of_ops.put('\u2192', 7);		// Right shift
		order_of_ops.put('\u2190', 7);		// Left shift
		order_of_ops.put('+', 8);
		order_of_ops.put('-', 8);
		order_of_ops.put('*', 9);
		order_of_ops.put('/', 9);
		order_of_ops.put('%', 9);
		order_of_ops.put('~', 10);
	}
	
	// The number of priority layers
	private static final int order_of_op_layers = 6;
	
	// Returns integer from string
	// Hex values are prefixed with "0x"
	// Binary values are suffixed with "b"
	// Octal values are suffixed with "o"
	// Decimal values are the default
	private static int getNumber(String num) throws NumberFormatException {		
		num = num.trim();
		if(num.isEmpty())
			throw new NumberFormatException("Attempting to convert empty string");
		
		int radix = 10;
		
		if(num.startsWith("0x")) {
			radix = 16;
			num = num.substring(2);
		}
		else if(num.endsWith("b")) {
			radix = 2;
			num = num.substring(0, num.length() - 1);
		}
		else if(num.endsWith("o")) {
			radix = 8;
			num = num.substring(0, num.length() - 1);
		}
		else if(num.startsWith("'") && num.endsWith("'")) {
			if(num.length() == 3)
				return (int)num.charAt(1);
			else
				throw new NumberFormatException("Illegal character conversion");
		}
		
		return Integer.parseInt(num, radix);
	}
	
	// Evaluates a simple expression given two inputs and an operator
	// Note: ~ is a single operand operation so num1 is ignored
	private static int evalSimpleExpression(int num1, int num2, char op) throws NumberFormatException, ArithmeticException {
		int result;
		
		switch(op) {
		case '+': result = num1 + num2; break;
		case '-': result = num1 - num2; break;
		case '*': result = num1 * num2; break;
		case '/': result = num1 / num2; break;
		case '%': result = num1 % num2; break;
		case '&': result = num1 & num2; break;
		case '|': result = num1 | num2; break;
		case '^': result = num1 ^ num2; break;
		case '~': result = ~num2; break;
		case '\u2228': result = (num1 != 0) || (num2 != 0) ? 1 : 0; break;
		case'\u2227': result = (num1 != 0) && (num2 != 0) ? 1 : 0; break;
		case '\uFF1D': result = (num1 == num2) ? 1 : 0; break;
		case '\u2260': result = (num1 != num2) ? 1 : 0; break;
		case '<': result = (num1 < num2) ? 1 : 0; break;
		case '>': result = (num1 > num2) ? 1 : 0; break;
		case '\u2264': result = (num1 <= num2) ? 1 : 0; break;
		case '\u2265': result = (num1 >= num2) ? 1 : 0; break;
		case '\u2192': result = num1 >> num2; break;
		case '\u2190': result = num1 << num2; break;
		default: throw new ArithmeticException("Unknown operator '" + op + "'");
		}
		
		return result;
	}
	
	public static int parseExpression(String expr) throws NumberFormatException, ArithmeticException {
		expr = expr.replace("||", "\u2228");
		expr = expr.replace("&&", "\u2227");
		expr = expr.replace("==", "\uFF1D");
		expr = expr.replace("!=", "\u2260");
		expr = expr.replace("<=", "\u2264");
		expr = expr.replace(">=", "\u2265");
		expr = expr.replace(">>", "\u2192");
		expr = expr.replace("<<", "\u2190");
		return parseExpressionRecurse(expr);
	}
	
	// Recursively parses any complex expression
	private static int parseExpressionRecurse(String expr) throws NumberFormatException, ArithmeticException {
		expr = expr.trim();
		
		while(!expr.isEmpty() && expr.charAt(0) == '(' && expr.charAt(expr.length() - 1) == ')')
			expr = expr.substring(1, expr.length() - 1).trim();
		
		int priority = Integer.MAX_VALUE;
		int op_index = -1;
		int para_depth = 0;
		char op = '\0';
		
		for(int i = expr.length() - 1; i >= 0; i--) {
			if(expr.charAt(i) == '(')
				para_depth--;
			else if(expr.charAt(i) == ')')
				para_depth++;
			
			if(para_depth < 0)
				throw new ArithmeticException("Unbalanced parentheses");
			
			Integer temp = order_of_ops.get(expr.charAt(i));
			
			boolean isOneOpMinus = false;
			
			if(expr.charAt(i) == '-') {
				int j = i - 1;
				while(j > 0 && Character.isWhitespace(expr.charAt(j)))
					j--;
				
				if(j < 0 || isOperator(expr.charAt(j)) || expr.charAt(j) == ')' || expr.charAt(j) == '(' || Character.isWhitespace(expr.charAt(j))) {
					temp = 5;
					isOneOpMinus = true;
				}
			}
			
			if(temp != null) {
				temp += para_depth * order_of_op_layers;
				if(temp < priority) {
					priority = temp;
					op_index = i;
					op = (!isOneOpMinus) ? expr.charAt(op_index) : '0';
				}
			}
		}
		
		if(para_depth != 0)
			throw new ArithmeticException("Unbalanced parentheses");
		
		if(op_index == -1) {
			return getNumber(expr);
		}
		else {
			
			if(op == '~') {
				return evalSimpleExpression(0, parseExpressionRecurse(expr.substring(op_index + 1)), op);
			}
			else if(op == '0') {
				return evalSimpleExpression(0, parseExpressionRecurse(expr.substring(op_index + 1)), '-');
			}
			else {
				return evalSimpleExpression(parseExpressionRecurse(expr.substring(0, op_index)), parseExpression(expr.substring(op_index + 1)), op);
			}
		}
	}
	
	public static boolean isOperator(char op) {
		return order_of_ops.containsKey(op);
	}
}
