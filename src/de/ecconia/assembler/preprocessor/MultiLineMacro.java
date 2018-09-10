package de.ecconia.assembler.preprocessor;

import java.util.ArrayList;
import java.util.HashMap;

import de.ecconia.assembler.InstructionLine;
import de.ecconia.assembler.io.FileParseException;

public class MultiLineMacro extends Macro {

	private static int expansionCount = 0;
	
	public MultiLineMacro(String name, int numParameters, String expansion, InstructionLine line) throws FileParseException {
		super(name, expansion, line);
		super.parameters = new String[numParameters];
		for(int i = 0; i < numParameters; i++) {
			super.parameters[i] = "%" + (i + 1);
		}
	}

	public static String getKey(String macro) {
		macro = macro.trim();
		
		int index = 0;
		while(index < macro.length() && !Character.isWhitespace(macro.charAt(index)))
			index++;
		
		ArrayList<String> sections = StringHelper.splitOn(macro.substring(index).trim(), ',');
		
		return macro.substring(0, index) + "[" + sections.size() + "]";
	}
	
	@Override
	public String evaluateExpansion(String call, InstructionLine line) throws FileParseException {
		call = call.trim();
		
		int index = 0;
		while(index < call.length() && !Character.isWhitespace(call.charAt(index)))
			index++;
		
		ArrayList<String> args = StringHelper.splitOn(call.substring(index).trim(), ',');
		if(args.size() != parameters.length)
			throw new FileParseException("Unable to process macro arguments", line);
		
		String expand = expansion;
		for(int i = args.size() - 1; i >= 0; i--) {
			expand = expand.replace("%" + (i + 1), args.get(i));
		}
		expand = expand.replace("%%", "..@");
		
		HashMap<String, String> swap = new HashMap<String, String>();
		String[] split = StringHelper.splitNewline(expand);
		for(String s : split) {
			int i = StringHelper.indexOfColon(s);
			
			if(i >= 0) {
				String label = s.substring(0, i).trim();
				
				if(label.startsWith("..@")) {
					if(!StringHelper.isLegalVarName(label.substring(3)))
						throw new FileParseException("Illegal label name \"" + label + "\"", line);
					
					if(swap.containsKey(label))
						throw new FileParseException("Duplicate label name \"" + label + "\"", line);
					
					swap.put(label, "..@" + expansionCount + "." + label.substring(3));
				}
			}
		}
		
		expand = StringHelper.swap(expand, swap);
		
		expansionCount++;
		return expand;
	}
}
