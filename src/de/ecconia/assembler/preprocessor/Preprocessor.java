package de.ecconia.assembler.preprocessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.text.StringEscapeUtils;

import de.ecconia.assembler.Assembler;
import de.ecconia.assembler.InstructionLine;
import de.ecconia.assembler.MacroInstructionLine;
import de.ecconia.assembler.io.FileParseException;
import de.ecconia.assembler.io.FileWrap;

public class Preprocessor {
	private static int startingAddress = 0;
	private static int currentAddress = 0;
	private static Context context;
	static {
		context = new Context(null);
	}
	
	// Runs the preprocessor and returns the updated lines of code
	// List<String> lines - the lines in the main file
	// String file - the name of the main file
	// List<InstructionLine> includes - code defined by the ISA aka convenience macros
	public static List<InstructionLine> runPreprocessor(List<String> lines, String file, List<InstructionLine> includes) throws FileParseException {
		ArrayList<InstructionLine> iLines = new ArrayList<InstructionLine>();
		ExpressionHandler.setMacros(context.current);
		
		for(int i = 0; i < lines.size(); i++) {
			iLines.add(new InstructionLine(lines.get(i), i + 1, file));	
		}
		
		iLines.addAll(0, includes);
		
		int cut = processDirectives(iLines);
		if(cut >= 0) {
			for(int i = iLines.size() - 1; i >= cut; i--)
				iLines.remove(i);
		}
		
		evaluateExpressions(iLines);
		
		return iLines;
	}
	
	private static void evaluateExpressions(List<InstructionLine> lines) throws FileParseException {
		for(int l = 0; l < lines.size(); l++) {
			InstructionLine line = lines.get(l);
			String[] split = line.splitOnCommas(-1);

			HashMap<String, Macro> localMacros = new HashMap<String, Macro>();
			for(Macro m : context.current.values()) {
				if(m.getLine().getAddress() > line.getAddress())
					localMacros.put(m.getKey(), m);
			}
			
			localMacros.put("$[0]", new Macro("$", Integer.toString(line.getAddress()), line, false));
			
			for(int i = 1; i < split.length; i++) {
				ExpressionHandler.setMacros(localMacros);
				split[i] = ExpressionHandler.expand(split[i], line);
			}
					
			for(int i = 2; i < split.length; i++)
				split[i] = ExpressionHandler.evaluate(split[i], line);
				
			line.setSplitContent(split);
		}
	}
	
	private static int processDirectives(List<InstructionLine> lines) throws FileParseException {
		for(int i = 0; i < lines.size(); i++) {
			InstructionLine line = lines.get(i);
			line.setAddress(startingAddress + currentAddress);
			
			String[] sections = line.splitOnCommas(-1);
			
			if(!sections[0].isEmpty()) {
				if(context.current.get(sections[0] + "[0]") == null) {
					context.current.put(sections[0] + "[0]", new Macro(sections[0], Integer.toString(startingAddress + currentAddress), lines.get(i)));
				}
				else {
					throw new FileParseException("Duplicate macro or label name", lines.get(i));
				}
			}

			// Handle pre-expansion phase directives
			switch(sections[1].toLowerCase()) {
			case "org":
				processOrgDirective(line);
				lines.remove(i);
				i--;
				break;
			case "%include":
				lines.addAll(i + 1, processIncludeDirective(line));
				lines.remove(i);
				i--;
				break;
			case "%define":
				processDefineDirective(line);
				lines.remove(i);
				i--;
				break;
			case "%macro":
				processMacroDirective(lines, i);
				i--;
				break;
			case "%if":
				processIfDirective(lines, i);
				i--;
				break;
			case "%assign":
				processAssignDirective(line);
				lines.remove(i);
				i--;
				break;
			case "%xdefine":
				processXdefineDirective(line);
				lines.remove(i);
				i--;
				break;
			case "%rep":
				i += processRepDirective(lines, i) - 1;
				break;
			case "%exitrep":
			case "%exit":
				return i;
			case "%defstr":
				processDefstrDirective(line);
				lines.remove(i);
				i--;
				break;
			case "%undef":
				processUndefDirective(line);
				lines.remove(i);
				i--;
				break;
			case "%deftok":
				processDeftokDirective(line);
				lines.remove(i);
				i--;
				break;
			case "%strcat":
				processStrcatDirective(line);
				lines.remove(i);
				i--;
				break;
			case "%strlen":
				processStrlenDirective(line);
				lines.remove(i);
				i--;
				break;
			case "%substr":
				processSubstrDirective(line);
				lines.remove(i);
				i--;
				break;
			default:
				if(!sections[1].startsWith("%")) {
					if(!sections[1].isEmpty()) {
						Macro m = context.current.get(MultiLineMacro.getKey(line.getContent()));
						if(m instanceof MultiLineMacro) {
							String[] expansion = StringHelper.splitNewline(m.evaluateExpansion(line.getContent(), line));
							for(int j = expansion.length - 1; j >= 0; j--) {
								lines.add(i, new MacroInstructionLine(expansion[j], m.getLine().getLineNumber() + j + 1, m.getLine().getFile(), j + 1, lines.get(i + expansion.length - j - 1)));
							}
							lines.remove(i + expansion.length);
							i--;
						}
						else if(m == null) {
							currentAddress++;
							
							// Handle expansions that are currently possible
							for(int j = 2; j < sections.length; j++)
								sections[j] = ExpressionHandler.expand(sections[j], line);
							line.setSplitContent(sections);
						}
						else {
							line.setContent(ExpressionHandler.expand(line.getContent(), line));
							i--;
						}
					}
				}
			}
		}
		
		return -1;
	}
	
