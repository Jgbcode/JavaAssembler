package de.ecconia.assembler.is;

import java.util.ArrayList;
import java.util.List;

import de.ecconia.assembler.instruction.Instruction;
import de.ecconia.assembler.io.FileParseException;
import de.ecconia.assembler.io.WeirdFormat;

public class InstructionFormat
{
	private final String name;
	private final List<Section> elements;
	
	public static InstructionFormat fromWeirdFormat(WeirdFormat wf, int bitwidth) throws FileParseException
	{
		List<Section> elements = new ArrayList<>();
		
		String name = wf.getKey();
		
		int totalBits = 0;
		
		for(WeirdFormat entry : wf.getGroup(0))
		{
			int bits = Integer.parseInt(entry.getKey());
			totalBits += bits;
			String type = entry.getValue();
			
			elements.add(new Section(bits, type));
		}
		
		if(totalBits != bitwidth)
		{
			throw new FileParseException("Format \"" + name + "\"'s total bits do not match the IS bitwidth. Counted bits: " + totalBits  + " Should be " + bitwidth + " bits.");
		}
		
//		if(wf.groupAmount() == 2)
//		{
//			for(WeirdFormat entry : wf.getGroup(1))
//			{
//				String format = entry.getKey();
//				
//				FormatRegex.parseFormatRegex(format);
//			}
//		}
		
		return new InstructionFormat(name, elements);
	}
	
	public InstructionFormat(String name, List<Section> elements)
	{
		this.name = name;
		this.elements = elements;
	}
	
	public String getName()
	{
		return name;
	}
	
	public List<Section> getElements()
	{
		return elements;
	}

	public Instruction parseInstruction(String[] parts, String opcode)
	{
		return Instruction.generate(parts, this, opcode);
	}
}