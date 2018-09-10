package de.ecconia.assembler.preprocessor;

import java.util.ArrayList;
import java.util.HashMap;

public class StringHelper {
	public static String escapeQuotes(String str) {
		for(int i = str.length() - 1; i >= 0; i--) {
			if(str.charAt(i) == '"' && !isCharEscaped(str, i))
				str = str.substring(0, i) + '\\' + str.substring(i);
		}
		
		return str;
	}
	
	public static int getStringEnd(String str, int start) {
		char c = str.charAt(start);
		
		int index = start + 1;
		while(str.charAt(index) != c || isCharEscaped(str, index)) {
			if(++index >= str.length())
				return -1;
		}
		
		return index;
	}
	
	public static boolean isCharEscaped(String str, int index) {
		boolean isEscaped = false;
		while(--index >= 0 && str.charAt(index) == '\\')
				isEscaped = !isEscaped;
		
		return isEscaped;
	}
	
	// Returns true if the index is in a quoted string
	public static boolean isInString(String str, int index) {
		if(index <= 0 || index >= str.length() - 1)
			return false;
		
		int start = 0;
		
		while(start < index) {
			start = Math.min(str.indexOf('\''), str.indexOf('"'));
			if(start < 0)
				return false;
			
			char c = str.charAt(start);
			
			for(int i = start; i <= index; i++) {
				if(str.charAt(i) == c && !isCharEscaped(str, i))
					continue;
			}
			
			return true;
		}
		
		return false;
	}
	
	// Returns true if the string only contains a single quoted string
	public static boolean isCompleteString(String str) {
		if(str.length() <= 1)
			return false;
		
		char c = str.charAt(0);
		if((c != '"' && c != '\'') || str.charAt(str.length() - 1) != c)
			return false;
		
		for(int i = 1; i < str.length() - 1; i++) {
			if(str.charAt(i) == c && !isCharEscaped(str, i))
				return false;
		}
		
		return true;
	}
	
	public static ArrayList<String> splitOn(String content, char s) {
		ArrayList<String> split = new ArrayList<String>();
		
		if(content.trim().isEmpty())
			return split;
		
		int i1 = 0;
		int i2 = 0;
		
		int paraDepth = 0;
		int brktDepth = 0;
		int curlDepth = 0;
		boolean isDQuote = false;
		boolean isSQuote = false;
		do {
			if(i2 >= content.length() || (content.charAt(i2) == s && paraDepth == 0 && brktDepth == 0 && curlDepth == 0 && !isDQuote && !isSQuote)) {
				split.add(content.substring(i1, i2).trim());
				while(++i2 < content.length() && content.charAt(i2) == s);
				i1 = i2;
				continue;
			}
			else if(content.charAt(i2) == '(' && !isSQuote && !isDQuote)
				paraDepth++;
			else if(content.charAt(i2) == ')' && !isSQuote && !isDQuote)
				paraDepth--;
			else if(content.charAt(i2) == '[' && !isSQuote && !isDQuote)
				brktDepth++;
			else if(content.charAt(i2) == ']' && !isSQuote && !isDQuote)
				brktDepth--;
			else if(content.charAt(i2) == '{' && !isSQuote && !isDQuote)
				curlDepth++;
			else if(content.charAt(i2) == '}' && !isSQuote && !isDQuote)
				curlDepth--;
			else if(content.charAt(i2) == '"' && !isSQuote && !StringHelper.isCharEscaped(content, i2))
				isDQuote = !isDQuote;
			else if(content.charAt(i2) == '\'' && !isDQuote && !StringHelper.isCharEscaped(content, i2))
				isSQuote = !isSQuote;
			
			i2++;
		} while(i2 <= content.length());
		
		return split;
	}
	
	public static ArrayList<String> splitOnWhitespace(String content) {
		content = content.trim();
		ArrayList<String> split = new ArrayList<String>();
		
		if(content.isEmpty())
			return split;
		
		int i1 = 0;
		int i2 = 0;
		
		int paraDepth = 0;
		int brktDepth = 0;
		int curlDepth = 0;
		boolean isDQuote = false;
		boolean isSQuote = false;
		do {
			if(i2 >= content.length() || (Character.isWhitespace(content.charAt(i2)) && paraDepth == 0 && brktDepth == 0 && curlDepth == 0 && !isDQuote && !isSQuote)) {
				split.add(content.substring(i1, i2).trim());
				while(++i2 < content.length() && Character.isWhitespace(content.charAt(i2)));
				i1 = i2;
				continue;
			}
			else if(content.charAt(i2) == '(' && !isSQuote && !isDQuote)
				paraDepth++;
			else if(content.charAt(i2) == ')' && !isSQuote && !isDQuote)
				paraDepth--;
			else if(content.charAt(i2) == '[' && !isSQuote && !isDQuote)
				brktDepth++;
			else if(content.charAt(i2) == ']' && !isSQuote && !isDQuote)
				brktDepth--;
			else if(content.charAt(i2) == '{' && !isSQuote && !isDQuote)
				curlDepth++;
			else if(content.charAt(i2) == '}' && !isSQuote && !isDQuote)
				curlDepth--;
			else if(content.charAt(i2) == '"' && !isSQuote && !StringHelper.isCharEscaped(content, i2))
				isDQuote = !isDQuote;
			else if(content.charAt(i2) == '\'' && !isDQuote && !StringHelper.isCharEscaped(content, i2))
				isSQuote = !isSQuote;
			
			i2++;
		} while(i2 <= content.length());
		
		return split;
	}
	
	public static String swap(String str, HashMap<String, String> swap) {
		int i = 0;
		
		while(i < str.length()) {
			if(isLegalVarChar(str.charAt(i))) {
				int start = i;
				while(isLegalVarChar(str.charAt(i)) && ++i < str.length());
				String replace = swap.get(str.substring(start, i));
				if(replace != null) {
					if(i != str.length())
						str = str.substring(0, start) + replace + str.substring(i);
					else
						str = str.substring(0, start) + replace;
					
					i = start + replace.length();
				}
			}
			else {
				i++;
			}
		}
		
		return str;
	}
	
	// Returns true if the character could legally belong to a variable name
	public static boolean isLegalVarChar(char c) {
		return Character.isLetter(c) || Character.isDigit(c) || c == '_' || c == '$' || c == '.' || c == '@';
	}
	
	// Returns true if name is a valid variable name
	// Variable names must begin with a letter and be alphanumeric, underscores are allowed
	public static boolean isLegalVarName(String name) {
		if(name.isEmpty() || (!Character.isLetter(name.charAt(0)) && !name.startsWith("..@")))
			return false;
		
		for(int i = 0; i < name.length(); i++) {
			if(!isLegalVarChar(name.charAt(i))) {
				return false;
			}
		}
		
		return true;
	}
	
	public static int indexOfColon(String str) {
		int index = str.indexOf(':');
		while(index != -1) {
			if(!isInString(str, index))
				return index;
			index = str.indexOf(':');
		}
		
		return -1;
	}
	
	public static String[] splitNewline(String str) {
		return str.split("[\\r\\n]+");
	}
}
