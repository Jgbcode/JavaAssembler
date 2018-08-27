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
		
		FileWrap codeFile = new FileWrap(filepathCode);
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
		
		FileWrap isFile = new FileWrap(filepathIS + ".isa");
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
		
		//start assembling
		List<String> binaryLines = assemble(linesCode, is);
		
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

	public static List<String> assemble(List<String> lines, IS is)
	{
		List<InstructionLine> iLines = new ArrayList<>();
		//Add metadata to each line. (Linenumber)
		for(int i = 0; i < lines.size(); i++)
		{
			iLines.add(new InstructionLine(lines.get(i), i+1));
		}
		
		//Abort if line starts with space instead of tabs
		for(InstructionLine line : iLines)
		{
			if(line.getRawContent().startsWith(" "))
			{
				die("Lines must not start with space. " + line);
			}
		}

		//Filter lines without relevant data. (Only comments, or empty lines)
		iLines = iLines.stream().filter(iLine -> iLine.hasContent()).collect(Collectors.toList());
		
		//Seperate labels from instructions. Also attatch final addresses.
		//TODO: More aliases friendly. Difficult to add them now.
		List<InstructionLine> labelLines = new ArrayList<>();
		List<InstructionLine> instructions = new ArrayList<>();
		
		int i = 0;
		for(InstructionLine line : iLines)
		{
			line.setAddress(i);
			if(line.isLabel())
			{
				labelLines.add(line);
				//Check if label contains space or tab
				if(line.getContent().matches(".*[\t ].*"))
				{
					die("Label contains spaces or tabs. " + line);
				}
			}
			else
			{
				instructions.add(line);
				i += 1;
			}
		}
		
		Map<String, Integer> labels = new HashMap<>();
		for(InstructionLine label : labelLines)
		{
			labels.put(label.getContent(), label.getAddress());
		}
		
		//Start parsing instructions.
		List<String> binaryLines = new ArrayList<>();
		
		for(InstructionLine line : instructions)
		{
			String parts[] = line.getContent().split(" ");
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