	private static void processSubstrDirective(InstructionLine line) throws FileParseException {
		String[] sections = line.splitOnSpaces(5);
		for(String s : sections)
			System.out.println(s);
		
		if(sections.length != 5)
			throw new FileParseException("Illegal substr directive format", line);
		
		sections[3] = ExpressionHandler.expand(sections[3], line);
		if(!StringHelper.isCompleteString(sections[3]))
			throw new FileParseException("Parameter is not a valid string: " + sections[3], line);
		
		String str = StringEscapeUtils.unescapeJava(sections[3].substring(1, sections[3].length() - 1));
		
		String[] paras = StringHelper.splitOn(sections[4], ',').toArray(new String[0]);
		if(paras.length != 1 && paras.length != 2)
			throw new FileParseException("Incorrect parameter formatting", line);
		
		int start = Integer.parseInt(ExpressionHandler.evaluate(ExpressionHandler.expand(paras[0], line), line)) - 1;
		int end = 1;
		if(paras.length == 2) {
			end = Integer.parseInt(ExpressionHandler.evaluate(ExpressionHandler.expand(paras[1], line), line));
			if(end < 0)
				end = str.length() + end - start + 1;
		}
		
		if(start < 0)
			throw new FileParseException("Substring starting index cannot be less than 1", line);
		if(end < 0)
			throw new FileParseException("Cannot return substring of negative length", line);
		if(start + end > str.length())
			throw new FileParseException("Substring bounds exceed string length", line);
		
		String result = "\"" + StringEscapeUtils.escapeJava(str.substring(start,start + end)) + "\"";
		Macro m = new Macro(sections[2], result, line);
		context.current.put(m.getKey(), m);
	}
	
	private static void processStrlenDirective(InstructionLine line) throws FileParseException {
		String[] sections = line.splitOnSpaces(4);
		
		if(sections.length != 4)
			throw new FileParseException("Illegal strlen directive format", line);
		
		sections[3] = ExpressionHandler.expand(sections[3], line);
		if(!StringHelper.isCompleteString(sections[3]))
			throw new FileParseException("Parameter is not a valid string: " + sections[3], line);
		
		String str = StringEscapeUtils.unescapeJava(sections[3].substring(1, sections[3].length() - 1));
		Macro m = new Macro(sections[2], Integer.toString(str.length()), line);
		context.current.put(m.getKey(), m);
	}
	
	private static void processStrcatDirective(InstructionLine line) throws FileParseException {
		String[] sections = line.splitOnSpaces(4);
		
		if(sections.length != 4)
			throw new FileParseException("Illegal strcat directive format", line);
		
		String[] strs = StringHelper.splitOn(sections[3], ',').toArray(new String[0]);
		if(strs.length != 2) {
			strs = StringHelper.splitOnWhitespace(sections[3]).toArray(new String[0]);
			if(strs.length != 2)
				throw new FileParseException("Incorrect parameter formatting", line);
		}
		
		strs[0] = ExpressionHandler.expand(strs[0], line);
		strs[1] = ExpressionHandler.expand(strs[1], line);
		
		if(!StringHelper.isCompleteString(strs[0]))
			throw new FileParseException("Parameter is not a valid string: " + strs[0], line);
		
		if(!StringHelper.isCompleteString(strs[1]))
			throw new FileParseException("Parameter is not a valid string: " + strs[1], line);
		
		strs[0] = StringEscapeUtils.unescapeJava(strs[0].substring(1, strs[0].length() - 1));
		strs[1] = StringEscapeUtils.unescapeJava(strs[1].substring(1, strs[1].length() - 1));
		String result = "\"" + StringEscapeUtils.escapeJava(strs[0] + strs[1]) + "\"";
		
		Macro m = new Macro(sections[2], result, line);
		context.current.put(m.getKey(), m);
	}
	
