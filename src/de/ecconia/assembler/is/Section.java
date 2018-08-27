package de.ecconia.assembler.is;

import de.ecconia.assembler.instruction.LabelSeg;
import de.ecconia.assembler.instruction.NumberSeg;
import de.ecconia.assembler.instruction.OpcodeSeg;
import de.ecconia.assembler.instruction.RegSeg;
import de.ecconia.assembler.instruction.Segment;
import de.ecconia.assembler.instruction.LabelSeg.JumpType;

public class Section
{
	private final int bits;
	private final String type;
	
	public Section(int bits, String type)
	{
		this.bits = bits;
		this.type = type;
	}
	
	public int getBits()
	{
		return bits;
	}
	
	public String getType()
	{
		return type;
	}
	
	//TODO: move to Constructor + validation + cloneable
	public Segment getSegment()
	{
		String parts[] = type.split(" ");
		String type = parts[0];
		
		Segment seg = null;
		
		switch(type.charAt(0))
		{
		case 'o':
			seg = new OpcodeSeg(bits);
			break;
		case 'l':
			seg = new LabelSeg(bits);
			((LabelSeg) seg).setJumpType(type.charAt(1) == 'a' ? JumpType.ABSOLUTE : JumpType.RELATIVE);
			break;
		case 'n':
			seg = new NumberSeg(bits);
			break;
		case 'a':
		case 'b':
		case 'c':
		case 'r':
			seg = new RegSeg(bits);
			break;
		case '0':
			seg = new Segment(bits);
			String constant = "";
			for(int i = 0; i < bits; i++)
			{
				constant += '0';
			}
			seg.parse(constant);
		default:
			if(type.matches("[0-1]{" + bits + "}"))
			{
				seg = new Segment(bits);
				seg.parse(type);
			}
		}
		
		if(parts.length > 1)
		{
			if(parts[parts.length-1].equals("<"))
			{
				seg.setReversed();
			}
		}
		return seg;
	}
}
