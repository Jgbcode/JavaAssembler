package de.ecconia.assembler.instruction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.ecconia.assembler.is.InstructionFormat;
import de.ecconia.assembler.is.Section;

public class Instruction
{
	private final List<LabelSeg> labels;
	private final List<Segment> parts;
	
	private Instruction(List<LabelSeg> labels, List<Segment> parts)
	{
		this.labels = labels;
		this.parts = parts;
	}

	//Get labels, associate them, and return them back to the instruction.
	//Generic for all instructions, no matter if they have labels.
	public Set<String> getLables()
	{
		return labels.stream().map(l -> l.getLabel()).collect(Collectors.toSet());
	}

	public void setLabels(Map<String, Integer> labels, int pc)
	{
		for(LabelSeg label : this.labels)
		{
			if(!labels.containsKey(label.getLabel()))
			{
				throw new InstructionParseException("Label " + label.getLabel() + " could not be resolved.");
			}
			
			label.parse(labels.get(label.getLabel()), pc);
		}
	}

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
		List<LabelSeg> labels = new ArrayList<>();
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
				else if (current instanceof LabelSeg)
				{
					LabelSeg lSeg = (LabelSeg) current;
					labels.add(lSeg);
					lSeg.setLabel(parts[parameter++]);
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

		return new Instruction(labels, segements);
	}
}