	private static void processDeftokDirective(InstructionLine line) throws FileParseException {
		String[] sections = line.splitOnSpaces(4);
		
		if(sections.length != 4)
			throw new FileParseException("Illegal deftok directive format", line);
		
		sections[3] = ExpressionHandler.expand(sections[3], line);
		if(!StringHelper.isCompleteString(sections[3]))
			throw new FileParseException("Parameter is not a valid string: " + sections[3], line);
		
		String str = StringEscapeUtils.unescapeJava(sections[3].substring(1, sections[3].length() - 1));
		Macro m = new Macro(sections[2], str, line);
		context.current.put(m.getKey(), m);
	}
	
	private static void processUndefDirective(InstructionLine line) throws FileParseException {
		String[] sections = line.splitOnSpaces(3);
		
		if(sections.length != 3)
			throw new FileParseException("Illegal undef directive format", line);
		
		context.current.remove(Macro.getKey(sections[2]));
	}
	
	private static void processDefstrDirective(InstructionLine line) throws FileParseException {
		String[] sections = line.splitOnSpaces(4);
		
		if(sections.length < 3)
			throw new FileParseException("Illegal defstr directive format", line);
		
		String str = "";
		if(sections.length == 4)
			str = "\"" + StringHelper.escapeQuotes(ExpressionHandler.expand(sections[3], line)) + "\"";
		
		Macro m = new Macro(sections[2], str, line);
		context.current.put(m.getKey(), m);
	}
	
	private static int processRepDirective(List<InstructionLine> lines, int index) throws FileParseException {
		InstructionLine line = lines.get(index);
		String[] repSec = line.splitOnSpaces(3);
		
		if(repSec.length < 3)
			throw new FileParseException("Illegal rep directive format", line);
		
		int repDepth = 1;
		int i = index;
		while(repDepth >= 1) {
			if(++i >= lines.size())
				throw new FileParseException("Run away rep directive", line);
			
			String[] sections = lines.get(i).splitOnSpaces(3);
			switch(sections[1]) {
			case "%rep":
				repDepth++;
				break;
			case "%endrep":
				repDepth--;
				break;
			}
		}
		
		int numReps = Integer.parseInt(ExpressionHandler.evaluate(ExpressionHandler.expand(repSec[2], line), line));
		int repSize = i - index - 1;
		
		ArrayList<InstructionLine> rep = new ArrayList<InstructionLine>();
		lines.remove(i);
		lines.remove(index);
		for(int j = 0; j < repSize; j++)
			rep.add(lines.remove(index));
		
		ArrayList<InstructionLine> repExpand = new ArrayList<InstructionLine>();
		for(int j = 0; j < numReps; j++) {
			for(InstructionLine il : rep) {
				try {
					repExpand.add((InstructionLine)il.clone());
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
					Assembler.die("Clone error during rep processing");
				}
			}
		}
			
		int exit = processDirectives(repExpand);
		if(exit >= 0) {
			for(int j = repExpand.size() - 1; j >= exit; j--)
				repExpand.remove(j);
		}
		lines.addAll(index, repExpand);
		return repExpand.size();
	}
	
	private static void processXdefineDirective(InstructionLine line) throws FileParseException {
		String[] sections = line.splitOnSpaces(4);
		
		if(sections.length != 4)
			throw new FileParseException("Illegal define directive format", line);
		
		String expansion = ExpressionHandler.expand(sections[3], line);
		
		Macro macro = new Macro(sections[2], expansion, line);
		context.current.put(macro.getKey(), macro);
	}
	
	private static void processAssignDirective(InstructionLine line) throws FileParseException {
		System.out.println("Calling assign");
		String[] sections = line.splitOnSpaces(4);
		
		if(sections.length < 4)
			throw new FileParseException("Illegal assign directive format", line);
		
		Macro m = new Macro(sections[2], ExpressionHandler.evaluate(ExpressionHandler.expand(sections[3], line), line), line);
		context.current.put(m.getKey(), m);
	}
	
