package de.ecconia.assembler.preprocessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import de.ecconia.assembler.InstructionLine;
import de.ecconia.assembler.io.FileParseException;

public class Macro {
	protected String name;
	protected String[] parameters;
	protected String expansion;
	protected InstructionLine line;
	
	// Gets the key of a macro, returns null if the input string is invalid
	public static String getKey(String macro) {
		int index1 = macro.indexOf('(');
		int index2 = macro.lastIndexOf(')');
		
		if(index1 > index2)
			return null;
		
		if(index1 == -1)
			return macro + "[0]";
		else
			return macro.substring(0, index1).trim() + "[" + StringHelper.splitOn(macro.substring(index1 + 1, index2), ',').size() + "]";
	}
	
	public Macro(String definition, String expansion, InstructionLine line) throws FileParseException {
		this.line = line;
		
		int index1 = definition.indexOf('(');
		int index2 = definition.indexOf(')');
		
		if(index1 > index2)
			throw new FileParseException("Invalid macro definition", line);
		
		if(index1 == -1) {
			this.name = definition.trim();
			this.parameters = new String[0];
			this.expansion = expansion;
		}
		else {
			if(index2 != definition.trim().length() - 1)
				throw new FileParseException("Invalid macro definition", line);
			
			this.name = definition.substring(0, index1).trim();
			this.parameters = definition.substring(index1 + 1, index2).split(",");
			this.expansion = expansion;
		}
		
		if(!StringHelper.isLegalVarName(this.name))
			throw new FileParseException("Illegal macro name: " + name, line);
		
		HashSet<String> duplicate = new HashSet<String>();
		for(String para : parameters) {
			para = para.trim();
			if(!StringHelper.isLegalVarName(para))
				throw new FileParseException("Illegal macro parameter name: " + para, line);
			if(duplicate.contains(para))
				throw new FileParseException("Duplicate macro argument names", line);
			duplicate.add(para);
		}
	}
	
	// Converts macro into a key
	// The key takes the form "<name>[<num_parameters>]"
	public String getKey() {
		return this.name + "[" + this.parameters.length + "]";
	}
	
	public String getName() {
		return this.name;
	}
	
	public int getNumParameters() {
		return this.parameters.length;
	}
	
	public String getExpansion() {
		return this.expansion;
	}
	
	public String evaluateExpansion(String call, InstructionLine line) throws FileParseException {
		int open = call.indexOf('(');
		int close = call.lastIndexOf(')');
		
		if(open > close)
			throw new FileParseException("Unable to parse macro call", line);
		
		if(open == -1 && parameters.length == 0)
			return expansion;
		
		ArrayList<String> args = StringHelper.splitOn(call.substring(open + 1, close), ',');
		if(args.size() != parameters.length)
			throw new FileParseException("Unable to process macro arguments", line);
		
		HashMap<String, String> convert = new HashMap<String, String>();
		for(int i = 0; i < parameters.length; i++)
			convert.put(parameters[i], args.get(i));
	
		return StringHelper.swap(expansion, convert);
	}
	
	public InstructionLine getLine() {
		return this.line;
	}
}
