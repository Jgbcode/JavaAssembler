package de.ecconia.assembler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.ecconia.assembler.instruction.Instruction;
import de.ecconia.assembler.instruction.InstructionParseException;
import de.ecconia.assembler.io.FileParseException;
import de.ecconia.assembler.io.FileWrap;
import de.ecconia.assembler.is.IS;
import de.ecconia.assembler.preprocessor.Preprocessor;

public class Assembler
{
	//TODO use proper error codes and not all good 0
	public static void main(String[] args)
	{
		if(args.length < 1)
		{
			System.out.println("Provide the filepath as argument.");
			System.exit(0);
		}
		
		//Get the lines of the Codefile
		String filepathCode = args[0];
		
		FileWrap codeFile = new FileWrap(filepathCode.trim());
		if(!codeFile.exists())
		{
			die("Codefile does not exist. File: " + filepathCode);
		}
		
		List<String> linesCode = codeFile.lines();
		
		//Get the lines of the ISFile
		String filepathIS = getISType(linesCode);
		if(filepathIS == null)
		{
			System.out.println("Assemblercode does not specify instruction set.");
			die("Add a line before the code: \";IS<isname>\" where <isname> is the name of the IS file without the ending \".isa\".");
		}
		
		FileWrap isFile = new FileWrap(filepathIS.trim() + ".isa");
		if(!isFile.exists())
		{
			die("IS does not exist. File: " + filepathIS + ".isa");
		}
		
		//Create IS
		IS is = null;
		try
		{
			is = IS.getISFromLines(isFile.lines());
		}
		catch (FileParseException e)
		{
			System.out.println("Error parsing ISA file format:");
			die(e.getMessage());
		}
		System.out.println("ISA: " + filepathIS);
		
		List<InstructionLine> linesPreprocess = null;
		try
		{
			linesPreprocess = Preprocessor.runPreprocessor(linesCode, filepathCode);
		}
		catch (FileParseException e)
		{
			//Debug
			e.printStackTrace();
			
			System.out.println("Error during preprocessor execution:");
			die(e.getMessage());
		}
		
		// Preprocessor debugging
		System.out.println("========== BEGIN PREPROCESSOR OUTPUT ==========");
		for(InstructionLine line : linesPreprocess) {
			String label = line.getLabel();
			if(!label.isEmpty())
				System.out.print(label + ": ");
			System.out.println(line.getContent());
		}
		System.out.println("========== END PREPROCESSOR OUTPUT ==========");
		
		//start assembling
		List<String> binaryLines = assemble(linesPreprocess, is);
		
		System.out.println("Binary code:");
		for(String bin : binaryLines)
		{
			System.out.println(bin);
		}
		System.out.println();
		
		System.out.println("Printing in file.");
		String filename;
		if(filepathCode.indexOf('.') != -1)
		{
			filename = filepathCode.substring(0, filepathCode.indexOf('.'));
		}
		else
		{
			filename = filepathCode;
		}
		filename += ".dat";
		
		File outputFile = new File(filename);
		try
		{
			outputFile.createNewFile();
			
			FileWriter fw = new FileWriter(outputFile);
			for(String bin : binaryLines)
			{
				fw.write(bin);
				fw.write("\n");
			}
			fw.close();
			System.out.println("done.");
		}
		catch (IOException e)
		{
			System.out.println("Coulnd't write in file: " + filename);
		}
	}
	
	public static String getISType(List<String> lines)
	{
		for(String line : lines)
		{
			String trimmed = line.trim();
			//Dump lines with no content
			if(trimmed.isEmpty())
			{
				continue;
			}
			
			//Abort when a line is not a comment
			if(trimmed.charAt(0) != ';')
			{
				return null;
			}
			
			//Check if the line is the instruction type
			if(line.startsWith(";IS:"))
			{
				//Format: ";IS:<ISName>"
				return line.substring(4);
			}
		}
		//File empty (no content) or only comments in there
		return null;
	}

	public static List<String> assemble(List<InstructionLine> iLines, IS is)
	{
		// Extract labels
		Map<String, Integer> labels = new HashMap<>();
		for(InstructionLine label : iLines)
		{
			String l = label.getLabel();
			if(!l.isEmpty()) {
				Integer oldAddr = labels.put(l, label.getAddress());
				
				if(oldAddr != null)
					die("Duplicated label - " + label);
			}
		}
		
		//Filter lines without relevant data. (Only comments, or empty lines)
		iLines = iLines.stream().filter(iLine -> iLine.hasContent()).collect(Collectors.toList());
		
		//Seperate labels from instructions. Also attatch final addresses.
		//TODO: More aliases friendly. Difficult to add them now.
		
		//Start parsing instructions.
		List<String> binaryLines = new ArrayList<>();
		
		for(InstructionLine line : iLines)
		{
			String parts[] = line.getContent().split("\\s*(,|\\s)\\s*");
			String operation = parts[0];

			if(!is.isInstruction(operation))
			{
				die("Instruction in line " + line.getLineNumber() + " is not defined: " + operation);
			}
			
			Instruction instruction = null;
			try
			{
				instruction = is.getFormat(operation).parseInstruction(parts, is.getOpcode(operation));
				
				Set<String> labelsToFind = instruction.getLables();
				Map<String, Integer> foundLabels = new HashMap<>();
				for(String labelToFind : labelsToFind)
				{
					Integer label = labels.get(labelToFind);
					if(label == null)
					{
						die("Couldn't find label: " + labelToFind);
					}
					foundLabels.put(labelToFind, label);
				}
				
				instruction.setLabels(foundLabels, line.getAddress());
			}
			catch (InstructionParseException e)
			{
				System.out.println("Could not parse instruction \"" + line.getContent() + "\" at line " + line.getLineNumber());
				die(e.getMessage());
			}
			
			System.out.println(line.getAddress() + ": " + line.getContent() + " ->");
			System.out.println("    " + instruction);
			
			binaryLines.add(instruction.toString().replace(" ", ""));
		}
		
		return binaryLines;
	}
	
	public static void print(List<InstructionLine> iLines)
	{
		for(InstructionLine s : iLines)
		{
			System.out.println(s.getAddress() + ": " + s);
		}
	}

	public static void die(String message)
	{
		System.out.println(message);
		//Fuuu lets just use 1, yes there was an error
		System.exit(1);
	}
}
