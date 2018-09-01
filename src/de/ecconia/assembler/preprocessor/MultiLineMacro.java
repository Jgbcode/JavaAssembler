package de.ecconia.assembler.preprocessor;

import java.util.ArrayList;

import de.ecconia.assembler.InstructionLine;
import de.ecconia.assembler.io.FileParseException;

public class MultiLineMacro extends Macro {

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
		
		return expand;
	}
}
