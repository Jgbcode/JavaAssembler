package de.ecconia.assembler.preprocessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.ecconia.assembler.InstructionLine;
import de.ecconia.assembler.MacroInstructionLine;
import de.ecconia.assembler.io.FileParseException;
import de.ecconia.assembler.io.FileWrap;

public class Preprocessor {
	private static int startingAddress;
	private static HashMap<String, Macro> macros;
	static {
		macros = new HashMap<String, Macro>();
	}
	
	// Runs the preprocessor and returns the updated lines of code
	public static ArrayList<InstructionLine> runPreprocessor(List<String> lines, String file) throws FileParseException {
		ArrayList<InstructionLine> iLines = new ArrayList<InstructionLine>();
		ExpressionHandler.setMacros(macros);
		
		for(int i = 0; i < lines.size(); i++) {
			iLines.add(new InstructionLine(lines.get(i), i + 1, file));	
		}
		
		System.out.println("Processing directives.");
		processDirectives(iLines);
		System.out.println("Evaluating expressions.");
		evaluateExpressions(iLines);
		System.out.println("Preprocessor complete.");
		
		return iLines;
	}
	
	private static void evaluateExpressions(ArrayList<InstructionLine> lines) throws FileParseException {
		for(InstructionLine line : lines) {
			String[] split = line.splitOnCommas();
			for(int i = 1; i < split.length; i++)
				split[i] = ExpressionHandler.expand(split[i], line);
				
			for(int i = 2; i < split.length; i++)
				split[i] = ExpressionHandler.evaluate(split[i], line);
			
			line.setSplitContent(split);
		}
	}
	
	private static void processDirectives(ArrayList<InstructionLine> lines) throws FileParseException {
		int currentAddress = 0;
		
		for(int i = 0; i < lines.size(); i++) {
			InstructionLine line = lines.get(i);
			
			String[] sections = line.splitOnCommas();
			
			if(!sections[0].isEmpty()) {
				if(macros.get(sections[0] + "[0]") == null) {
					macros.put(sections[0] + "[0]", new Macro(sections[0], Integer.toString(startingAddress + currentAddress), lines.get(i)));
				}
				else {
					throw new FileParseException("Duplicate macro or label name", lines.get(i));
				}
			}

			switch(sections[1].toLowerCase()) {
			case "org":
				processOrgDirective(line);
				lines.remove(i);
				i--;
				break;
			case "%include":
				lines.addAll(i + 1, processIncludeDirective(lines.get(i)));
				lines.remove(i);
				i--;
				break;
			case "%define":
				processDefineDirective(lines.get(i));
				lines.remove(i);
				i--;
				break;
			case "%macro":
				processMacroDirective(lines, i);
				i--;
				break;
			default:
				if(!sections[1].isEmpty()) {
					Macro m = macros.get(MultiLineMacro.getKey(lines.get(i).getContent()));
					if(m instanceof MultiLineMacro) {
						String[] expansion = StringHelper.splitNewline(m.evaluateExpansion(lines.get(i).getContent(), lines.get(i)));
						for(int j = expansion.length - 1; j >= 0; j--) {
							lines.add(i, new MacroInstructionLine(expansion[j], lines.get(i).getLineNumber(), lines.get(i).getFile(), j + 1, lines.get(i).getContent()));
						}
						lines.remove(i + expansion.length);
						i--;
					}
					else if(m == null) {
						lines.get(i).setAddress(startingAddress + currentAddress++);
					}
					else {
						line.setContent(ExpressionHandler.expand(line.getContent(), line));
						i--;
					}
				}
				else {
					lines.get(i).setAddress(startingAddress + currentAddress);
				}
			}
		}
	}
	/*
	private static void processIfDirective(String[] sections, ArrayList<InstructionLine> lines, int index) throws FileParseException {
		if(sections.length != 3)
			throw new FileParseException("Illegal if directive format", lines.get(index));
		
		int ifDepth = 0;
		if(ExpressionHandler.evaluate(sections[2], lines.get(index)).equals("0")) {
			
		}
	}*/
	
	private static void processOrgDirective(InstructionLine line) throws FileParseException {
		String[] sections = line.splitOnSpaces();
		
		if(sections.length != 3)
			throw new FileParseException("Illegal org directive format", line);
		
		try {
			startingAddress = Integer.parseInt(ExpressionHandler.evaluate(sections[2], line));
		}
		catch (Exception e) {
			throw new FileParseException("Illegal org value \"" + sections[2] + "\"", line);
		}
	}
	
	private static ArrayList<InstructionLine> processIncludeDirective(InstructionLine line) throws FileParseException {
		String[] sections = line.splitOnSpaces();
		
		if(sections.length != 3)
			throw new FileParseException("Illegal include directive format", line);
		
		FileWrap file = new FileWrap(sections[2]);
		
		ArrayList<String> rawLines;
		try {
			rawLines = (ArrayList<String>)file.lines();
		}
		catch(Exception e) {
			throw new FileParseException("Unable to read file", line);
		}
		
		ArrayList<InstructionLine> lines = new ArrayList<InstructionLine>();
		for(int i = 0; i < rawLines.size(); i++)
			lines.add(new InstructionLine(rawLines.get(i), i + 1, sections[2]));
		
		return lines;
	}
	
	private static void processDefineDirective(InstructionLine line) throws FileParseException {
		String[] sections = line.splitOnSpaces();
		
		if(sections.length < 3)
			throw new FileParseException("Illegal define directive format", line);
		
		String expansion = "";
		for(int i = 3; i < sections.length; i++)
			expansion += sections[i] + " ";
		
		Macro macro = new Macro(sections[2], expansion, line);
		macros.put(macro.getKey(), macro);
	}
	
	// Returns index of %endmacro
	private static void processMacroDirective(ArrayList<InstructionLine> lines, int index) throws FileParseException {
		String[] sections = lines.get(index).splitOnSpaces();
		
		if(sections.length != 4)
			throw new FileParseException("Illegal macro directive format", lines.get(index));
		
		int numParameters;
		try {
			numParameters = Integer.parseInt(ExpressionHandler.evaluate(sections[3], lines.get(index)));
		}
		catch(Exception e) {
			throw new FileParseException("Invalid number of parameters", lines.get(index));
		}
		
		String expansion = "";
		int depth = 1;
		String[] sec;
		int i = index + 1;
		
		while(true) {
			if(i >= lines.size())
				throw new FileParseException("Run away macro declaration", lines.get(index));
			
			sec = lines.get(i).splitOnSpaces();
			
			if(sec[1].equals("%macro"))
				depth++;
			else if(sec[1].equals("%endmacro")) {
				depth--;
				if(depth == 0) {
					lines.remove(i);
					break;
				}
			}
			
			expansion += lines.get(i).getContent() + "\n";
			lines.remove(i);
		}
		
		expansion = expansion.substring(0, expansion.length() - 1);	// Remove last new line
		Macro macro = new MultiLineMacro(sections[2], numParameters, expansion, lines.get(index));
		lines.remove(index);
		macros.put(macro.getKey(), macro);
	}
}