	private static void processIfDirective(List<InstructionLine> lines, int index) throws FileParseException {
		String[] sections = lines.get(index).splitOnSpaces(3);
		
		if(!sections[1].equals("%else") && sections.length != 3)
			throw new FileParseException("Illegal if directive format", lines.get(index));
		
		if(sections[1].equals("%else") && sections.length != 2) {
			System.out.println(sections.length);
			for(String s : sections)
				System.out.println(s);
			throw new FileParseException("Illegal else directive format", lines.get(index));
		}
		
		String cond = "1";
		if(!sections[1].equals("%else")) {
			// Evaluate condition
			cond = ExpressionHandler.evaluate(ExpressionHandler.expand(sections[2], lines.get(index)), lines.get(index));
		}
			
		// Run if the statement is false
		if(cond.equals("0")) {
			int i = index;
			int ifDepth = 1;
			String instr;
			do {
				i++;
				if(i >= lines.size())
					throw new FileParseException("Run away if declaration", lines.get(index));
				instr = lines.get(i).splitOnSpaces(-1)[1];
				switch(instr) {
				case "%if":
					ifDepth++;
					break;
				case "%endif":
					ifDepth--;
					break;
				}
			} while(ifDepth > 1 || (!instr.equals("%else") && !instr.equals("%elseif") && !instr.equals("%endif")));
			
			// Remove statements inside if which evaluated to 0
			for(int j = index; j < i; j++)
				lines.remove(index);
			
			if(instr.equals("%endif")) {
				lines.remove(index);
			}
			else {
				processIfDirective(lines, index);
			}
		}
		else {	// Run if the statement is true
			InstructionLine line = lines.remove(index);
			int i = index;
			int ifDepth = 1;
			String instr;
			do {
				i++;
				if(i >= lines.size())
					throw new FileParseException("Run away if declaration", line);
				instr = lines.get(i).splitOnSpaces(-1)[1];
				switch(instr) {
				case "%if":
					ifDepth++;
					break;
				case "%endif":
					ifDepth--;
					break;
				}
			} while(ifDepth > 1 || (!instr.equals("%else") && !instr.equals("%elseif") && !instr.equals("%endif")));	
		
			ifDepth = 1;
			do {
				if(i >= lines.size())
					throw new FileParseException("Run away if declaration", line);
				instr = lines.get(i).splitOnSpaces(-1)[1];
				switch(instr) {
				case "%if":
					ifDepth++;
					break;
				case "%endif":
					ifDepth--;
					break;
				}
				lines.remove(i);
			} while(ifDepth != 0);
		}
	}
	
	private static void processOrgDirective(InstructionLine line) throws FileParseException {
		String[] sections = line.splitOnSpaces(3);
		
		if(sections.length != 3)
			throw new FileParseException("Illegal org directive format", line);
		
		try {
			startingAddress = Integer.parseInt(ExpressionHandler.evaluate(ExpressionHandler.expand(sections[2], line), line));
			currentAddress = 0;
		}
		catch (Exception e) {
			throw new FileParseException("Illegal org value \"" + sections[2] + "\"", line);
		}
	}
	
	private static ArrayList<InstructionLine> processIncludeDirective(InstructionLine line) throws FileParseException {
		String[] sections = line.splitOnSpaces(3);
		
		if(sections.length != 3)
			throw new FileParseException("Illegal include directive format", line);
		
		if(StringHelper.isCompleteString(sections[2]))
			throw new FileParseException("File path could not be parsed", line);
		
		FileWrap file = new FileWrap(StringEscapeUtils.escapeJava(sections[2].substring(1, sections[2].length() - 1)));
		
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
		String[] sections = line.splitOnSpaces(4);
		
		if(sections.length < 3)
			throw new FileParseException("Illegal define directive format", line);
		
		String expansion = (sections.length == 4) ? sections[3] : "";
		
		Macro macro = new Macro(sections[2], expansion, line);
		context.current.put(macro.getKey(), macro);
	}
	
	// Returns index of %endmacro
	private static void processMacroDirective(List<InstructionLine> lines, int index) throws FileParseException {
		String[] sections = lines.get(index).splitOnSpaces(4);
		
		if(sections.length != 4)
			throw new FileParseException("Illegal macro directive format", lines.get(index));
		
		int numParameters;
		try {
			numParameters = Integer.parseInt(ExpressionHandler.evaluate(ExpressionHandler.expand(sections[3], lines.get(index)), lines.get(index)));
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
			
			sec = lines.get(i).splitOnSpaces(-1);
			
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
			String label = lines.get(i).getLabel();
			
			if(!label.isEmpty())
				expansion = label + ": " + expansion;
				
			lines.remove(i);
		}
		
		expansion = expansion.substring(0, expansion.length() - 1);	// Remove last new line
		Macro macro = new MultiLineMacro(sections[2], numParameters, expansion, lines.get(index));
		lines.remove(index);
		context.current.put(macro.getKey(), macro);
	}
}
