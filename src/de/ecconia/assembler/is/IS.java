package de.ecconia.assembler.is;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.ecconia.assembler.InstructionLine;
import de.ecconia.assembler.io.FileParseException;
import de.ecconia.assembler.io.WeirdFormat;

public class IS
{
	public static IS getISFromLines(List<String> lines) throws FileParseException
	{
		int bitWidth = 0;
		Map<String, InstructionFormat> format = new HashMap<>();
		Map<String, InstructionAlias> instructions = new HashMap<>();
		
		//Load isa file
		WeirdFormat file = WeirdFormat.parse(lines);
		
//		isFile.printStructure();
//		System.out.println("------------------------");
//		file.printTree();
		
		//Validate/Parse isa file
		checkVersion(file);
		
		//Check all entity group counts
		checkGroupLevel(file);
		
		//check keynames of all keys
		checkKeyNames(file);
		
		
		//Get Information:
		List<WeirdFormat> kids;
		
		//Bitwidth
		kids = file.getGroup(0);
		for(WeirdFormat wf : kids)
		{
			if("IS-Width".equals(wf.getKey()))
			{
				try
				{
					bitWidth = Integer.parseInt(wf.getValue());
				}
				catch (NumberFormatException e)
				{
					throw new FileParseException("Bit width could not be parsed. Value: " + wf.getValue());
				}
				break;
			}
		}
		
		//Formats
		kids = file.getGroup(1);
		for(WeirdFormat instructionFormat : kids)
		{
			String formatName = instructionFormat.getKey();
			if(format.containsKey(formatName))
			{
				throw new FileParseException("Instruction Format defined multiple times. Format: " + formatName);
			}
			format.put(formatName, InstructionFormat.fromWeirdFormat(instructionFormat, bitWidth));
		}
		
		//Instructions
		kids = file.getGroup(2);
		for(WeirdFormat instrctionFormatGroup : kids)
		{
			String formatName = instrctionFormatGroup.getKey();
			if(!format.containsKey(formatName))
			{
				throw new FileParseException("Format not defined. Format: " + formatName);
			}
			for(WeirdFormat instructionType : instrctionFormatGroup.getGroup(0))
			{
				String opcode = instructionType.getKey();
				String value = instructionType.getValue();
				if(value.isEmpty())
				{
					throw new FileParseException("Empty instruction name. Format+Opcode: " + formatName + " " + opcode);
				}
				
				String aliases[] = value.split(" ");
				for(String alias : aliases)
				{
					alias = alias.toLowerCase();
					if(instructions.containsKey(alias))
					{
						throw new FileParseException("Instruction name already defined. Format+Opcode: " + formatName + " " + alias);
					}
					
					instructions.put(alias, new InstructionAlias(opcode, alias, format.get(formatName)));
				}
			}
		}
		
		return new IS(instructions);
	}
	
	public static void checkVersion(WeirdFormat file) throws FileParseException
	{
		if(!file.hasGroups())
		{
			throw new FileParseException("The isa file is corrupted it has no entries.");
		}
		
		List<WeirdFormat> l = file.getGroup(0);
		for(WeirdFormat w : l)
		{
			if("Version".equals(w.getKey()))
			{
				if("1".equals(w.getValue()))
				{
					return;
				}
				throw new FileParseException("Expected isa version 1, found version: " + w.getValue());
			}
		}
		
		throw new FileParseException("No version key found in the first group in the isa file.");
	}
	
	public static void checkGroupLevel(WeirdFormat file) throws FileParseException
	{
		if(file.groupAmount() != 3)
		{
			throw new FileParseException("The isa file should have 3 main groups. It has: " + file.groupAmount());
		}
		
		List<WeirdFormat> list;

		list = file.getGroup(0);
		for(WeirdFormat f : list)
		{
			if(f.groupAmount() != 0)
			{
				throw new FileParseException("The entries in the first group should not have any sub-group. Key: " + f.getKey());
			}
		}
		
		list = file.getGroup(1);
		for(WeirdFormat f : list)
		{
			if(f.groupAmount() < 1 || f.groupAmount() > 2)
			{
				throw new FileParseException("The entries in the second group should have 1 or 2 sub-groups. Key: " + f.getKey());
			}
			
			for(WeirdFormat w : f.getGroup(0))
			{
				if(w.groupAmount() != 0)
				{
					throw new FileParseException("No third level depth allowed. Key: " + f.getKey() + " > " + w.getKey());
				}
			}
			
			if(f.groupAmount() == 2)
			{
				for(WeirdFormat w : f.getGroup(1))
				{
					if(w.groupAmount() != 0)
					{
						throw new FileParseException("No third level depth allowed. Key: " + f.getKey() + " > " + w.getKey());
					}
				}
			}
		}
		
		list = file.getGroup(2);
		for(WeirdFormat f : list)
		{
			if(f.groupAmount() != 1)
			{
				throw new FileParseException("The entries in the third group should have 1 sub-group. Key: " + f.getKey());
			}
			
			for(WeirdFormat w : f.getGroup(0))
			{
				if(w.groupAmount() != 0)
				{
					throw new FileParseException("No third level depth allowed. Key: " + f.getKey() + " > " + w.getKey());
				}
			}
		}
	}

	public static void checkKeyNames(WeirdFormat file) throws FileParseException
	{
		List<WeirdFormat> kids;
		
		kids = file.getGroup(0);
		test1:{
			for(WeirdFormat wf : kids)
			{
				if("IS-Width".equals(wf.getKey()))
				{
					break test1;
				}
			}
			throw new FileParseException("No IS-Width defined.");
		}
		
		kids = file.getGroup(1);
		for(WeirdFormat instructionFormatGroup : kids)
		{
			List<WeirdFormat> formatGroups = instructionFormatGroup.getGroup(0);
			for(WeirdFormat entry : formatGroups)
			{
				if(!entry.getKey().matches("[1-9][0-9]*"))
				{
					throw new FileParseException("Keys in this group have to be numeric. Key: " + entry.getKey());
				}
			}
			
			//TODO: Check assemblerFormats?
		}
		
		kids = file.getGroup(2);
		for(WeirdFormat formatInstructions : kids)
		{
			List<WeirdFormat> instructions = formatInstructions.getGroup(0);
			for(WeirdFormat instruction : instructions)
			{
				if(!instruction.getKey().matches("[01]+"))
				{
					throw new FileParseException("Keys in this group can only match regex [01]+ since it will be inserted into ROM (output). Key: " + instruction.getKey());
				}
			}
		}
	}
	
	public static List<InstructionLine> getDefaultInclude(List<String> lines, String file) {
		int start = -1;
		
		for(int i = 0; i < lines.size(); i++) {
			if(lines.get(i).startsWith("%include")) {
				lines.remove(i);
				start = i;
				break;
			}
		}
		
		if(start == -1)
			return new ArrayList<InstructionLine>();
		
		ArrayList<InstructionLine> ret = new ArrayList<InstructionLine>();
		for(int i = start; i < lines.size();) {
			ret.add(new InstructionLine(lines.get(i), i, file));
			lines.remove(i);
		}
		
		return ret;
	}
	
	private final Map<String, InstructionAlias> instructions;
	
	public IS(Map<String, InstructionAlias> instructions)
	{
		this.instructions = instructions;
	}
	
	public boolean isInstruction(String operation)
	{
		return instructions.containsKey(operation);
	}
	
	public String getOpcode(String operation)
	{
		return instructions.get(operation).getOpcode();
	}
	
	public InstructionFormat getFormat(String operation)
	{
		return instructions.get(operation).getFormat();
	}
}
