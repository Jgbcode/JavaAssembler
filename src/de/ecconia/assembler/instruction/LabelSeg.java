package de.ecconia.assembler.instruction;

public class LabelSeg extends Segment
{
	private JumpType type;
	private String label;
	
	public LabelSeg(int bitWidth)
	{
		super(bitWidth);
	}
	
	public void parse(int address, int pc)
	{
		String jumpValue = "";
		if(type == JumpType.ABSOLUTE)
		{
			jumpValue = integerToUnsigned(address);
		}
		else
		{
			//PC + Value = Address
			//Value = Address - PC
			// 10 + (5) = 15
			// 10 + (-5) = 5
			jumpValue = integerToSigned(address - pc);
		}
		
		super.parse(jumpValue);
	}
	
	public void setLabel(String label)
	{
		this.label = label;
	}
	
	public String getLabel()
	{
		return label;
	}
	
	public void setJumpType(JumpType type)
	{
		this.type = type;
	}
	
	public enum JumpType
	{
		ABSOLUTE,
		RELATIVE
	}
}
