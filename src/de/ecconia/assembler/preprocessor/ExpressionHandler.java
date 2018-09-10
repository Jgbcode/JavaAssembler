package de.ecconia.assembler.preprocessor;

import java.util.HashMap;
import java.util.HashSet;

import de.ecconia.assembler.InstructionLine;
import de.ecconia.assembler.io.FileParseException;

public class ExpressionHandler {
	private static HashMap<String, Macro> macros;
	private static HashSet<String> stack;
	static {
		stack = new HashSet<String>();
	}
	
	// Evaluates an expression
	public static String evaluate(String expr, InstructionLine line) throws FileParseException {
		expr = expr.trim();
		
		if(expr.isEmpty())
			return expr;
		
		int result;
		try {
			result = ExpressionParser.parseExpression(expr);
		}
		catch(Exception e) {
			throw new FileParseException(e.getMessage(), line);
		}
		
		return Integer.toString(result);
	}
	
	// Recursively expands macros in the provided expression
	public static String expand(String expr, InstructionLine line) throws FileParseException {
		expr = expr.trim();
		expr = bracketHandler(expr, line);
		
		int i1 = expr.length() - 1;
		int i2 = expr.length() - 1;
		
		while(i2 >= 0) {
			if(StringHelper.isLegalVarChar(expr.charAt(i2)) && !StringHelper.isInString(expr, i2)) {
				i1 = i2 + 1;
				if(i1 < expr.length() && expr.charAt(i1) == '(') {
					int paraDepth = 1;
					while(++i1 < expr.length() && paraDepth != 0) {
						if(expr.charAt(i1) == '(')
							paraDepth++;
						else if(expr.charAt(i1) == ')')
							paraDepth--;
					}
					
					if(paraDepth != 0)
						throw new FileParseException("Unbalanced parentheses", line);
				}
				while(--i2 >= 0 && StringHelper.isLegalVarChar(expr.charAt(i2)));
				String key = Macro.getKey(expr.substring(i2 + 1, i1));
				Macro mac = macros.get(key);
				
				if(mac instanceof MultiLineMacro) {
					throw new FileParseException("Cannot expand multi-line macro inside of expression", line);
				}
				else if(mac != null && !stack.contains(key)) {
					stack.add(key);
					String expansion = mac.evaluateExpansion(expr.substring(i2 + 1, i1), line);
					expr = expr.substring(0, i2 + 1) + expand(expansion, line) + expr.substring(i1);
					stack.remove(key);
				}
			}
			
			i2--;
		}
		
		return expr;
	}
	
	public static void setMacros(HashMap<String, Macro> _macros) {
		macros = _macros;
	}
	
	private static String bracketHandler(String expr, InstructionLine line) throws FileParseException {
		int brktIndex = expr.indexOf("%[");
		
		while(brktIndex != -1) {
			int brktClose = brktIndex + 2;
			int brktDepth = 0;
			while(brktDepth != 0 || expr.charAt(brktClose) != ']') {
				switch(expr.charAt(brktClose)) {
				case '[': brktDepth++; break;
				case ']': brktDepth--; break;
				}
				
				brktClose++;
				if(brktClose >= expr.length())
					throw new FileParseException("Missing closing bracket", line);
			}
			
			expr = expr.substring(0, brktIndex) + expand(expr.substring(brktIndex + 2, brktClose), line) + expr.substring(brktClose + 1);
			brktIndex = expr.indexOf("%[");
		}
		
		return expr;
	}
}
