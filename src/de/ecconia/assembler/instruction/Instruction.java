package de.ecconia.assembler.instruction;

import java.util.ArrayList;
import java.util.List;

import de.ecconia.assembler.is.InstructionFormat;
import de.ecconia.assembler.is.Section;

public class Instruction
{
	private final List<Segment> parts;
	
	private Instruction(List<Segment> parts)
	{
		this.parts = parts;
	}

	//Get labels, associate them, and return them back to the instruction.
	//Generic for all instructions, no matter if they have labels.

	//Returns the binary String
	public String toString()
	{
		String binary = "";
		for(Segment seg : parts)
		{
			binary += " " + seg.toString();
		}
		return binary;
	}

	//TODO leftover args
	public static Instruction generate(String[] parts, InstructionFormat instructionFormat, String opcode)
	{
		int parameter = 1;
		List<Segment> segements = new ArrayList<>();

		try
		{
			for (Section sec : instructionFormat.getElements())
			{
				Segment current = sec.getSegment();
				if (current.hasParsed())
				{
					//Then all good
				}
				else if (current instanceof OpcodeSeg)
				{
					current.parse(opcode);
				}
				else
				{
					current.parse(parts[parameter++]);
				}
				
				segements.add(current);
			}
		}
		catch (IndexOutOfBoundsException e)
		{
			throw new InstructionParseException("The command needs more parameter then given.");
		}

		return new Instruction(segements);
	}
}
