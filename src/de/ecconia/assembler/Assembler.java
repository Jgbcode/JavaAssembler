package de.ecconia.assembler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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
			System.err.println("Provide the filepath as argument.");
			System.exit(0);
		}
		
		//Get the lines of the Codefile
		String filepathCode = args[0];
		
		FileWrap codeFile = new FileWrap(filepathCode.trim());
		if(!codeFile.exists())
		{
			die("Codefile does not exist. File: " + filepathCode);
		}
		
		System.out.println("Reading code file: " + filepathCode);
		
		List<String> linesCode = codeFile.lines();
		
		//Get the lines of the ISFile
		String filepathIS = getISType(linesCode);
		if(filepathIS == null)
		{
			System.err.println("Assemblercode does not specify instruction set.");
			die("Add a line before the code: \";IS<isname>\" where <isname> is the name of the IS file without the ending \".isa\".");
		}
		
		FileWrap isFile = new FileWrap(codeFile.getFile().getParent() + File.separator + filepathIS.trim() + ".isa");
		if(!isFile.exists())
		{
			die("IS does not exist. File: " + filepathIS + ".isa");
		}
		
		//Create IS
		IS is = null;
		List<InstructionLine> isIncludes = null;
		try
		{
			List<String> temp = isFile.lines();
			isIncludes = IS.getDefaultInclude(temp, filepathIS);
			is = IS.getISFromLines(temp);
		}
		catch (FileParseException e)
		{
			System.err.println("Error parsing ISA file format:");
			die(e.getMessage());
		}
		System.out.println("ISA: " + filepathIS);
		
		List<InstructionLine> linesPreprocess = null;
		try
		{
			linesPreprocess = Preprocessor.runPreprocessor(linesCode, filepathCode, isIncludes);
		}
		catch (FileParseException e)
		{
			//Debug
			e.printStackTrace();
			
			System.err.println("Error during preprocessor execution:");
			die(e.getMessage());
		}
		
		// Preprocessor info
		System.out.println("========== BEGIN PREPROCESSOR OUTPUT ==========");
		for(InstructionLine line : linesPreprocess) {
			String label = line.getLabel();
			if(!label.isEmpty())
				System.out.println(label + ": ");
			if(line.hasContent())
				System.out.println("\t" + line.getContent());
		}
		System.out.println("=========== END PREPROCESSOR OUTPUT ===========\n\n");
		
		//start assembling
		List<String> binaryLines = null;
		
		System.out.println("========== BEGIN ASSEMBLER OUTPUT ==========");
		try {
			binaryLines = assemble(linesPreprocess, is);
		}
		catch(FileParseException e) {
			System.err.println("Error during assembler execution:");
			die(e.getMessage());
		}
		System.out.println("=========== END ASSEMBLER OUTPUT ===========\n\n");
		
		System.out.println("========== BEGIN BINARY OUTPUT ==========");
		for(String bin : binaryLines)
		{
			System.out.println(bin);
		}
		System.out.println("=========== END BINARY OUTPUT ===========\n\n");
		
		String filename;
		String torcher;
		if(filepathCode.lastIndexOf('.') != -1)
		{
			filename = filepathCode.substring(0, filepathCode.lastIndexOf('.'));
		}
		else
		{
			filename = filepathCode;
		}
		torcher = filename + ".torch";
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
		}
		catch (IOException e)
		{
			die("Couldn't write in file: " + filename);
		}
		
		String chars = convertToTorcher(binaryLines);
		File torcherFile = new File(torcher);
		System.out.println("========== BEGIN TORCHER COMPRESSED OUTPUT ==========");
		System.out.println(chars);
		System.out.println("=========== END TORCHER COMPRESSED OUTPUT ===========\n\n");
		try
		{
			torcherFile.createNewFile();
			
			FileWriter fw = new FileWriter(torcherFile);
			fw.write(chars);
			fw.close();
			System.out.println("done.");
		}
		catch (IOException e)
		{
			die("Couldn't write in file: " + torcher);
		}
	}
	
	public static String convertToTorcher(List<String> lines) {
		LinkedList<Boolean> bits = new LinkedList<Boolean>();
		for(String s : lines) {
			for(char c : s.toCharArray()) {
				if(c == '1')
					bits.add(true);
				else if(c == '0')
					bits.add(false);
			}
		}
		
		String result = "";
		while(!bits.isEmpty()) {
			// 200 characters per line
			for(int i = 0; i < 200 && !bits.isEmpty(); i++) {
				int num = 0;
				for(int j = 0; j < 15 && !bits.isEmpty(); j++) {
					if(bits.removeFirst())
						num += 1 << j;
				}
				
				result += (char) (num + 256);
			}
			result += '\n';
		}
		
		return result;
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

	public static List<String> assemble(List<InstructionLine> iLines, IS is) throws FileParseException
	{
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
				throw new FileParseException("Instruction \"" + operation + "\" not defined", line);
			}
			
			Instruction instruction = null;
			try
			{
				instruction = is.getFormat(operation).parseInstruction(parts, is.getOpcode(operation));
			}
			catch (InstructionParseException e)
			{
				throw new FileParseException("Failed to parse instruction: " + e.getMessage(), line);
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
		System.err.println(message);
		//Fuuu lets just use 1, yes there was an error
		System.exit(1);
	}
}
